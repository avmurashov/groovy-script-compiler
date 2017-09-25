package org.sandbox.groovy.customizers;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.classgen.GeneratorContext;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.control.customizers.CompilationCustomizer;

import java.util.Map;

import static org.codehaus.groovy.ast.tools.GeneralUtils.args;
import static org.codehaus.groovy.ast.tools.GeneralUtils.assignS;
import static org.codehaus.groovy.ast.tools.GeneralUtils.callX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.castX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.constX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.fieldX;
import static org.codehaus.groovy.ast.tools.GeneralUtils.varX;
import static org.codehaus.groovy.control.CompilePhase.CONVERSION;
import static org.objectweb.asm.Opcodes.ACC_FINAL;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.sandbox.groovy.ClassNodes.classNode;

public class FinalFieldsFromMapParam extends CompilationCustomizer {
    private final Map<String, ClassNode> fieldDefinitions;

    public FinalFieldsFromMapParam(Map<String, ClassNode> fieldDefinitions) {
        super(CONVERSION);
        this.fieldDefinitions = fieldDefinitions;
    }

    @Override
    public void call(SourceUnit source, GeneratorContext context, ClassNode classNode) throws CompilationFailedException {
        final ModuleNode module = source.getAST();

        if (!classNode.getName().equals(module.getMainClassName())) {
            return;
        }

        final BlockStatement code = new BlockStatement();

        fieldDefinitions.forEach((fieldName, fieldType) -> {
            classNode.addField(fieldName, ACC_PRIVATE | ACC_FINAL, fieldType, null);

            final Statement fieldInit = assignS(
                    fieldX(classNode, fieldName),
                    castX(fieldType, callX( varX("params"), "get", args(constX(fieldName)) ))
            );

            code.addStatement(fieldInit);
        });

        final Parameter[] parameters = {
                new Parameter(classNode(Map.class, String.class, Object.class), "params")
        };

        classNode.addConstructor(ACC_PUBLIC, parameters, new ClassNode[0], code);
    }
}
