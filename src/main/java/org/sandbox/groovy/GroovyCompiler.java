package org.sandbox.groovy;

import groovy.lang.GroovyClassLoader;
import groovy.transform.CompileStatic;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.codehaus.groovy.tools.GroovyClass;
import org.sandbox.groovy.customizers.ExplicitToString;
import org.sandbox.groovy.customizers.FinalFieldsFromMapParam;
import org.sandbox.groovy.customizers.MethodParamNames;
import org.sandbox.groovy.customizers.PojoClass;
import org.sandbox.groovy.customizers.SamInterfaceImplementation;
import org.sandbox.groovy.customizers.VariablesFromMapParam;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.stream;
import static org.codehaus.groovy.control.Phases.CLASS_GENERATION;
import static org.codehaus.groovy.control.Phases.OUTPUT;

/**
 * Groovy compiler.
 * <p>
 *     This class may be used to {@link #compile(String) compile} Groovy script or class into Java byte code.
 * </p>
 * <p>
 *     Instances of this class should be {@link #close() closed} after use. Compiled Java classes will stay intact.
 * </p>
 */
public class GroovyCompiler implements AutoCloseable {
    private static final AtomicInteger COMPILERS_SEQ = new AtomicInteger();

    private final int compilerSeqNo;
    private final AtomicInteger compilationsSeq;
    private final GroovyClassLoader groovyClassLoader;

    /** Creates new {@code GroovyCompiler}. */
    public GroovyCompiler() {
        compilerSeqNo = COMPILERS_SEQ.getAndIncrement();
        compilationsSeq = new AtomicInteger();
        groovyClassLoader = new GroovyClassLoader(currentThread().getContextClassLoader());
    }

    /**
     * Creates new {@code GroovyCompilation} for the given Groovy source code text.
     *
     * @param text Groovy source code text, may represent both scripts and classes.
     * @return {@code GroovyCompilation} for the given Groovy source code.
     */
    public GroovyCompilation compile(String text) {
        return new GroovyCompilation(groovyClassLoader, text, compilerSeqNo, compilationsSeq.getAndIncrement());
    }

    /**
     * Closes given {@code GroovyCompiler}.
     * <p>
     *     All the compiled Java classes will stay intact after close.
     * </p>
     *
     * @throws Exception If any issue happened.
     */
    @Override
    public void close() throws Exception {
        groovyClassLoader.close();
    }

    /**
     * Groovy source code compilation.
     * <p>
     *     This class is backed by {@code CompilerConfiguration}, and as such, it supports all the {@link
     *     CompilerConfiguration#CompilerConfiguration(Properties) Groovy Compiler Configuration Properties} with the
     *     following deviations:
     *     <ul>
     *         <li>
     *             If supported by Groovy, current JDK version is {@link CompilerConfiguration#setTargetBytecode(String)
     *             set} as target byte code. Otherwise the standard Groovy configuration is used.
     *         </li>
     *         <li>
     *             Class files are generated if and only if the <code>"groovy.target.directory"</code> system property
     *             is defined. The standard Groovy approach to generate class files in current working directory is
     *             suppressed.
     *         </li>
     *     </ul>
     * </p>
     * <p>
     *     {@code GroovyCompilation} may be {@link #thenApply(CompilationCustomizer...) customized} with standard Groovy
     *     {@link CompilationCustomizer}-s. After all customizations are set up, one should trigger compilation {@link
     *     #toClass() to Class}.
     * </p>
     * <p>
     *     {@code ASTTransformationCustomizer} for {@code CompileStatic} feature is always added as the last {@code
     *     CompilationCustomizer} when the Groovy code is compiled to Class.
     * </p>
     *
     * @see #thenApplyImports
     * @see #thenApplyStaticImports
     * @see #thenApplyPojoClass
     * @see #thenApplySamImplementation
     * @see #thenApplyParamNames
     * @see #thenApplyVarsFromMap
     * @see #thenApplyFieldsFromMap
     * @see #thenApplyExplicitToString
     */
    public static class GroovyCompilation {
        private final CompilerConfiguration configuration;
        private final GroovyClassLoader classLoader;
        private final String code;

        private final int compilerSeqNo;
        private final int compilationSeqNo;

        private GroovyCompilation(
                GroovyClassLoader groovyClassLoader,
                String code,
                int compilerSeqNo,
                int compilationSeqNo
        ) {
            this.classLoader = groovyClassLoader;
            this.code = code;

            this.compilerSeqNo = compilerSeqNo;
            this.compilationSeqNo = compilationSeqNo;

            this.configuration = new CompilerConfiguration();
            this.configuration.setTargetBytecode(Runtime.class.getPackage().getSpecificationVersion());
        }

        /**
         * Customizes the given {@code GroovyCompilation} with specified {@code CompilationCustomizer}-s.
         * <p>
         *     {@code CompilationCustomizer}-s of the same compilation phase are applied in the order they were
         *     registered with given method.
         * </p>
         *
         * @param compilationCustomizers compilation customizers to use for this Groovy compilation.
         * @return This {@code GroovyCompilation} after all the {@code compilationCustomizers} are registered.
         */
        public GroovyCompilation thenApply(CompilationCustomizer... compilationCustomizers) {
            configuration.addCompilationCustomizers(compilationCustomizers);
            return this;
        }

