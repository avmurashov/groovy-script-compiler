package org.sandbox.groovy;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.GenericsType;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.codehaus.groovy.ast.ClassHelper.OBJECT_TYPE;
import static org.codehaus.groovy.ast.ClassHelper.make;
import static org.codehaus.groovy.ast.tools.GenericsUtils.buildWildcardType;
import static org.codehaus.groovy.ast.tools.GenericsUtils.makeClassSafeWithGenerics;

public class ClassNodes {
    private static final GenericsType[] GENERICS_TYPES = {};

    public static ClassNode classNode(Class<?> clazz, Object... genericTypes) {
        final ClassNode rowClassNode = make(clazz);

        if (genericTypes == null || genericTypes.length == 0) {
            return rowClassNode;
        }

        final GenericsType[] safeGenericTypes = stream(genericTypes)
                .map(ClassNodes::safeGenericsType)
                .collect(toList())
                .toArray(GENERICS_TYPES);

        return makeClassSafeWithGenerics(rowClassNode, safeGenericTypes);
    }

    public static GenericsType wildcard(ClassNode... upperBounds) {
        if (upperBounds == null || upperBounds.length == 0) {
            return buildWildcardType(OBJECT_TYPE);
        } else {
            return buildWildcardType(upperBounds);
        }
    }

    private static ClassNode safeClassNode(Object object) {
        if (object instanceof ClassNode) {
            return (ClassNode) object;
        }

        if (object instanceof Class) {
            return make((Class) object);
        }

        throw new IllegalArgumentException(
                "Expected: ClassNode or Class<?> instance\n" +
                        "Found: " + object
        );
    }

    private static GenericsType safeGenericsType(Object object) {
        if (object instanceof GenericsType) {
            return (GenericsType) object;
        }

        return new GenericsType(safeClassNode(object));
    }
}
