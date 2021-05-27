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

package me.darksidecode.accesswarden.gradle;

import me.darksidecode.accesswarden.core.AccessWardenCore;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AccessWardenGradle implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        project.task("accessWardenGradle").doLast(task -> run(project));
    }

    private void run(Project project) {
        transformFirst(pickCandidateBuildJars(project));
    }

    private static List<File> pickCandidateBuildJars(Project project) {
        List<File> candidates = new ArrayList<>();
        File buildDir = project.getBuildDir();

        if (buildDir.isDirectory()) {
            File buildLibsDir = new File(buildDir, "libs");

            if (buildLibsDir.isDirectory()) {
                /* project-name-all.jar */
                File fatJarUnversioned = new File(buildLibsDir,
                        project.getName() + "-all.jar");

                if (fatJarUnversioned.isFile())
                    candidates.add(fatJarUnversioned);

                /* project-name-all-1.0.0-SNAPSHOT.jar */
                File fatJarVersioned = new File(buildLibsDir,
                        project.getName() + "-all-" + project.getVersion() + ".jar");

                if (fatJarVersioned.isFile())
                    candidates.add(fatJarVersioned);

                /* project-name.jar */
                File jarUnversioned = new File(buildLibsDir,
                        project.getName() + ".jar");

                if (jarUnversioned.isFile())
                    candidates.add(jarUnversioned);

                /* project-name-1.0.0-SNAPSHOT.jar */
                File jarVersioned = new File(buildLibsDir,
                        project.getName() + "-" + project.getVersion() + ".jar");

                if (jarVersioned.isFile())
                    candidates.add(jarVersioned);
            }
        }

        return candidates;
    }

    private static void transformFirst(List<File> candidates) {
        System.out.println("Access Warden: " + candidates.size() + " transform candidates:");
        candidates.forEach(candidate -> System.out.println("    - build/libs/" + candidate.getName()));

        for (File candidate : candidates) {
            try {
                if (AccessWardenCore.transform(candidate)) {
                    System.out.println("Access Warden: successfully transformed build/libs/" + candidate.getName());
                    return;
                } else
                    System.err.println("Access Warden: failed to transform candidate " +
                            "build/libs/" + candidate.getName() + " - will try another candidate (if any left)");
            } catch (Exception ex) {
                System.err.println("Access Warden: failed to transform candidate " +
                        "build/libs/" + candidate.getName() + " - will try another candidate (if any left)");
                ex.printStackTrace();
            }
        }
    }

}
