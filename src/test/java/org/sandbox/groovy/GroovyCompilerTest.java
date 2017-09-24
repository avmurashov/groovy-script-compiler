package org.sandbox.groovy;

import com.google.common.collect.ImmutableMap;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.GenericsType;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.sandbox.groovy.customizers.MethodParamNames;
import org.sandbox.groovy.customizers.PojoClass;
import org.sandbox.groovy.customizers.SamInterfaceImplementation;
import org.sandbox.groovy.customizers.VariablesFromMapParam;

import java.util.Map;
import java.util.function.ToDoubleFunction;

import static java.util.Arrays.asList;
import static org.codehaus.groovy.ast.ClassHelper.*;
import static org.codehaus.groovy.ast.tools.GenericsUtils.makeClassSafeWithGenerics;
import static org.sandbox.groovy.ClassNodes.classNode;

public class GroovyCompilerTest {
    public static void main(String[] args) throws Exception {
        try (final GroovyCompiler compiler = new GroovyCompiler()) {

            final ImportCustomizer imports = new ImportCustomizer();
            imports.addStaticStars(Math.class.getCanonicalName());

            final PojoClass pojoClass = new PojoClass();

//            final ClassNode iface = classNode(ToDoubleFunction.class, classNode(Map.class, String.class, Object.class));
            final ClassNode iface = classNode(MyFn.class);

            makeClassSafeWithGenerics(make(ToDoubleFunction.class),
                    new GenericsType(makeClassSafeWithGenerics(MAP_TYPE,
                            new GenericsType(STRING_TYPE), new GenericsType(OBJECT_TYPE))
                    )
            );

            final SamInterfaceImplementation samImpl = new SamInterfaceImplementation(iface);
            final MethodParamNames paramNames = new MethodParamNames(samImpl::getSamImpl, "params");
            final VariablesFromMapParam varsFromMap = new VariablesFromMapParam(samImpl::getSamImpl, "params", ImmutableMap.of(
                    "A", double_TYPE,
                    "B", double_TYPE
            ));

            final Class<?> clazz = compiler.compile("A + sqrt(B) - 1\n")
                    .with(imports)
                    .with(pojoClass)
                    .with(samImpl)
                    .with(paramNames)
                    .with(varsFromMap)
                    .toClass();

            System.out.println("Class: " + clazz.getCanonicalName());
            System.out.println("Super class: " + clazz.getSuperclass().getCanonicalName());
            System.out.println("Declared ctors: " + asList(clazz.getConstructors()));
            System.out.println("Declared methods: " + asList(clazz.getDeclaredMethods()));
            System.out.println("Public methods: " + asList(clazz.getMethods()));


            System.out.println("Creating instance with default ctor");
//            @SuppressWarnings("unchecked")
//            final ToDoubleFunction<Map<String, Object>> fn = (ToDoubleFunction<Map<String, Object>>) clazz.newInstance();
            final MyFn fn = (MyFn) clazz.newInstance();
            System.out.println("Created instance: " + fn);

            System.out.println(fn.applyAsDouble(ImmutableMap.of(
                    "A", -1d,
                    "B", 4d,
                    "C", 100d
            )));
        }
    }
}
