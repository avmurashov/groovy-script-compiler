package org.sandbox.groovy;

import com.google.common.collect.ImmutableMap;
import org.codehaus.groovy.ast.ClassNode;
import org.sandbox.groovy.customizers.SamInterfaceImplementation;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import static java.util.Arrays.asList;
import static org.codehaus.groovy.ast.ClassHelper.double_TYPE;
import static org.sandbox.groovy.ClassNodes.classNode;
import static org.sandbox.groovy.ClassNodes.wildcard;
import static org.sandbox.groovy.Customizers.fieldsFromMap;
import static org.sandbox.groovy.Customizers.paramNames;
import static org.sandbox.groovy.Customizers.pojoClass;
import static org.sandbox.groovy.Customizers.samImplementation;
import static org.sandbox.groovy.Customizers.staticImports;
import static org.sandbox.groovy.Customizers.varsFromMap;

public class GroovyCompilerTest {
    public static void main(String[] args) throws Exception {
        try (final GroovyCompiler compiler = new GroovyCompiler()) {
            final ClassNode iface = classNode(ToDoubleFunction.class, classNode(Map.class, String.class, wildcard()));
//            final ClassNode iface = classNode(MyFn.class);

            final SamInterfaceImplementation samImpl = samImplementation(iface);

            final Class<?> clazz = compiler.compile("A + sqrt(B) - 1 + C\n")
                    .with( staticImports(Math.class, "sqrt") )
                    .with( pojoClass() )
                    .with( fieldsFromMap(ImmutableMap.of("C", double_TYPE)) )
                    .with( samImpl )
                    .with( paramNames(samImpl::getSamImpl, "params") )
                    .with( varsFromMap(samImpl::getSamImpl, "params", ImmutableMap.of(
                            "A", double_TYPE,
                            "B", double_TYPE
                    )) )
                    .toClass();

            System.out.println("Class: " + clazz.getCanonicalName());
            System.out.println("Super class: " + clazz.getSuperclass().getCanonicalName());
            System.out.println("Declared ctors: " + asList(clazz.getConstructors()));
            System.out.println("Declared methods: " + asList(clazz.getDeclaredMethods()));
            System.out.println("Public methods: " + asList(clazz.getMethods()));


            System.out.println("Creating instance with default ctor");
            Constructor<?> ctor = clazz.getConstructor(Map.class);
            @SuppressWarnings("unchecked")
            final ToDoubleFunction<Map<String, Object>> fn = (ToDoubleFunction<Map<String, Object>>) ctor.newInstance(
                    ImmutableMap.of("C", 200d)
            );
//            final MyFn fn = (MyFn) clazz.newInstance();
            System.out.println("Created instance: " + fn);

            System.out.println(fn.applyAsDouble(ImmutableMap.of(
                    "A", -1d,
                    "B", 4d,
                    "D", 100d
            )));
        }
    }
}
