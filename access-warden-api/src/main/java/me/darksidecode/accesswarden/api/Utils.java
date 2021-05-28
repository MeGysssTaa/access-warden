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

package me.darksidecode.accesswarden.api;

final class Utils {

    private Utils() {}

    private static String globToRegex(String glob) {
        if (glob == null)
            throw new NullPointerException("glob cannot be null");

        if (glob.isEmpty())
            throw new IllegalArgumentException("glob cannot be empty");

        StringBuilder regex = new StringBuilder("^");
        char[] globChars = glob.toCharArray();

        for (char c : globChars) {
            switch (c) {
                case '*':
                    regex.append(".*");
                    break;

                case '?':
                    regex.append('.');
                    break;

                case '.':
                    regex.append("\\.");
                    break;

                case '\\':
                    regex.append("\\\\");
                    break;

                default:
                    regex.append(c);
                    break;
            }
        }

        return regex.append('$').toString();
    }

    static boolean callFrameMatches(StackTraceElement frame, String globFilter) {
        if (frame == null)
            throw new NullPointerException("frame cannot be null");

        String regexFilter = globToRegex(globFilter);
        String callStr = frame.getClassName() + "#" + frame.getMethodName();

        return callStr.matches(regexFilter);
    }

}
