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

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.annotation.Annotation;

final class BytecodeUtils implements Opcodes {

    public static final String CHECKER_CLASS_NAME = "__access__warden__generated__/__CheckerClass__";

    private BytecodeUtils() {}

    static String describeAnnotation(Class<? extends Annotation> annoClass) {
        return "L" + annoClass.getName().replace(".", "/") + ";";
    }

    static ClassWriter beginCreateCheckerClass() {
        ClassWriter cw = new ClassWriter(0);

        // Final access to avoid any inheritance (utility-class).
        cw.visit(52, ACC_PUBLIC | ACC_SUPER | ACC_SYNTHETIC | ACC_FINAL,
                CHECKER_CLASS_NAME, null,
                "java/lang/Object", null);

        // Don't let anyone create an instance of this class (utility-class).
        MethodVisitor mv = cw.visitMethod(ACC_PRIVATE, "<init>", "()V", null, null);
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitLineNumber(10, l0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitLocalVariable("this", "L" + CHECKER_CLASS_NAME + ";", null, l0, l1, 0);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        return cw;
    }

    static void pushIntConstant(MethodVisitor mv, int ival) {
        switch (ival) {
            case 0:
                mv.visitInsn(ICONST_0);
                break;

            case 1:
                mv.visitInsn(ICONST_1);
                break;

            case 2:
                mv.visitInsn(ICONST_2);
                break;

            case 3:
                mv.visitInsn(ICONST_3);
                break;

            case 4:
                mv.visitInsn(ICONST_4);
                break;

            case 5:
                mv.visitInsn(ICONST_5);
                break;

            default:
                if (ival >= Byte.MIN_VALUE && ival <= Byte.MAX_VALUE)
                    mv.visitIntInsn(BIPUSH, ival);
                else if (ival >= Short.MIN_VALUE && ival <= Short.MAX_VALUE)
                    mv.visitIntInsn(SIPUSH, ival);
                else
                    mv.visitLdcInsn(ival);

                break;
        }
    }

}