        /**
         * Compiles Groovy code to Java {@link Class}.
         *
         * @return {@code Class} constructed for the Groovy code, {@link #thenApply(CompilationCustomizer...)
         * customized} with the help of registered {@code CompilationCustomizer}-s.
         * @throws RuntimeException If any issue happened. Notice that any {@code RuntimeException} may be thrown, not
         * only the {@link CompilationFailedException}.
         */
        public Class<?> toClass() throws RuntimeException {
            try {
                configuration.addCompilationCustomizers(new ASTTransformationCustomizer(CompileStatic.class));

                final CompilationUnit unit = new CompilationUnit(configuration, null, classLoader);
                unit.addSource(String.format("GroovyScript_%03d_%03d", compilerSeqNo, compilationSeqNo), code);
                unit.compile(CLASS_GENERATION);

                if (configuration.getTargetDirectory() != null) {
                    unit.compile(OUTPUT);
                }

                unit.gotoPhase(Phases.FINALIZATION);
                unit.completePhase();

                for (Object compiledClass : unit.getClasses()) {
                    final GroovyClass groovyClass = (GroovyClass) compiledClass;
                    classLoader.defineClass(groovyClass.getName(), groovyClass.getBytes());
                }

                return classLoader.loadClass(unit.getFirstClassNode().getName());

            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

        }

        /**
         * Registers {@link ImportCustomizer} that is configured to import provided classes.
         *
         * @param classes Classes to import during the source code compilation.
         * @return This {@code GroovyCompilation} after the {@code ImportCustomizer} is registered.
         */
        public GroovyCompilation thenApplyImports(Class<?>... classes) {
            final ImportCustomizer importCustomizer = new ImportCustomizer();
            stream(classes).map(Class::getCanonicalName).forEach(importCustomizer::addImports);
            return thenApply(importCustomizer);
        }

        /**
         * Registers {@link ImportCustomizer} that is configured to import all or specific static members of given
         * class.
         *
         * @param clazz Class whose static members to import during the source code compilation.
         * @param staticMembers Names of the {@code clazz}' static members to import. If empty, the star import will be
         * used.
         * @return This {@code GroovyCompilation} after the {@code ImportCustomizer} is registered.
         */
        public GroovyCompilation thenApplyStaticImports(Class<?> clazz, String... staticMembers) {
            final String className = clazz.getCanonicalName();
            final ImportCustomizer importCustomizer = new ImportCustomizer();

            if (staticMembers != null && staticMembers.length > 0) {
                stream(staticMembers).forEach(member -> importCustomizer.addStaticImport(className, member));
            } else {
                importCustomizer.addStaticStars(className);
            }

            return thenApply(importCustomizer);
        }

        /**
         * Registers {@link PojoClass} customizer to drop-off {@code GroovyScript} infrastructure from the script class
         * definition.
         *
         * @return This {@code GroovyCompilation} after the {@code PojoClass} is registered.
         */
        public GroovyCompilation thenApplyPojoClass() {
            return thenApply(new PojoClass());
        }

        /**
         * Registers {@link SamInterfaceImplementation} customizer that is configured to implement the given SAM
         * interface.
         *
         * @param samInterface SAM interface to implement.
         * @return This {@code GroovyCompilation} after the {@code SamInterfaceImplementation} is registered.
         */
        public GroovyCompilation thenApplySamImplementation(ClassNode samInterface) {
            return thenApply(new SamInterfaceImplementation(samInterface));
        }

        /**
         * Registers {@link MethodParamNames} customizer that is configured to assign the given names to a method formal
         * parameters.
         *
         * @param methodNodeSupplier Supplier of the {@code MethodNode} to assign formal parameter names. Returned
         * method must have exactly the same number of formal parameters as the number of provided parameter names.
         * @param paramNames Names to be assigned to method formal parameters.
         * @return This {@code GroovyCompilation} after the {@code MethodParamNames} is registered.
         */
        public GroovyCompilation thenApplyParamNames(Supplier<MethodNode> methodNodeSupplier, String... paramNames) {
            return thenApply(new MethodParamNames(methodNodeSupplier, paramNames));
        }

        /**
         * Registers {@link VariablesFromMapParam} customizer that is configured to define typed local variables based
         * on provided type information.
         *
         * @param methodNodeSupplier Supplier of the {@code MethodNode} to define typed local variables for. Returned
         * method must have formal parameter of type {@code Map<String, ?>} with the name, equal to
         * {@code mapParamName}.
         * @param mapParamName Name of the formal parameter of type {@code Map<String, ?>} to initialize local variables
         * from.
         * @param variableDefinitions Variable definitions in the form of map, where the key defines both local variable
         * name and the key in the map, referred by {@code mapParamName}, and the value defines local variable type.
         * @return This {@code GroovyCompilation} after the {@code VariablesFromMapParam} is registered.
         */
        public GroovyCompilation thenApplyVarsFromMap(
                Supplier<MethodNode> methodNodeSupplier,
                String mapParamName,
                Map<String, ClassNode> variableDefinitions
        ) {
            return thenApply(new VariablesFromMapParam(methodNodeSupplier, mapParamName, variableDefinitions));
        }

        /**
         * Registers {@link FinalFieldsFromMapParam} customizer that is configured to define typed final fields
         * initialized by constructor from the {@code Map<String, ?>}.
         *
         * @param fieldDefinitions Final fields definitions in the form of map, where the key defines both the field
         * name and the key in the map, accepted by a constructor, and the value defines the final field type.
         * @return This {@code GroovyCompilation} after the {@code FinalFieldsFromMapParam} is registered.
         */
        public GroovyCompilation thenApplyFieldsFromMap(Map<String, ClassNode> fieldDefinitions) {
            return thenApply(new FinalFieldsFromMapParam(fieldDefinitions));
        }

        /**
         * Registers {@link ExplicitToString} customizer that defines {@code toString()} method, listing the non-static
         * non-synthetic fields plus the script text, if the class is generated from the Groovy script.
         *
         * @return This {@code GroovyCompilation} after the {@code FinalFieldsFromMapParam} is registered.
         */
        public GroovyCompilation thenApplyExplicitToString() {
            return thenApply(new ExplicitToString());
        }

    }

}
