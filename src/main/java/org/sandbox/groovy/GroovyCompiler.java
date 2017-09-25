package org.sandbox.groovy;

import groovy.lang.GroovyClassLoader;
import groovy.transform.CompileStatic;
import org.codehaus.groovy.control.CompilationUnit;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.tools.GroovyClass;

import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Thread.currentThread;
import static org.codehaus.groovy.control.Phases.CLASS_GENERATION;
import static org.codehaus.groovy.control.Phases.OUTPUT;

public class GroovyCompiler implements AutoCloseable {
    private static final AtomicInteger COMPILERS_SEQ = new AtomicInteger();

    private final int compilerSeqNo;
    private final AtomicInteger compilationsSeq;
    private final GroovyClassLoader groovyClassLoader;

    public GroovyCompiler() {
        compilerSeqNo = COMPILERS_SEQ.getAndIncrement();
        compilationsSeq = new AtomicInteger();
        groovyClassLoader = new GroovyClassLoader(currentThread().getContextClassLoader());
    }

    public GroovyCompilation compile(String text) {
        return new GroovyCompilation(groovyClassLoader, text, compilerSeqNo, compilationsSeq.getAndIncrement());
    }

    @Override
    public void close() throws Exception {
        groovyClassLoader.close();
    }

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
            this.configuration = new CompilerConfiguration();
            this.classLoader = groovyClassLoader;
            this.code = code;

            this.compilerSeqNo = compilerSeqNo;
            this.compilationSeqNo = compilationSeqNo;
        }

        public GroovyCompilation with(CompilationCustomizer... compilationCustomizers) {
            configuration.addCompilationCustomizers(compilationCustomizers);
            return this;
        }

        public Class<?> toClass() {
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
