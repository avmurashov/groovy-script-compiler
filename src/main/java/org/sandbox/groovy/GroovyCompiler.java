package org.sandbox.groovy;

import groovy.lang.GroovyClassLoader;
import groovy.transform.CompileStatic;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.tools.GroovyClass;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.currentThread;
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
     * <p/>
     * All the compiled Java classes will stay intact after close.
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
     *     {@code GroovyCompilation} may be customized {@link #with(CompilationCustomizer...) with} standard Groovy
     *     {@link CompilationCustomizer}-s. After all customizations are set up, one should trigger compilation {@link
     *     #toClass() to Class}.
     * </p>
     * <p>
     *     {@code ASTTransformationCustomizer} for {@code CompileStatic} feature is always added as the last {@code
     *     CompilationCustomizer} when the Groovy code is compiled to Class.
     * </p>
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
         *
         * @param compilationCustomizers compilation customizers to use for this Groovy compilation.
         * @return This {@code GroovyCompilation} after all the {@code compilationCustomizers} are registered.
         */
        public GroovyCompilation with(CompilationCustomizer... compilationCustomizers) {
            configuration.addCompilationCustomizers(compilationCustomizers);
            return this;
        }

        /**
         * Compiles Groovy code to Java {@link Class}.
         *
         * @return {@code Class} constructed for the Groovy code {@link #with(CompilationCustomizer...) with} the help
         * of registered {@code CompilationCustomizer}-s.
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
    }
}
