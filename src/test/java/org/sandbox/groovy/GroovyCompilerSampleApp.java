package org.sandbox.groovy;

import com.google.common.collect.ImmutableMap;
import org.codehaus.groovy.ast.ClassNode;
import org.sandbox.groovy.customizers.SamInterfaceImplementation;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

import static java.util.Arrays.asList;
import static org.codehaus.groovy.ast.ClassHelper.double_TYPE;
import static org.sandbox.groovy.ClassNodes.classNode;
import static org.sandbox.groovy.ClassNodes.wildcard;

public class GroovyCompilerSampleApp {
    private static final String SCRIPT_TEXT = "" +
            "static double pow2(double x) { x * x }\n" +
            "double random() { rnd.nextDouble() }\n"+
            "\n" +
            "random() + sqrt(pow2(x) + pow2(y))";

    private static ClassNode TARGET_INTERFACE = classNode(
            ToDoubleFunction.class,
            classNode(Map.class, String.class, wildcard(Number.class))
    );

    private static Map<String, ClassNode> FIELDS = ImmutableMap.of(
            "rnd", classNode(Random.class)
    );

    private static Map<String, ClassNode> PARAMS = ImmutableMap.of(
            "x", double_TYPE,
            "y", double_TYPE
    );

    public static void main(String[] args) throws Exception {
        try (final GroovyCompiler compiler = new GroovyCompiler()) {
            final SamInterfaceImplementation samImpl = new SamInterfaceImplementation(TARGET_INTERFACE);

            final Class<?> clazz = compiler.takeSourceCode(SCRIPT_TEXT)
                    .thenApplyImports(Function.class)
                    .thenApplyStaticImports(Math.class, "sqrt")
                    .thenApplyPojoClass()
                    .thenApplyFieldsFromMap(FIELDS)
                    .thenApply(samImpl)
                    .thenApplyParamNames(samImpl::getSamImpl, "params")
                    .thenApplyVarsFromMap(samImpl::getSamImpl, "params", PARAMS)
                    .thenApplyExplicitToString()
                    .thenCompile();

            System.out.println("Class: " + clazz.getCanonicalName());
            System.out.println("Super class: " + clazz.getSuperclass().getCanonicalName());
            System.out.println("Declared ctors: " + asList(clazz.getConstructors()));
            System.out.println("Declared methods: " + asList(clazz.getDeclaredMethods()));
            System.out.println("Public methods: " + asList(clazz.getMethods()));


            System.out.println("Creating instance");
            final Constructor<?> ctor = clazz.getConstructor(Map.class);
            @SuppressWarnings("unchecked")
            final ToDoubleFunction<Map<String, Object>> fn = (ToDoubleFunction<Map<String, Object>>) ctor.newInstance(
                    ImmutableMap.of("rnd", new Random())
            );
            System.out.println("Created instance: " + fn);

            final double result = fn.applyAsDouble(ImmutableMap.of(
                    "x", 3d,
                    "y", 4d
            ));
            System.out.println("fn(x = 3, y = 4) = " + result);
        }
    }
}
