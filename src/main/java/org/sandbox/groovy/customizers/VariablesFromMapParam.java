package org.sandbox.groovy.customizers;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
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
import static org.codehaus.groovy.ast.tools.GeneralUtils.*;
import static org.codehaus.groovy.control.CompilePhase.CONVERSION;

public class VariablesFromMapParam extends CompilationCustomizer {
    private final Supplier<MethodNode> methodNodeSupplier;
    private final String mapParamName;
    private final Map<String, ClassNode> variableDefinitons;

    public VariablesFromMapParam(
            Supplier<MethodNode> methodNodeSupplier, String mapParamName, Map<String, ClassNode> variableDefinitons
    ) {
        super(CONVERSION);

        this.methodNodeSupplier = methodNodeSupplier;
        this.mapParamName = mapParamName;
        this.variableDefinitons = variableDefinitons;
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

        final List<Statement> varDefinitions = variableDefinitons.entrySet().stream()
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
        return declS(
                varX(entry.getKey(), entry.getValue()),
                castX(entry.getValue(), propX(varX(mapParamName), entry.getKey()))
        );
    }
}
