/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package test.injector;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;

/**
 *
 * @author jbachorik
 */
public class Agent {
    public static final Map<String, String> LAMBDAS = new HashMap<>();

    final private static ClassFileTransformer transformer = (loader, className, classBeingRedefined, protectionDomain, classfileBuffer) -> {
        if (className == null) {
            ClassReader cr = new ClassReader(classfileBuffer);
            cr.accept(new ClassVisitor(Opcodes.ASM4) {
                private String lName = "";
                @Override
                public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                    System.out.println("+++ " + name);
                    lName = name;
                    super.visit(version, access, name, signature, superName, interfaces);
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                    if (name.equals("inject") &&
                        desc.equals("(Ltest/injector/api/Injector$Event;Ltest/injector/api/Injector$Context;)V")) {

                        mv = new MethodVisitor(Opcodes.ASM4, mv) {

                            @Override
                            public void visitMethodInsn(int opcode, String owner, String name, String desc) {
                                if (opcode == Opcodes.INVOKESTATIC) {
                                    LAMBDAS.put(lName, owner + "." + name);
                                }
                                super.visitMethodInsn(opcode, owner, name, desc);
                            }

                        };
                    }

                    return mv;
                }
            }, ClassReader.SKIP_DEBUG);
        }
        return null;
    };

    public static void premain(String agentArgs, final Instrumentation inst) {
        inst.addTransformer(transformer);
    }
}
