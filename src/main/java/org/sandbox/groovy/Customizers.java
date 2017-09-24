package org.sandbox.groovy;

import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.sandbox.groovy.customizers.PojoClass;

import static java.util.Arrays.stream;

public class Customizers {
    public static ImportCustomizer imports(Class<?>... classes) {
        final ImportCustomizer importCustomizer = new ImportCustomizer();
        stream(classes).map(Class::getCanonicalName).forEach(importCustomizer::addImports);
        return importCustomizer;
    }

    public static ImportCustomizer staticImports(Class<?> clazz, String... members) {
        final String className = clazz.getCanonicalName();
        final ImportCustomizer importCustomizer = new ImportCustomizer();
        stream(members).forEach(member -> importCustomizer.addStaticImport(className, member));
        return importCustomizer;
    }

    public static PojoClass pojoClass() {
        return new PojoClass();
    }
}
