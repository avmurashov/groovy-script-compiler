package org.sandbox.groovy.customizers;

import com.google.common.io.CharStreams;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;
import org.codehaus.groovy.control.io.ReaderSource;

import java.io.IOException;
import java.util.List;

import static groovyjarjarasm.asm.Opcodes.ACC_PUBLIC;
import static org.apache.commons.lang3.StringEscapeUtils.escapeJava;
import static org.codehaus.groovy.ast.ClassHelper.STRING_TYPE;
import static org.codehaus.groovy.ast.tools.GeneralUtils.callX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.constX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.fieldX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.plusX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.returnS;
import static org.codehaus.groovy.ast.tools.GeneralUtils.varX;
import static org.codehaus.groovy.control.CompilePhase.CONVERSION;
import static org.sandbox.groovy.ClassNodes.classNode;

/**
 * {@link CompilationCustomizer} that defines {@link Object#toString() toString} method.
 * <p>
 *     Constructed {@code toString()} method reflects:
 *     <ul>
 *         <li> Class name; </li>
 *         <li> Identity hash-code; </li>
 *         <li> Declared non-static non-synthetic fields; </li>
 *         <li> Groovy script text. </li>
 *     </ul>
 * </p>
 * <p>
 *     This customizer is executed at {@link org.codehaus.groovy.control.CompilePhase#CONVERSION CONVERSION} phase.
 * </p>
 */
public class ExplicitToString extends CompilationCustomizer {

    private static final String UNKNOWN_SOURCE = "<unknown source>";

    private static final Expression IDENTITY_HASH = callX(
            classNode(Integer.class), "toHexString", callX(
                    classNode(System.class), "identityHashCode", varX("this")
            )
    );

    public ExplicitToString() {
        super(CONVERSION);
    }

    @Override
    public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
        final ModuleNode module = source.getAST();

        if (!classNode.getName().equals(module.getMainClassName())) {
            return;
        }

        final Statement body = body(classNode, source.getSource());

        final MethodNode toStringMethod = new MethodNode(
                "toString",
                ACC_PUBLIC,
                STRING_TYPE,
                Parameter.EMPTY_ARRAY,
                ClassNode.EMPTY_ARRAY,
                body
        );

        classNode.addMethod(toStringMethod);
    }



    private Statement body(ClassNode classNode, ReaderSource source) {
        final List<FieldNode> fields = classNode.getFields();
        final String script = extractScript(source);

        Expression expression = constX(classNode.getText() + "@");

        expression = plusX(expression, IDENTITY_HASH);
        expression = plusX(expression, constX(" {"));

        boolean first = true;

        for (final FieldNode f : fields) {
            if (f.isStatic() || f.isSynthetic()) {
                continue;
            }

            if (!first) {
                expression = plusX(expression, constX(", "));
            } else {
                first = false;
            }

            expression = plusX(expression, constX(f.getName()));
            expression = plusX(expression, constX("="));
            expression = plusX(expression, fieldX(f));
        }

        expression = plusX(expression, constX("} // " + script));

        final BlockStatement result = new BlockStatement();
        result.addStatement(returnS(expression));

        return result;
    }


    private String extractScript(ReaderSource source) {

        try {
            if (!source.canReopenSource()) {
                return UNKNOWN_SOURCE;
            }

            final String originalSource = CharStreams.toString(source.getReader());
            return escapeJava(originalSource);

        } catch (IOException e) {
            return UNKNOWN_SOURCE;
        }
    }
}
