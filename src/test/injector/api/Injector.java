/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package test.injector.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.util.ASMifier;
import jdk.internal.org.objectweb.asm.util.CheckMethodAdapter;
import jdk.internal.org.objectweb.asm.util.Printer;
import jdk.internal.org.objectweb.asm.util.Textifier;
import jdk.internal.org.objectweb.asm.util.TraceClassVisitor;
import jdk.internal.org.objectweb.asm.util.TraceMethodVisitor;
import test.injector.Agent;

/**
 *
 * @author jbachorik
 */
public class Injector {
    public static interface Event {
        
    }
    
    public static class Context<C> {
        public C self;
        public Class<? extends C> clazz;
        public Optional<Object> $(String paramName) {
            return null;
        }
    }
    
    public static interface Injection {}
    
    @FunctionalInterface
    public static interface EventInjector<T extends Event, C> extends Injection {
        public void inject(T event, Context<C> injection);
    }
    
    public static interface Injectable {
        Injection getInjection();
    }
    
    public static class ClassInjector<F> {
        public Handler<F> onMethods(String ... methods) {
            return new Handler();
        }
    }
    
    public static class Handler<F>  {
        public <E extends Event> InjectableHandler<F> addEvent(Class<E> eventClass, EventInjector<E, F> injector) {
            return new InjectableHandler<>(eventClass, injector);
        }
    }
    
    public static class InjectableHandler<F> extends Handler<F> implements Injectable {
        private final Class<? extends Event> eventClass;
        private final EventInjector<? extends Event, F> injector;
        
        private <E extends Event> InjectableHandler(Class<? extends E> eventClass, EventInjector<E, F> injector) {
            this.eventClass = eventClass;
            this.injector = injector;
        }

        @Override
        public EventInjector<? extends Event, F> getInjection() {
            return injector;
        }
    }
    
    public static <T> ClassInjector<T> forClass(Class<? extends T> clz) {
        return new ClassInjector<>();
    }
    
    public static void inject(Injectable i1, Injectable ... i) {
        Collection<Injectable> injectables = new HashSet<>();
        injectables.add(i1);
        injectables.addAll(Arrays.asList(i));
        injectables.stream().forEach(in -> {
            String cName = in.getInjection().getClass().getName();
            cName = cName.substring(0, cName.indexOf('/')).replace('.', '/');
            
            String handler = Agent.LAMBDAS.get(cName);
            
            System.out.println(cName + " : " + handler);
            
            String[] params = handler.split("\\.");
            String handlerClass = "/" + params[0] + ".class";
            String handlerMethod = params[1];
            
            try {
                InputStream is = Injector.class.getResourceAsStream(handlerClass);

                ClassReader cr = new ClassReader(is);
                cr.accept(new ClassVisitor(Opcodes.ASM4) {
                    @Override
                    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                        MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                        
                        if (name.equals(handlerMethod)) {
                            mv = new MethodVisitor(Opcodes.ASM4, mv) {
                                // Translate the lamda body to injectable code
                            };
                        }
                        
                        return mv;
                    }
                }, ClassReader.SKIP_DEBUG);
            } catch (IOException e) {}
        });
    }
}
