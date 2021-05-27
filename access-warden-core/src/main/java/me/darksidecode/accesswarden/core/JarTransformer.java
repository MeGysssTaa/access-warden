/*
 * Copyright 2021 German Vekhorev (DarksideCode)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.darksidecode.accesswarden.core;

import org.apache.commons.io.IOUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

final class JarTransformer {

    private static final Logger log = LoggerFactory.getLogger(JarTransformer.class);

    private final File jarFile;

    private State currentState = State.CREATED;

    private JarFile inputJar;

    private File tempOutputJar;

    private final Collection<ClassNode> classes = new ArrayList<>();

    private final Map<String, ClassNode> modifiedClasses = new HashMap<>();

    private ClassWriter checkerClassWriter;

    JarTransformer(File jarFile) {
        this.jarFile = jarFile;
    }

    State state() {
        return currentState;
    }

    void read() {
        if (currentState != State.CREATED)
            throw new IllegalStateException("read() in illegal state: " + currentState);

        try {
            log.info("Reading input jar");

            inputJar = new JarFile(jarFile);
            loadClassesWithoutClosing(inputJar);
            tempOutputJar = new File(jarFile.getParentFile(),
                    jarFile.getName() + ".access-warden-temp.out");

            if (tempOutputJar.exists())
                if (!tempOutputJar.delete())
                    throw new IOException("failed to delete temporary output file");

            currentState = State.READ;
        } catch (Exception ex) {
            log.error("Unhandled exception in read()", ex);
            currentState = State.ERROR;
        }
    }

    void apply() {
        if (currentState != State.READ)
            throw new IllegalStateException("apply() in illegal state: " + currentState);

        checkerClassWriter = BytecodeUtils.beginCreateCheckerClass();

        Collection<TransformingVisitor> transformers = Arrays.asList(
                new RestrictedCallVisitor(checkerClassWriter)
        );

        log.info("Applying transformations to {} classes with {} visitors",
                classes.size(), transformers.size());

        for (ClassNode cls : classes) {
            for (TransformingVisitor transformer : transformers) {
                try {
                    transformer.visitClass(cls);
                } catch (Exception ex) {
                    log.warn("Error in visitClass({}) of transformer {}: {}",
                            cls.name, transformer.getClass().getName(), ex.toString());
                }

                for (FieldNode fld : cls.fields) {
                    try {
                        transformer.visitField(fld);
                    } catch (Exception ex) {
                        log.warn("Error in visitField({}#{}) of transformer {}: {}",
                                cls.name, fld.name, transformer.getClass().getName(), ex.toString());
                    }
                }

                for (MethodNode mtd : cls.methods) {
                    try {
                        transformer.visitMethod(mtd);
                    } catch (Exception ex) {
                        log.warn("Error in visitMethod({}#{}) of transformer {}: {}",
                                cls.name, mtd.name, transformer.getClass().getName(), ex.toString());
                    }
                }

                if (transformer.anythingModified()) {
                    log.info("Modified something in class {}", cls.name);
                    modifiedClasses.put(cls.name + ".class", cls);
                }
            }
        }

        checkerClassWriter.visitEnd();
        currentState = State.TRANSFORMED;
    }

    void save() {
        if (currentState != State.TRANSFORMED)
            throw new IllegalStateException("save() in illegal state: " + currentState);

        if (modifiedClasses.isEmpty()) {
            close();
            log.info("No classes modified");
            currentState = State.SAVED;

            return;
        }

        log.info("Saving changes");

        try (JarOutputStream outputJar = new JarOutputStream(new FileOutputStream(tempOutputJar))) {
            Enumeration<JarEntry> entries = inputJar.entries();

            // Copy everything from the input jar to the output jar and replace all modified classes.
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                JarEntry toSave = new JarEntry(name);

                // Jar entry begin.
                outputJar.putNextEntry(toSave);

                if (modifiedClasses.containsKey(name)) {
                    // Serialize the modified ClassNode and overwrite it.
                    ClassNode modifiedClass = modifiedClasses.get(name);
                    ClassWriter writer = new ClassWriter(0);

                    modifiedClass.accept(writer);
                    byte[] bytes = writer.toByteArray();
                    outputJar.write(bytes, 0, bytes.length);
                } else {
                    // Copy the entry as is.
                    byte[] buf = new byte[4096];
                    int len;

                    try (InputStream is = inputJar.getInputStream(entry)) {
                        while ((len = is.read(buf)) > 0) {
                            outputJar.write(buf, 0, len);
                        }
                    }
                }

                // Jar entry end.
                outputJar.closeEntry();
            }

            // Add the generated checker class in final jar.
            byte[] checkerClassBytes = checkerClassWriter.toByteArray();
            outputJar.putNextEntry(new JarEntry(BytecodeUtils.CHECKER_CLASS_NAME + ".class"));
            outputJar.write(checkerClassBytes);

            close();

            if (!jarFile.delete())
                throw new IOException("failed to delete the original input file");

            Files.move(tempOutputJar.toPath(), jarFile.toPath());
            currentState = State.SAVED;
        } catch (Exception ex) {
            log.error("Unhandled exception in save()", ex);
            currentState = State.ERROR;
        }
    }

    void close() {
        if (currentState == State.CREATED)
            throw new IllegalStateException("close() in illegal state: " + currentState);

        if (inputJar != null) {
            try {
                inputJar.close();
            } catch (Exception ignored) {}
        }
    }

    private void loadClassesWithoutClosing(JarFile inputJar) {
        inputJar.stream().forEach(entry -> readJarEntry(inputJar, entry, classes));
    }

    private void readJarEntry(JarFile jar, JarEntry entry, Collection<ClassNode> classes) {
        String name = entry.getName();

        try (InputStream is = jar.getInputStream(entry)) {
            if (name.endsWith(".class")) {
                byte[] bytes = IOUtils.toByteArray(is);
                String cafebabe = String.format(
                        "%02X%02X%02X%02X", bytes[0], bytes[1], bytes[2], bytes[3]);

                if (cafebabe.equalsIgnoreCase("cafebabe")) {
                    try {
                        ClassNode clsNode = readNode(bytes);

                        if (clsNode.name.equals("java/lang/Object") || clsNode.superName != null)
                            classes.add(clsNode);
                    } catch (Exception ex) {
                        throw new RuntimeException("failed to read a class", ex);
                    }
                }
            }
        } catch (Exception ex) {
            throw new RuntimeException("failed to read a jar entry", ex);
        }
    }

    private static ClassNode readNode(byte[] classBytes) {
        ClassReader reader = new ClassReader(classBytes);
        ClassNode clsNode = new ClassNode();

        try {
            reader.accept(clsNode, ClassReader.EXPAND_FRAMES);
        } catch (Exception ex) {
            try {
                reader.accept(clsNode, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
            } catch (Exception fatal) {
                throw new RuntimeException("failed to get node from bytes, previous error: " + ex, fatal);
            }
        }

        return clsNode;
    }

    enum State {
        CREATED,
        READ,
        TRANSFORMED,
        SAVED,
        ERROR
    }

}
