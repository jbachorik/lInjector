/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package test.injector;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.util.TraceClassVisitor;
import test.injector.api.Injector;

/**
 *
 * @author jbachorik
 */
public class Main {
    private static class MyEvent implements Injector.Event {
        public void setAttr1(String a) {}
        public void setAttr2(byte[] b) {}
    }
    
    public static void main(String[] args) throws Exception {
        Injector.Injectable i1 = Injector.forClass(FileInputStream.class).onMethods("read").
            addEvent(MyEvent.class, (e, ctx) -> {
                e.setAttr1(ctx.self.toString());
            });
        
        Injector.Injectable i2 = Injector.forClass(FileInputStream.class).onMethods("int read(byte[] b)").
            addEvent(MyEvent.class, (e, ctx) -> {
                e.setAttr1(ctx.self.toString());
                if (ctx.$("b").isPresent()) {
                    e.setAttr2((byte[])ctx.$("b").get());
                }
            });
        
        InputStream is = Main.class.getResourceAsStream("/test/injector/Main.class");
        ClassReader cr = new ClassReader(is);
        
        cr.accept(new TraceClassVisitor(new PrintWriter(System.err)), ClassReader.SKIP_DEBUG);
        
        System.err.println("*** " + Agent.LAMBDAS.size());
        Agent.LAMBDAS.entrySet().stream().forEach(e -> System.err.println(e.getKey() + " = " + e.getValue()));
        Injector.inject(i1, i2);
        
    }
}
