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

package me.darksidecode.accesswarden.demo;

import me.darksidecode.accesswarden.api.RestrictedCall;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressWarnings ("UseOfSystemOutOrSystemErr")
public class AccessWardenDemo {

    public static void main(String[] args) throws Exception {
        System.out.println(" \n * BEGIN DEMO * \n \n ");

        System.out.println("--- Trying normal call (should work) ---");
        first(15);

        System.out.println("--- Trying reflection call (should error) ---");

        try {
            Class<?> clazz = Class.forName("me.darksidecode.accesswarden.demo.AccessWardenDemo");
            Method method = clazz.getDeclaredMethod("first", int.class);
            method.invoke(null, 15);
        } catch (InvocationTargetException ex) {
            System.err.println(">>> As expected, reflection call failed");
            ex.printStackTrace();
        }

        System.out.println("--- Trying arbitrary call (should error) ---");

        try {
            second(15);
        } catch (SecurityException ex) {
            System.err.println(">>> As expected, arbitrary call failed");
            ex.printStackTrace();
        }

        System.out.println("--- Trying non-prohibited call (should work) ---");
        prohibitTest();

        System.out.println("--- Trying prohibited call 1/3 (should error) ---");

        try {
            prohibitTest1();
        } catch (SecurityException ex) {
            System.err.println(">>> As expected, prohibited call 1/3 failed");
            ex.printStackTrace();
        }

        System.out.println("--- Trying prohibited call 2/3 (should error) ---");

        try {
            prohibitTest2();
        } catch (SecurityException ex) {
            System.err.println(">>> As expected, prohibited call 2/3 failed");
            ex.printStackTrace();
        }

        System.out.println("--- Trying prohibited call 3/3 (should error) ---");

        try {
            otherProhibitedTest();
        } catch (SecurityException ex) {
            System.err.println(">>> As expected, prohibited call 3/3 failed");
            ex.printStackTrace();
        }

        System.out.println(" \n \n * END DEMO * \n ");
    }

    private static void first(int x) {
        test(x);
    }

    private static void second(int x) {
        test(x);
    }

    private static void prohibitTest1() {
        prohibitTest();
    }

    private static void prohibitTest2() {
        prohibitTest();
    }
    
    private static void otherProhibitedTest() {
        prohibitTest();
    }

    @RestrictedCall (
            preserveThisAnnotation      = true,
            prohibitReflectionTraces    = true,
            prohibitNativeTraces        = true,
            prohibitArbitraryInvocation = true,
            permittedSources            = "me.darksidecode.accesswarden.demo.AccessWardenDemo#first"
    )
    private static void test(int x) {
        System.out.println(">>> Successful test call, x=" + x);
    }

    @RestrictedCall (
            prohibitedSources = {
                    "me.darksidecode.accesswarden.demo.AccessWardenDemo#prohibitTest*",
                    "me.darksidecode.accesswarden.demo.AccessWardenDemo#otherProhibitedTest"
            }
    )
    private static void prohibitTest() {
        System.out.println(">>> Successful prohibitTest call");
    }

}
