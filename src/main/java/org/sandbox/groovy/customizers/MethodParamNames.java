package org.sandbox.groovy.customizers;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;

import java.util.function.Supplier;

import static java.lang.String.format;
import static org.codehaus.groovy.control.CompilePhase.CONVERSION;

public class MethodParamNames extends CompilationCustomizer {
    private final Supplier<MethodNode> methodNodeSupplier;
    private final String[] paramNames;

    public MethodParamNames(Supplier<MethodNode> methodNodeSupplier, String... paramNames) {
        super(CONVERSION);

        this.methodNodeSupplier = methodNodeSupplier;
        this.paramNames = paramNames;
    }

    @Override
    public void call(
            SourceUnit source, GeneratorContext context, ClassNode classNode
    ) throws CompilationFailedException {
        final MethodNode methodNode = methodNodeSupplier.get();

        if (!methodNode.getDeclaringClass().equals(classNode)) {
            return;
        }

        final Parameter[] paramsSrc = requireParametersCount(methodNode);
        final Parameter[] paramsDst = new Parameter[paramsSrc.length];

        for (int i = 0; i < paramsSrc.length; ++i) {
            final Parameter paramSrc = paramsSrc[i];
            final Parameter paramDst = new Parameter(paramSrc.getType(), paramNames[i]);

            paramDst.setClosureSharedVariable(paramSrc.isClosureSharedVariable());
            paramDst.setInitialExpression(paramSrc.getInitialExpression());
            paramDst.setInStaticContext(paramSrc.isInStaticContext());
            paramDst.setModifiers(paramSrc.getModifiers());
            paramDst.setOriginType(paramSrc.getOriginType());

            paramDst.addAnnotations(paramSrc.getAnnotations());

            paramDst.setSourcePosition(paramSrc);
            paramDst.copyNodeMetaData(paramSrc);

            paramsDst[i] = paramDst;
        }

        methodNode.setParameters(paramsDst);
    }

    private Parameter[] requireParametersCount(MethodNode methodNode) {
        final Parameter[] paramsSrc = methodNode.getParameters();
        if (paramsSrc.length != paramNames.length) {
            throw new IllegalStateException(format(
                    "Expected: Method with %s parameters\n" +
                            "Found: %s", paramNames.length, methodNode
            ));
        }
        return paramsSrc;
    }
}
