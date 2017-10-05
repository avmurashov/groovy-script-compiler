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

/**
 * {@link CompilationCustomizer} that converts Groovy script to a Single Abstract Method (Functional) interface
 * implementation.
 * <p>
 *     The method, generated to implement SAM, may be accessed via {@link #getSamImpl() samImpl} property. It is an
 *     error to access this property before the given customizer is applied. Please make sure that you call it only for
 *     customizers, executed after this one, i.e. those that are called on later phases, or called on the same phase but
 *     registered after the given customizer.
 * </p>
 * <p>
 *     This customizer is executed at {@link org.codehaus.groovy.control.CompilePhase#CONVERSION CONVERSION} phase.
 * </p>
 *
 * @see #SamInterfaceImplementation(ClassNode) constructor
 */
public class SamInterfaceImplementation extends CompilationCustomizer {
    private final ClassNode samInterface;

    private MethodNode samImpl = null;

    /**
     * Constructs {@code SamInterfaceImplementation}, configured to implement the given SAM interface.
     *
     * @param samInterface
     * {@code ClassNode}, representing Java interface with Single Abstract Method. Generics of any complexity are
     * supported, e.g. {@code Function<Map<String, ? extends Number>, BigDecimal>}.
     */
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

        samImpl.copyNodeMetaData(sam);
        samImpl.copyNodeMetaData(code);
        samImpl.setSourcePosition(code);

        classNode.addInterface(samInterface);
        classNode.addMethod(samImpl);
    }

    /**
     * Returns {@code MethodNode}, describing the Single Abstract Method implementation.
     * <p>
     *     It is an error to access this property before the given customizer is {@link #call(SourceUnit,
     *     GeneratorContext, ClassNode) called}. Please make sure you access it only for those customizers that are
     *     registered to be called after this customizer.
     * </p>
     *
     * @return {@code MethodNode}, describing the SAM method implementation.
     * @throws IllegalStateException If this property is accessed before the given customizer is called.
     */
    public MethodNode getSamImpl() throws IllegalStateException {
        if (isNull(samImpl)) {
            throw new IllegalStateException(
                    getClass().getSimpleName() + " must be called before accessing SAM implementation"
            );
        }
        return samImpl;
    }

    private static ClassNode requireSamInterface(ClassNode classNode) {
        if (classNode.isInterface() && isSAMType(classNode)) {
            return classNode;
        }

        throw new IllegalArgumentException(
                "Expected: Single Abstract Method (functional) interface\n" +
                        "Found: " + classNode
        );
    }
}
