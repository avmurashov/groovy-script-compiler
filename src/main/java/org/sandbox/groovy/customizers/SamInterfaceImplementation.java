package org.sandbox.groovy.customizers;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;

import java.util.Map;

import static java.util.Objects.isNull;
import static org.codehaus.groovy.ast.ClassHelper.findSAM;
import static org.codehaus.groovy.ast.ClassHelper.isSAMType;
import static org.codehaus.groovy.ast.tools.GenericsUtils.correctToGenericsSpec;
import static org.codehaus.groovy.ast.tools.GenericsUtils.createGenericsSpec;
import static org.codehaus.groovy.control.CompilePhase.CONVERSION;
import static org.objectweb.asm.Opcodes.ACC_ABSTRACT;

public class SamInterfaceImplementation extends CompilationCustomizer {
    private final ClassNode samInterface;

    private MethodNode samImpl = null;

    public SamInterfaceImplementation(ClassNode samInterface) {
        super(CONVERSION);

        this.samInterface = requireSamInterface(samInterface);
    }

    @Override
    public void call(
            SourceUnit source, GeneratorContext context, ClassNode classNode
    ) throws CompilationFailedException {
        final ModuleNode module = source.getAST();

        if (!classNode.getName().equals(module.getMainClassName())) {
            return;
        }

        final BlockStatement code = module.getStatementBlock();

        final Map<String, ClassNode> genericsSpec = createGenericsSpec(samInterface);
        final MethodNode sam = correctToGenericsSpec(genericsSpec, findSAM(samInterface));

        samImpl = new MethodNode(
                sam.getName(),
                sam.getModifiers() & ~ACC_ABSTRACT,
                sam.getReturnType(),
                sam.getParameters(),
                sam.getExceptions(),
                code
        );

        samImpl.addAnnotations(sam.getAnnotations());

        samImpl.setSourcePosition(sam);
        samImpl.copyNodeMetaData(sam);

        classNode.addInterface(samInterface);
        classNode.addMethod(samImpl);
    }

    public MethodNode getSamImpl() throws IllegalStateException {
        if (isNull(samImpl)) {
            throw new IllegalStateException(
                    getClass().getSimpleName() + " must be called before accessing SAM implementation"
            );
        }
        return samImpl;
    }

    private static ClassNode requireSamInterface(ClassNode classNode) {
        if (isSAMType(classNode)) {
            return classNode;
        }

        throw new IllegalArgumentException(
                "Expected: Single Abstract Method (functional) interface\n" +
                        "Found: " + classNode
        );
    }
}
