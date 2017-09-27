package org.sandbox.groovy;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.sandbox.groovy.customizers.FinalFieldsFromMapParam;
import org.sandbox.groovy.customizers.MethodParamNames;
import org.sandbox.groovy.customizers.PojoClass;
import org.sandbox.groovy.customizers.SamInterfaceImplementation;
import org.sandbox.groovy.customizers.VariablesFromMapParam;

import java.util.Map;
import java.util.function.Supplier;

import static java.util.Arrays.stream;

public class Customizers {
    public static ImportCustomizer imports(Class<?>... classes) {
        final ImportCustomizer importCustomizer = new ImportCustomizer();
        stream(classes).map(Class::getCanonicalName).forEach(importCustomizer::addImports);
        return importCustomizer;
    }

    public static ImportCustomizer staticImports(Class<?> clazz, String... members) {
        final String className = clazz.getCanonicalName();
        final ImportCustomizer importCustomizer = new ImportCustomizer();
        if (members != null && members.length > 0) {
            stream(members).forEach(member -> importCustomizer.addStaticImport(className, member));
        } else {
            importCustomizer.addStaticStars(className);
        }
        return importCustomizer;
    }

    public static PojoClass pojoClass() {
        return new PojoClass();
    }

    public static SamInterfaceImplementation samImplementation(ClassNode samInterface) {
        return new SamInterfaceImplementation(samInterface);
    }

    public static MethodParamNames paramNames(Supplier<MethodNode> methodNodeSupplier, String... paramNames) {
        return new MethodParamNames(methodNodeSupplier, paramNames);
    }

    public static VariablesFromMapParam varsFromMap(
            Supplier<MethodNode> methodNodeSupplier, String mapParamName, Map<String, ClassNode> variableDefinitions
    ) {
        return new VariablesFromMapParam(methodNodeSupplier, mapParamName, variableDefinitions);
    }

    public static FinalFieldsFromMapParam fieldsFromMap(Map<String, ClassNode> fieldDefinitions) {
        return new FinalFieldsFromMapParam(fieldDefinitions);
    }
}
