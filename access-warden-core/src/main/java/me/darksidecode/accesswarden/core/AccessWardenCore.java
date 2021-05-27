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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public final class AccessWardenCore {

    private AccessWardenCore() {}

    private static final Logger log = LoggerFactory.getLogger(AccessWardenCore.class);
    
    public static void main(String[] args) {
        log.info("=============================== access-warden-core ===============================");
        log.info("Configuring");

        if (args.length == 0) {
            helpAndQuit();
            return;
        }

        StringBuilder pathBuilder = new StringBuilder();

        for (int i = 0; i < args.length; i++) {
            if (i != 0) pathBuilder.append(' ');
            pathBuilder.append(args[i]);
        }

        File jarFile = new File(pathBuilder.toString());
        log.info("Input jar file: {}", jarFile.getAbsolutePath());

        if (jarFile.isFile()) {
            if (transform(jarFile))
                log.info("Success");
            else
                log.error("Something went wrong");
        } else
            log.error("The specified file either does not exist or is a directory");

        log.info("Bye");
    }
    
    private static void helpAndQuit() {
        log.error("Please specify path to the input jar file as a command argument.");
        System.exit(1);
    }

    public static boolean transform(File jarFile) {
        if (jarFile == null)
            throw new NullPointerException("jarFile cannot be null");

        if (!jarFile.isFile())
            throw new IllegalArgumentException("not a file");

        try {
            JarTransformer transformer = new JarTransformer(jarFile);
            checkedRun(transformer);
            log.info("Finished transformation in file '{}'", jarFile.getAbsolutePath());

            return true;
        } catch (Exception ex) {
            log.error("Failed to transform the specified jar file: {}", ex.getMessage());
            return false;
        }
    }

    private static void checkedRun(JarTransformer transformer) {
        checkedRead (transformer);
        checkedApply(transformer);
        checkedSave (transformer);

        transformer.close();
    }

    private static void checkedRead(JarTransformer transformer) {
        transformer.read();

        if (transformer.state() == JarTransformer.State.ERROR) {
            transformer.close();
            throw new RuntimeException("internal JarTransformer error: read()");
        }
    }

    private static void checkedApply(JarTransformer transformer) {
        transformer.apply();

        if (transformer.state() == JarTransformer.State.ERROR) {
            transformer.close();
            throw new RuntimeException("internal JarTransformer error: apply()");
        }
    }

    private static void checkedSave(JarTransformer transformer) {
        transformer.save();

        if (transformer.state() == JarTransformer.State.ERROR) {
            transformer.close();
            throw new RuntimeException("internal JarTransformer error: save()");
        }
    }

}
