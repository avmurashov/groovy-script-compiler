package org.sandbox.groovy;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.sandbox.groovy.customizers.FinalFieldsFromMapParam;
import org.sandbox.groovy.customizers.MethodParamNames;
import org.sandbox.groovy.customizers.PojoClass;
import org.sandbox.groovy.customizers.SamInterfaceImplementation;
import org.sandbox.groovy.customizers.VariablesFromMapParam;

import java.util.Map;
import java.util.function.Supplier;

import static java.util.Arrays.stream;

/**
 * Collection of {@link CompilationCustomizer}-s.
 *
 * @see #imports(Class[]) imports
 * @see #staticImports(Class, String...) staticImports
 * @see #pojoClass() pojoClass
 * @see #samImplementation(ClassNode) samImplementation
 * @see #paramNames(Supplier, String...) paramNames
 * @see #varsFromMap(Supplier, String, Map) varsFromMap
 * @see #fieldsFromMap(Map) fieldsFromMap
 */
public class Customizers {
    /**
     * Constructs {@link ImportCustomizer} that is configured to import provided classes.
     *
     * @param classes Classes to import during the source code compilation.
     * @return {@link ImportCustomizer} that is configured to import provided classes.
     */
    public static ImportCustomizer imports(Class<?>... classes) {
        final ImportCustomizer importCustomizer = new ImportCustomizer();
        stream(classes).map(Class::getCanonicalName).forEach(importCustomizer::addImports);
        return importCustomizer;
    }

    /**
     * Constructs {@link ImportCustomizer} that is configured to import all or specific static members of given class.
     *
     * @param clazz Class whose static members to import during the source code compilation.
     * @param members Names of the {@code clazz}' static members to import. If empty, the star import will be used.
     * @return {@link ImportCustomizer} that is configured to import all or specific static members of given class.
     */
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

    /**
     * Constructs {@link PojoClass} customizer.
     *
     * @return {@link PojoClass} customizer.
     */
    public static PojoClass pojoClass() {
        return new PojoClass();
    }

    /**
     * Constructs {@link SamInterfaceImplementation} customizer that is configured to implement the given SAM interface.
     *
     * @param samInterface SAM interface to implement.
     * @return {@link SamInterfaceImplementation} customizer that is configured to implement the given SAM interface.
     */
    public static SamInterfaceImplementation samImplementation(ClassNode samInterface) {
        return new SamInterfaceImplementation(samInterface);
    }

    /**
     * Constructs {@link MethodParamNames} customizer that is configured to assign the given names to a method formal
     * parameters.
     *
     * @param methodNodeSupplier
     * Supplier of the {@code MethodNode} to assign formal parameter names. Returned method must have exactly the same
     * number of formal parameters as the number of provided parameter names.
     * @param paramNames
     * Names of the method formal parameters.
     * @return {@link MethodParamNames} customizer that is configured to assign the given names to a method formal
     * parameters.
     */
    public static MethodParamNames paramNames(Supplier<MethodNode> methodNodeSupplier, String... paramNames) {
        return new MethodParamNames(methodNodeSupplier, paramNames);
    }

    /**
     * Constructs {@link VariablesFromMapParam} customizer that is configured to define typed local variables based on
     * provided type information.
     *
     * @param methodNodeSupplier
     * Supplier of the {@code MethodNode} to define typed local variables for. Returned method must have formal
     * parameter of type {@code Map<String, ?>} with the name, equal to {@code mapParamName}.
     * @param mapParamName
     * Name of the formal parameter of type {@code Map<String, ?>} to initialize local variables from.
     * @param variableDefinitions
     * Variable definitions in the form of map, where the key defines both local variable name and the key in the map,
     * referred by {@code mapParamName}, and the value defines local variable type.
     * @return {@link VariablesFromMapParam} customizer that is configured to define typed local variables based on
     * provided type information.
     */
    public static VariablesFromMapParam varsFromMap(
            Supplier<MethodNode> methodNodeSupplier, String mapParamName, Map<String, ClassNode> variableDefinitions
    ) {
        return new VariablesFromMapParam(methodNodeSupplier, mapParamName, variableDefinitions);
    }

    /**
     * Constructs {@link FinalFieldsFromMapParam} customizer that is configured to define typed final fields initialized
     * by constructor from the {@code Map<String, ?>}.
     *
     * @param fieldDefinitions
     * Final fields definitions in the form of map, where the key defines both the field name and the key in the map,
     * accepted by a constructor, and the value defines the final field type.
     * @return {@link FinalFieldsFromMapParam} customizer that is configured to define typed final fields initialized
     * by constructor from the {@code Map<String, ?>}.
     */
    public static FinalFieldsFromMapParam fieldsFromMap(Map<String, ClassNode> fieldDefinitions) {
        return new FinalFieldsFromMapParam(fieldDefinitions);
    }
}
