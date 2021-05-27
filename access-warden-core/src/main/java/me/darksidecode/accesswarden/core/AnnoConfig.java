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

import org.objectweb.asm.tree.AnnotationNode;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class AnnoConfig {

    private final Map<String, Object> annoValues;

    AnnoConfig(AnnotationNode anno) {
        if (anno.values == null || anno.values.isEmpty() || anno.values.size() % 2 != 0)
            throw new IllegalArgumentException(
                    "invalid annotation of type " + anno.desc + " (is the bytecode correct?): " +
                            "bad values");

        annoValues = new HashMap<>();

        for (int i = 0; i < anno.values.size() - 1; i += 2) {
            Object vKey = anno.values.get(i);

            if (vKey instanceof String) {
                Object vVal = anno.values.get(i + 1);

                if (checkValueType(vVal))
                    annoValues.put((String) anno.values.get(i), anno.values.get(i + 1));
                else
                    throw new IllegalArgumentException(
                            "invalid annotation of type " + anno.desc + " (is the bytecode correct?): " +
                                    "unsupported value type at index " + i);
            } else
                throw new IllegalArgumentException(
                        "invalid annotation of type " + anno.desc + " (is the bytecode correct?): " +
                                "non-string key at index " + i);
        }
    }
    
    private static boolean checkValueType(Object val) {
        if (val instanceof List) {
            List<Object> asList = (List<Object>) val;
            return asList.isEmpty() || asList.stream().allMatch(AnnoConfig::checkValueType);
        } else
            return     val instanceof Byte  || val instanceof Boolean || val instanceof Character
                    || val instanceof Short || val instanceof Integer || val instanceof Long
                    || val instanceof Float || val instanceof Double  || val instanceof String
                    || (val instanceof String[] && ((String[]) val).length == 2);
    }

    String getString(String key) {
        Object val = annoValues.get(key);

        if (val == null)
            return "";
        else {
            try {
                return (String) val;
            } catch (ClassCastException ex) {
                throw new ClassCastException(
                        "annotation value with key '" + key + "' is not a String " +
                                "- it is " + val.getClass().getName());
            }
        }
    }

    List<String> getStringList(String key) {
        Object val = annoValues.get(key);

        if (val == null)
            return Collections.emptyList();
        else {
            try {
                return (List<String>) val;
            } catch (ClassCastException ex) {
                throw new ClassCastException(
                        "annotation value with key '" + key + "' is not a List<String> " +
                                "- it is " + val.getClass().getName());
            }
        }
    }

    Boolean getBoolean(String key) {
        Object val = annoValues.get(key);

        if (val == null)
            return false;
        else {
            try {
                return (Boolean) val;
            } catch (ClassCastException ex) {
                throw new ClassCastException(
                        "annotation value with key '" + key + "' is not a Boolean " +
                                "- it is " + val.getClass().getName());
            }
        }
    }

}
