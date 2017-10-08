package org.sandbox.groovy.customizers;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharStreams;
import groovy.lang.GroovyObject;
import groovy.lang.Script;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.GenericsType;
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
import java.util.Set;

import static groovyjarjarasm.asm.Opcodes.ACC_PUBLIC;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringEscapeUtils.escapeJava;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.join;
import static org.codehaus.groovy.ast.ClassHelper.OBJECT_TYPE;
import static org.codehaus.groovy.ast.ClassHelper.STRING_TYPE;
import static org.codehaus.groovy.ast.tools.GeneralUtils.constX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.fieldX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.plusX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.returnS;
import static org.codehaus.groovy.control.CompilePhase.CONVERSION;
import static org.sandbox.groovy.ClassNodes.classNode;

/**
 * {@link CompilationCustomizer} that defines {@link Object#toString() toString} method.
 * <p>
 *     Constructed {@code toString()} method reflects:
 *     <ul>
 *         <li> Class name; </li>
 *         <li> Superclass name, if the latter is defined; </li>
 *         <li> List of implemented interfaces; </li>
 *         <li> Declared non-static non-synthetic fields; </li>
 *         <li> Groovy script text, if the class is defined from Groovy script. </li>
 *     </ul>
 * </p>
 * <p>
 *     This customizer is executed at {@link org.codehaus.groovy.control.CompilePhase#CONVERSION CONVERSION} phase.
 * </p>
 */
public class ExplicitToString extends CompilationCustomizer {

    private static final Set<ClassNode> EXCLUDED_CLASSES = ImmutableSet.of(
            OBJECT_TYPE,
            classNode(Script.class),
            classNode(GroovyObject.class)
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

        final String header = header(classNode);
        final String footer = footer(classNode, source.getSource());
        final Statement body = body(classNode, header, footer);

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

    private Statement body(ClassNode classNode, String header, String footer) {
        final List<FieldNode> fields = classNode.getFields();

        Expression expression = constX(header);
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

        expression = plusX(expression, constX(footer));

        final BlockStatement result = new BlockStatement();
        result.addStatement(returnS(expression));

        return result;
    }

    private String toString(ClassNode classNode) {
        final StringBuilder result = new StringBuilder(classNode.getText());

        final GenericsType[] genericsTypes = classNode.getGenericsTypes();

        if (genericsTypes == null || genericsTypes.length == 0) {
            return result.toString();
        }

        result.append('<');

        boolean first = true;
        for (GenericsType genericsType : genericsTypes) {
            if (!first) {
                result.append(", ");
            } else {
                first = false;
            }

            result.append(genericsType);
        }

        result.append('>');

        return result.toString();
    }

    private String header(ClassNode classNode) {
        final StringBuilder header = new StringBuilder().append("class ").append(classNode.getText());

        if (!EXCLUDED_CLASSES.contains(classNode.getSuperClass())) {
            header.append(" extends ").append(classNode.getSuperClass().getText());
        }


        final List<String> interfaces = stream(classNode.getInterfaces())
                .filter(i -> !EXCLUDED_CLASSES.contains(i))
                .map(this::toString)
                .collect(toList());

        if (!interfaces.isEmpty()) {
            header.append(" implements ").append(join(interfaces, ", "));
        }

        return header.append(" {").toString();
    }

    private String footer(ClassNode classNode, ReaderSource source) {
        final StringBuilder footer = new StringBuilder("}");

        if (!classNode.isScript()) {
            return footer.toString();
        }

        final String sourceText = sourceText(source);

        footer.append(", compiled from Groovy script: ");
        if (isEmpty(sourceText)) {
            footer.append("<unknown>");
        } else {
            footer.append('"').append(escapeJava(sourceText)).append('"');
        }

        return footer.toString();
    }

    private String sourceText(ReaderSource source) {
        try {
            return source.canReopenSource() ? CharStreams.toString(source.getReader()) : null;

        } catch (IOException e) {
            return null;
        }
    }
}
