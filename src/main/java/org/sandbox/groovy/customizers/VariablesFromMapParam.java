package org.sandbox.groovy.customizers;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.codehaus.groovy.ast.tools.GeneralUtils.args;
import static org.codehaus.groovy.ast.tools.GeneralUtils.callX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.castX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.constX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.declS;
import static org.codehaus.groovy.ast.tools.GeneralUtils.varX;
import static org.codehaus.groovy.control.CompilePhase.CONVERSION;
import static org.objectweb.asm.Opcodes.ACC_FINAL;

public class VariablesFromMapParam extends CompilationCustomizer {
    private final Supplier<MethodNode> methodNodeSupplier;
    private final String mapParamName;
    private final Map<String, ClassNode> variableDefinitions;

    public VariablesFromMapParam(
            Supplier<MethodNode> methodNodeSupplier, String mapParamName, Map<String, ClassNode> variableDefinitions
    ) {
        super(CONVERSION);

        this.methodNodeSupplier = methodNodeSupplier;
        this.mapParamName = mapParamName;
        this.variableDefinitions = variableDefinitions;
    }

    @Override
    public void call(
            SourceUnit source, GeneratorContext context, ClassNode classNode
    ) throws CompilationFailedException {
        final MethodNode methodNode = methodNodeSupplier.get();

        if (!methodNode.getDeclaringClass().equals(classNode)) {
            return;
        }

        requireMapParameter(methodNode);

        final BlockStatement methodDefinition = (BlockStatement) methodNode.getCode();

        final List<Statement> varDefinitions = variableDefinitions.entrySet().stream()
                .map(this::variableDefinition).collect(toList());

        methodDefinition.getStatements().addAll(0, varDefinitions);
    }

    private void requireMapParameter(MethodNode methodNode) {
        final Predicate<Parameter> mapParam = p -> p.getName().equals(mapParamName) &&
                p.getType().getTypeClass().equals(Map.class) &&
                p.getType().getGenericsTypes()[0].getType().getTypeClass().equals(String.class);

        stream(methodNode.getParameters()).filter(mapParam).findAny().orElseThrow(() ->
                new IllegalArgumentException(String.format(
                        "Expected: Method with parameter Map<String, ?> %s\n" +
                                "Found: %s", mapParamName, methodNode
                ))
        );
    }

    private Statement variableDefinition(Map.Entry<String, ClassNode> entry) {
        final String varName = entry.getKey();
        final ClassNode varType = entry.getValue();

        final VariableExpression varX = varX(varName, varType);
        varX.setModifiers(ACC_FINAL);

        final MethodCallExpression mapGetX = callX( varX(mapParamName), "get", args(constX(varName)) );

        return declS(varX, castX(varType, mapGetX));
    }
}
