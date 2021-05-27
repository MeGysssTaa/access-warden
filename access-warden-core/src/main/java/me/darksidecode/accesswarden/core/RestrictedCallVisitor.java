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

import me.darksidecode.accesswarden.api.RestrictedCall;
import me.darksidecode.accesswarden.api.UnexpectedSetupException;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

final class RestrictedCallVisitor implements TransformingVisitor {

    private final Set<String> takenCheckerMethodNames = new HashSet<>();

    private final String annoDesc;

    private final ClassWriter cw;

    private boolean anythingModified;

    RestrictedCallVisitor(ClassWriter checkerClassWriter) {
        annoDesc = BytecodeUtils.describeAnnotation(RestrictedCall.class);
        cw = checkerClassWriter;
    }

    @Override
    public void visitClass(ClassNode cls) throws Exception {
        anythingModified = false;
    }

    @Override
    public void visitMethod(MethodNode mtd) throws Exception {
        if (mtd.visibleAnnotations != null)
            mtd.visibleAnnotations
                    .stream()
                    .filter(anno -> anno.desc.equals(annoDesc))
                    .findAny()
                    .ifPresent(anno -> transformMethod(mtd, anno, new AnnoConfig(anno)));
    }

    @Override
    public boolean anythingModified() {
        return anythingModified;
    }

    private void transformMethod(MethodNode mtd, AnnotationNode anno, AnnoConfig annoCfg) {
        try {
            mtd.instructions.insert(new LabelNode());
            String checkerMethodName = generateCheckerMethod(annoCfg);
            MethodInsnNode checkerMethodNode = new MethodInsnNode(INVOKESTATIC,
                    BytecodeUtils.CHECKER_CLASS_NAME, checkerMethodName, "()V");
            mtd.instructions.insert(checkerMethodNode);

            anythingModified = true;

            if (!annoCfg.getBoolean(RestrictedCall.k_preserveThisAnnotation))
                mtd.visibleAnnotations.remove(anno);
        } catch (UnexpectedSetupException ex) {
            throw new RuntimeException("unexpected setup: " + ex.getMessage());
        }
    }

    public String nextCheckerMethodName() {
        String name;

        do {
            long id = ThreadLocalRandom.current().nextLong(Integer.MAX_VALUE, Long.MAX_VALUE);
            name = "__check__" + Long.toString(id, 16) + "__";
        } while (!takenCheckerMethodNames.add(name)); // just in case

        return name;
    }

