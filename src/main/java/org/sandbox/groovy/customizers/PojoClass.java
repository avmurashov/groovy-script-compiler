package org.sandbox.groovy.customizers;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;

import static java.util.stream.Collectors.toList;
import static org.codehaus.groovy.ast.ClassHelper.OBJECT_TYPE;
import static org.codehaus.groovy.control.CompilePhase.CONVERSION;

/**
 * {@code CompilationCustomizer} that drops superclass, constructor and method declarations from the main class of the
 * currently compiled module.
 * <p>
 *     This is handy when you need to compile Groovy script, but do not want the resulting class to extend
 *     {@link groovy.lang.Script Script} class and declare fields and methods, related to Groovy scripts support.
 * </p>
 * <p>
 *     This customizer is executed at {@link org.codehaus.groovy.control.CompilePhase#CONVERSION CONVERSION} phase.
 * </p>
 */
public class PojoClass extends CompilationCustomizer {
    public PojoClass() {
        super(CONVERSION);
    }

    @Override
    public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
        final ModuleNode module = source.getAST();

        if (!classNode.getName().equals(module.getMainClassName())) {
            return;
        }

        classNode.setSuperClass(OBJECT_TYPE);

        classNode.getDeclaredConstructors()
                .stream()
                .collect(toList())
                .forEach(classNode::removeConstructor);

        classNode.getMethods()
                .stream()
                .collect(toList())
                .forEach(classNode::removeMethod);

        module.getMethods().forEach(classNode::addMethod);
    }
}
