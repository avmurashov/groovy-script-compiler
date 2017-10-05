package org.sandbox.groovy;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.GenericsType;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.codehaus.groovy.ast.ClassHelper.OBJECT_TYPE;
import static org.codehaus.groovy.ast.ClassHelper.make;
import static org.codehaus.groovy.ast.tools.GenericsUtils.buildWildcardType;
import static org.codehaus.groovy.ast.tools.GenericsUtils.makeClassSafeWithGenerics;

/**
 * Utilities for the {@link ClassNode} objects construction.
 *
 * @see #classNode(Class, Object...) classNode
 * @see #wildcard(Object...)
 */
public class ClassNodes {
    private static final ClassNode[] CLASS_NODES = {};
    private static final GenericsType[] GENERICS_TYPES = {};

    /**
     * Constructs {@code ClassNode} that represents the given Java class.
     *
     * @param clazz
     * Target class to construct {@code ClassNode} for.
     * @param genericTypes
     * Generic type arguments. Should be provided if the target class is a generic class. Number and order of generic
     * type arguments should match generic type declaration. Supported:
     * <ul>
     *     <li> {@link Class} </li>
     *     <li> {@link ClassNode} </li>
     *     <li> {@link GenericsType} </li>
     * </ul>
     *
     * @return {@code ClassNode} that represents the given Java class.
     */
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

    /**
     * Constructs {@code GenericsType} that represents a wildcard.
     *
     * @param upperBounds
     * Upper bounds for wildcard, if missed, the {@link Object} is taken as upper bound. Supported:
     * <ul>
     *     <li> {@link Class} </li>
     *     <li> {@link ClassNode} </li>
     * </ul>
     *
     * @return {@code GenericsType} that represents a wildcard.
     */
    public static GenericsType wildcard(Object... upperBounds) {
        if (upperBounds == null || upperBounds.length == 0) {
            return buildWildcardType(OBJECT_TYPE);
        }

        final ClassNode[] safeUpperBounds = stream(upperBounds)
                .map(ClassNodes::safeClassNode)
                .collect(toList())
                .toArray(CLASS_NODES);

        return buildWildcardType(safeUpperBounds);
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