    private String generateCheckerMethod(AnnoConfig annoCfg) throws UnexpectedSetupException {
        RestrictedCall.Configuration conf = RestrictedCall.Configuration
                .newBuilder()
                    .exactExpectedCallStack     (annoCfg.getStringList(RestrictedCall.k_exactExpectedCallStack     ))
                    .prohibitReflectionTraces   (annoCfg.getBoolean   (RestrictedCall.k_prohibitReflectionTraces   ))
                    .prohibitNativeTraces       (annoCfg.getBoolean   (RestrictedCall.k_prohibitNativeTraces       ))
                    .prohibitArbitraryInvocation(annoCfg.getBoolean   (RestrictedCall.k_prohibitArbitraryInvocation))
                    .permittedSources           (annoCfg.getStringList(RestrictedCall.k_permittedSources           ))
                    .prohibitedSources          (annoCfg.getStringList(RestrictedCall.k_prohibitedSources          ))
                .build();

        String checkerMethodName = nextCheckerMethodName();

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC | ACC_SYNTHETIC, checkerMethodName, "()V", null, null);
        mv.visitCode();
        Label l0 = new Label();
        Label l1 = new Label();
        Label l2 = new Label();
        mv.visitTryCatchBlock(l0, l1, l2, "me/darksidecode/accesswarden/api/UnexpectedSetupException");
        mv.visitLabel(l0);
        mv.visitMethodInsn(INVOKESTATIC, "me/darksidecode/accesswarden/api/RestrictedCall$Configuration", "newBuilder", "()Lme/darksidecode/accesswarden/api/RestrictedCall$Configuration$Builder;", false);
        generateSetStringList(conf.exactExpectedCallStack(), mv, "exactExpectedCallStack");
        if (conf.prohibitReflectionTraces()) {
            mv.visitInsn(ICONST_1);
            Label l4 = new Label();
            mv.visitLabel(l4);
            mv.visitMethodInsn(INVOKEVIRTUAL, "me/darksidecode/accesswarden/api/RestrictedCall$Configuration$Builder", "prohibitReflectionTraces", "(Z)Lme/darksidecode/accesswarden/api/RestrictedCall$Configuration$Builder;", false);
        }
        if (conf.prohibitNativeTraces()) {
            mv.visitInsn(ICONST_1);
            Label l5 = new Label();
            mv.visitLabel(l5);
            mv.visitMethodInsn(INVOKEVIRTUAL, "me/darksidecode/accesswarden/api/RestrictedCall$Configuration$Builder", "prohibitNativeTraces", "(Z)Lme/darksidecode/accesswarden/api/RestrictedCall$Configuration$Builder;", false);
        }
        if (conf.prohibitArbitraryInvocation()) {
            mv.visitInsn(ICONST_1);
            Label l6 = new Label();
            mv.visitLabel(l6);
            mv.visitMethodInsn(INVOKEVIRTUAL, "me/darksidecode/accesswarden/api/RestrictedCall$Configuration$Builder", "prohibitArbitraryInvocation", "(Z)Lme/darksidecode/accesswarden/api/RestrictedCall$Configuration$Builder;", false);
        }
        Label l7 = new Label();
        mv.visitLabel(l7);
        generateSetStringList(conf.permittedSources(), mv, "permittedSources");
        Label l8 = new Label();
        mv.visitLabel(l8);
        generateSetStringList(conf.prohibitedSources(), mv, "prohibitedSources");
        Label l9 = new Label();
        mv.visitLabel(l9);
        mv.visitMethodInsn(INVOKEVIRTUAL, "me/darksidecode/accesswarden/api/RestrictedCall$Configuration$Builder", "build", "()Lme/darksidecode/accesswarden/api/RestrictedCall$Configuration;", false);
        mv.visitVarInsn(ASTORE, 0);
        Label l10 = new Label();
        mv.visitLabel(l10);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "me/darksidecode/accesswarden/api/RestrictedCall$Configuration", "contextResolutionOptions", "()I", false);
        mv.visitMethodInsn(INVOKESTATIC, "me/darksidecode/accesswarden/api/ContextResolution", "resolve", "(I)Lme/darksidecode/accesswarden/api/FilteredContext;", false);
        mv.visitVarInsn(ASTORE, 1);
        Label l11 = new Label();
        mv.visitLabel(l11);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "me/darksidecode/accesswarden/api/FilteredContext", "filteredCallStack", "()Ljava/util/List;", false);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "size", "()I", true);
        mv.visitInsn(ICONST_1);
        Label l12 = new Label();
        mv.visitJumpInsn(IF_ICMPLE, l12);
        Label l13 = new Label();
        mv.visitLabel(l13);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "me/darksidecode/accesswarden/api/FilteredContext", "filteredCallStack", "()Ljava/util/List;", false);
        mv.visitInsn(ICONST_0);
        mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "remove", "(I)Ljava/lang/Object;", true);
        mv.visitInsn(POP);
        Label l14 = new Label();
        mv.visitLabel(l14);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, "me/darksidecode/accesswarden/api/ContextResolution", "ensureCallPermitted", "(Lme/darksidecode/accesswarden/api/FilteredContext;Lme/darksidecode/accesswarden/api/RestrictedCall$Configuration;)V", false);
        mv.visitJumpInsn(GOTO, l1);
        mv.visitLabel(l12);
        mv.visitFrame(F_APPEND, 2, new Object[] { "me/darksidecode/accesswarden/api/RestrictedCall$Configuration", "me/darksidecode/accesswarden/api/FilteredContext" }, 0, null);
        mv.visitTypeInsn(NEW, "me/darksidecode/accesswarden/api/UnexpectedSetupException");
        mv.visitInsn(DUP);
        mv.visitLdcInsn("filtered call stack is unexpectedly small");
        mv.visitMethodInsn(INVOKESPECIAL, "me/darksidecode/accesswarden/api/UnexpectedSetupException", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(ATHROW);
        mv.visitLabel(l1);
        mv.visitFrame(F_CHOP, 2, null, 0, null);
        Label l15 = new Label();
        mv.visitJumpInsn(GOTO, l15);
        mv.visitLabel(l2);
        mv.visitFrame(F_SAME1, 0, null, 1, new Object[] { "me/darksidecode/accesswarden/api/UnexpectedSetupException" });
        mv.visitVarInsn(ASTORE, 0);
        Label l16 = new Label();
        mv.visitLabel(l16);
        mv.visitTypeInsn(NEW, "java/lang/SecurityException");
        mv.visitInsn(DUP);
        mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
        mv.visitLdcInsn("unexpected setup: ");
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "me/darksidecode/accesswarden/api/UnexpectedSetupException", "getMessage", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/SecurityException", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(ATHROW);
        mv.visitLabel(l15);
        mv.visitFrame(F_SAME, 0, null, 0, null);
        mv.visitInsn(RETURN);
        mv.visitLocalVariable("conf", "Lme/darksidecode/accesswarden/api/RestrictedCall$Configuration;", null, l10, l1, 0);
        mv.visitLocalVariable("ctx", "Lme/darksidecode/accesswarden/api/FilteredContext;", null, l11, l1, 1);
        mv.visitLocalVariable("ex", "Lme/darksidecode/accesswarden/api/UnexpectedSetupException;", null, l16, l15, 0);
        mv.visitMaxs(5, 2);
        mv.visitEnd();

        return checkerMethodName;
    }

    private static void generateSetStringList(List<String> src, MethodVisitor mv, String listName) {
        if (src.isEmpty())
            // Don't push anything and keep null - it will be treated exactly the same as
            // Collections.emptyList(). This way we reduce the amount of extra generated bytecode.
            return;

        if (src.size() == 1) {
            // Collections.singletonList("str")
            mv.visitLdcInsn(src.get(0));
            mv.visitMethodInsn(INVOKESTATIC, "java/util/Collections", "singletonList", "(Ljava/lang/Object;)Ljava/util/List;", false);
        } else {
            // Arrays.asList("str1", "str2", ..., "strN")
            BytecodeUtils.pushIntConstant(mv, src.size());
            mv.visitTypeInsn(ANEWARRAY, "java/lang/String");

            for (int i = 0; i < src.size(); i++) {
                mv.visitInsn(DUP);
                BytecodeUtils.pushIntConstant(mv, i);
                mv.visitLdcInsn(src.get(i));
                mv.visitInsn(AASTORE);
            }

            Label l3 = new Label();
            mv.visitLabel(l3);
            mv.visitMethodInsn(INVOKESTATIC, "java/util/Arrays", "asList", "([Ljava/lang/Object;)Ljava/util/List;", false);
        }

        mv.visitMethodInsn(INVOKEVIRTUAL, "me/darksidecode/accesswarden/api/RestrictedCall$Configuration$Builder", listName, "(Ljava/util/List;)Lme/darksidecode/accesswarden/api/RestrictedCall$Configuration$Builder;", false);
    }

}
