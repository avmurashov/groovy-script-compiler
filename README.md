# Groovy Compiler

Utility library, designed to simplify Groovy scripts compilations into
Java classes at run-time.


## Basic use case

The basic use case is as simple as the following code sample:

```java
import org.sandbox.groovy.GroovyCompiler;

//...

public Class<?> compileGroovyScript(String groovyScriptText) {
    try (final GroovyCompiler compiler = new GroovyCompiler()) {
        return compiler.takeSourceCode(groovyScriptText).thenCompile();
    }
}
```


## Enhanced use cases

Often times there is a need to customize compilation.

To allow compilation customizations `GroovyCompilation` object is
returned by `takeSourceCode()` method of the `GroovyCompiler`.

`GroovyCompilation` object provides the following key methods:
1. `thenApply(CompilationCustomizer... customizers)`

   This method registers Groovy-standard `CompilationCustomizer`
   implementations to call during the Java class compilation.

   Provided `customizers` are grouped according to the `CompilePhase`
   they are bound to, and later they are called on appropriate
   compilation phase in the order they were specified (hence the method
   name).

   This method also supports chained invocations for convenience.
   The result of chained method calls is the same as specifying
   all the `customizers` in the same order in the single invocation of
   the given method.

   I.e. the following code snippet:

   ```java
   try (final GroovyCompiler compiler = new GroovyCompiler()) {
       return compiler
           .takeSourceCode(groovyScriptText)
           .thenApply(customizer1)
           .thenApply(customizer2)
           .thenApply(customizer3)
           .thenCompile();
   }
   ```

   is equivalent to:

   ```java
   try (final GroovyCompiler compiler = new GroovyCompiler()) {
       return compiler
           .takeSourceCode(groovyScriptText)
           .thenApply(customizer1, customizer2, customizer3)
           .thenCompile();
   }
   ```

2. `thenCompile()`

   This method implements actual Groovy script compilation to Java
   class. During the compilation, all the registered
   `CompilationCustomizer` implementations are called.

   Please note that compilation to java class implies that
   `groovy.transform.CompileStatic` AST transformation is enforced
   after all the explicitly defined customizers.


Enhanced use cases, supported by *Groovy Compiler* library are outlined
below.


### Define Groovy ClassNode

From time to time it is required to prepare
`org.codehaus.groovy.ast.ClassNode` for customizers.

`ClassNodes` class provides the following convenience methods for the
`ClassNode` definition:

* `classNode(Class<?> clazz, Object... genericTypes)`

  Constructs `ClassNode` definition for provided `clazz`.

  If the given `clazz` represents a generic type, then the
  `genericTypes` should contain as many elements, as there are generic
  type parameters.

  The following types are accepted as `genericTypes` elements:
  * `org.codehaus.groovy.ast.GenericsType`
  * `org.codehaus.groovy.ast.ClassNode`
  * `java.lang.Class`

  E.g. the following code will define `ClassNode` for
  `java.util.Map<String, Double>`:

  ```java
  classNode(Map.class, String.class, Double.class);
  ```

* `wildcard(Object... upperBounds)`

  Constructs `GenericsType` for the wildcard with optionally specified
  upper bounds.

  The following types are accepted as `upperBounds` elements:
  * `org.codehaus.groovy.ast.ClassNode`
  * `java.lang.Class`

  E.g. the following code will define `ClassNode` for
  `java.util.Map<String, ? extends Number>`:

  ```java
  classNode(Map.class, String.class, wildcard(Number.class));
  ```

  If no `upperBounds` are specified, `java.lang.Object` is taken as
  upper bound.


### Define imports

Groovy provides standard `CompilationCustomizer` for class and static
class members imports actually, it is named `ImportCustomizer`.

`GroovyCompilation` provides the following convenience methods, based on
`ImportCustomizer`:

* `thenApplyImports(Class<?>... classes)`

  Defines class imports, so that specified `classes` may be referred by
  simple name.

  E.g. the following code allows to refer
  `java.util.function.ToDoubleFunction` by simple name from within the
  compiled Groovy script:

  ```java
  import java.util.function.ToDoubleFunction;

  //...

  try (final GroovyCompiler compiler = new GroovyCompiler()) {
      return compiler
          .takeSourceCode(groovyScriptText) // this script may refer ToDoubleFunction by simple name
          .thenApplyImports(ToDoubleFunction.class)
          .thenCompile();
  }
  ```

* `thenApplyStaticImports(Class<?> clazz, String... staticMembers)`

  Defines static imports for the specified `staticMembers` of the
  specified `clazz`.

  E.g. the following code allows to refer `java.lang.Math.sqrt()`
  method by simple name from within the compiled Groovy script:

  ```java
  try (final GroovyCompiler compiler = new GroovyCompiler()) {
      return compiler
          .takeSourceCode(groovyScriptText) // this script may refer Math.sqrt() by simple name
          .thenApplyStaticImports(Math.class, "sqrt")
          .thenCompile();
  }
  ```

  If no `staticMembers` are specified, static star import will be
  defined. Please use this feature with caution, as imported members
  may unexpectidly clash with Groovy standard methods and with each
  other.

  E.g. the following code allows to refer all the static methods of
  `java.lang.Math` from within the compiled Groovy script:

  ```java
  try (final GroovyCompiler compiler = new GroovyCompiler()) {
      return compiler
          .takeSourceCode(groovyScriptText) // this script may refer Math static members by simple name
          .thenApplyStaticImports(Math.class)
          .thenCompile();
  }
  ```


### Convert to POJO class

By default, any Groovy script is compiled to Java class that extends
`groovy.lang.Script` class and defines certain fields and methods in
support of Groovy scripting environment.

If not used, the Groovy scripting environment routines may be dropped
with the help of `org.sandbox.groovy.customizers.PojoClass` customizer.

For convenience, `GroovyCompilation` also provides
`thenApplyPojoClass()` method.

E.g. the following code converts Groovy `Script` to POJO class by
dropping-off Groovy scripting environment support:

```java
try (final GroovyCompiler compiler = new GroovyCompiler()) {
    return compiler
        .takeSourceCode(groovyScriptText) // this script will be converted to POJO
        .thenApplyPojoClass()
        .thenCompile();
}
```

Please note that only the Groovy scripting environment routines are
dropped.

Groovy compiler will anyways make generated class implementing
the `groovy.lang.GroovyObject` interface, and as such - will define
corresponding fields and methods.

Those fields and methods are defined
with **synthetic** modifier. Many decompilers do not show synthetic
member definitions, don't be confused with this.


### Convert to Single Abstract Method interface

If you need to call Groovy script as some well-defined functional
interface, you may utilize
`org.sandbox.groovy.customizers.SamInterfaceImplementation`.

For convenience, `GroovyCompilation` also provides
`thenApplySamImplementation()` method.

E.g. the following code will compile Groovy script as `applyAsDouble()`
method body of `ToDoubleFunction<Map<String, ?>>`:

```java
import java.util.Map;
import java.util.function.ToDoubleFunction;
import static org.sandbox.groovy.ClassNodes.classNode;
import static org.sandbox.groovy.ClassNodes.wildcard;

//...

private static final SAM_TYPE = classNode(
    ToDoubleFunction.class,
    classNode(
        Map.class,
        String.class, wildcard()
    )
);

//...

public ToDoubleFunction<Map<String, ?>> compileGroovyScript(String groovyScriptText) {

    try (final GroovyCompiler compiler = new GroovyCompiler()) {
        final SamInterfaceImplementation samImpl = new SamInterfaceImplementation(SAM_TYPE);

        final Class<?> clazz = compiler
            .takeSourceCode(groovyScriptText) // this script will be compiled as `applyAsDouble()` method body
            .thenApplyImports(Map.class, ToDoubleFunction.class)
            .thenApply(samImpl)
            .thenCompile();

        final Constructor<?> ctor = clazz.getConstructor();

        @SuppressWarnings("unchecked")
        final ToDoubleFunction<Map<String, ?>> result = (ToDoubleFunction<Map<String, ?>>) ctor.newInstance();

        return result;
    }
}
```

`SamInterfaceImplementation` customizer exposes `getSamImpl()` method
that could be called by subsequently executed customizers to get
reference to `MethodNode`, describing newly defined SAM. See below.


### Define method parameters names

Typically, compiled Groovy script is supposed to implement some Single
Abstract Method interface. And in majority of cases implemented
interface comes from binary dependency without debug information.

To guarantee that script will compile, you need to fix method parameters
names.

This may be done with the help of
`org.sandbox.groovy.customizers.MethodParamNames`.

For convenience, `GroovyCompilation` also provides
`thenApplyParamNames()` method.

Both accept the following parameters:
1. `Supplier<MethodNode> methodNodeSupplier`

   This supplier is called during the customizer execution to retrieve
   definition of a method, whose parameter names are to be defined

2. `String... paramNames`

   Formal parameters names to be defined for given method definition.


E.g. the following code fixes `applyAsDouble()` parameter name, so that
compiled Groovy script may access it as `Map<String, ?> params`:

```java
private static final SAM_TYPE = classNode(
    ToDoubleFunction.class,
    classNode(
        Map.class,
        String.class, wildcard()
    )
);

//...

public ToDoubleFunction<Map<String, ?>> compileGroovyScript(String groovyScriptText) {

    try (final GroovyCompiler compiler = new GroovyCompiler()) {
        final SamInterfaceImplementation samImpl = new SamInterfaceImplementation(SAM_TYPE);

        final Class<?> clazz = compiler
            .takeSourceCode(groovyScriptText) // this script is the body of `applyAsDouble(Map<String, ?> params)` and may refer "params"
            .thenApplyImports(Map.class, ToDoubleFunction.class)
            .thenApply(samImpl)
            .thenApplyParamNames(samImpl::getSamImpl, "params") // take MethodNode from earlier called SamInterfaceImplementation, assign "params" name to the only SAM parameter
            .thenCompile();

        final Constructor<?> ctor = clazz.getConstructor();

        @SuppressWarnings("unchecked")
        final ToDoubleFunction<Map<String, ?>> result = (ToDoubleFunction<Map<String, ?>>) ctor.newInstance();

        return result;
    }
}
```


### Define typed variables from Map parameter

Sometimes it is required to bypass parameters to a script in a generic
way. At the same time, the script is compiled to Java, and as such -
Groovy dynamic typing is forbidden.

To provide all the necessary type information, one may use
`org.sandbox.groovy.customizers.VariablesFromMapParam`.

For convenience, `GroovyCompilation` also provides
`thenApplyVarsFromMap()` method.

Both accept the following parameters:
1. `Supplier<MethodNode> methodNodeSupplier`

   This supplier is called during the customizer execution to retrieve
   definition of a method that accepts parameters in generic way via the
   parameter of type `Map<String, ?>`.

2. `String mapParamName`

   Name of the formal parameter of type, assignable to `Map<String, ?>`.

3. `Map<String, ClassNode> variableDefinitions`

   Typed variable definitions.

   Keys represent script variable names, and hence should conform Java
   identifier requirements. Same keys are expected to be passed in the
   `Map<String, ?>` parameter, referred by `mapParamName`.

   Values represent variable types.

E.g. the following code defines typed variables `double x` and `int y`
for the script that are initialized from `Map<String, ?> params`
parameter:

```java
private static final SAM_TYPE = classNode(
    ToDoubleFunction.class,
    classNode(
        Map.class,
        String.class, wildcard()
    )
);

private static Map<String, ClassNode> PARAMS = ImmutableMap.of(
        "x", classNode(double.class),
        "y", classNode(int.class)
);

//...

public ToDoubleFunction<Map<String, ?>> compileGroovyScript(String groovyScriptText) {

    try (final GroovyCompiler compiler = new GroovyCompiler()) {
        final SamInterfaceImplementation samImpl = new SamInterfaceImplementation(SAM_TYPE);

        final Class<?> clazz = compiler
            .takeSourceCode(groovyScriptText) // this script may refer `double x` and `int y` variables, initialized from `Map<String, ?> params` parameter
            .thenApplyImports(Map.class, ToDoubleFunction.class)
            .thenApply(samImpl)
            .thenApplyParamNames(samImpl::getSamImpl, "params") // assign "params" name to the only SAM parameter
            .thenApplyVarsFromMap(samImpl::getSamImpl, "params", PARAMS) // defines typed variables "x" and "y", initialized from "params" SAM parameter.
            .thenCompile();

        final Constructor<?> ctor = clazz.getConstructor();

        @SuppressWarnings("unchecked")
        final ToDoubleFunction<Map<String, ?>> result = (ToDoubleFunction<Map<String, ?>>) ctor.newInstance();

        return result;
    }
}
```



### Define typed fields from Map parameter

Sometimes it is required to bypass some configuration
to a script in a generic way. At the same time, the script is compiled
to Java, and as such - Groovy dynamic typing is forbidden.

To define typed final fields and constructor that initializes those
fields from parameter of type, assignable to `Map<String, ?>`, one may
use `org.sandbox.groovy.customizers.FinalFieldsFromMapParam`.

For convenience, `GroovyCompilation` also provides
`thenApplyFieldsFromMap()` method.

Both accept the following parameter:
1. `Map<String, ClassNode> fieldDefinitions`

   Final fields definitions.

   Keys represent script field names, and hence should conform Java
   identifier requirements. Same keys are expected to be passed in the
   `Map<String, ?>` parameter of the generated constructor.

E.g. the following code defines typed final fields `Random rnd` and
`double multiplier` for the script that are initialized from the only
constructor parameter of type `Map<String, ?>`:
```java
private static final SAM_TYPE = classNode(
    Supplier.class, Double.class
);

private static Map<String, ClassNode> FIELDS = ImmutableMap.of(
        "rnd", classNode(Random.class),
        "multiplier", classNode(double.class)
);

//...

public Supplier<Double> compileGroovyScript(String groovyScriptText) {

    try (final GroovyCompiler compiler = new GroovyCompiler()) {
        final Class<?> clazz = compiler
            .takeSourceCode(groovyScriptText) // this script may refer `Random rnd` and `double multiplier` final fields, initialized from the only constructor parameter of type `Map<String, ?>`
            .thenApplyImports(Map.class, Supplier.class)
            .thenApply(new SamInterfaceImplementation(SAM_TYPE))
            .thenApplyFieldsFromMap(FIELDS)
            .thenCompile();

        final Constructor<?> ctor = clazz.getConstructor(Map.class);

        final Map<String, Object> fields = ImmutableMap.<String, Object>of(
                "rnd", new Random(),
                "multiplier", 42d
        );

        @SuppressWarnings("unchecked")
        final Supplier<Double> result = (Supplier<Double>) ctor.newInstance(fields);

        return result;
    }
}
```



### Define toString() method

To define `toString()` method, one can use
`org.sandbox.groovy.customizers.ExplicitToString`.

For convenience, `GroovyCompilation` also provides
`thenApplyExplicitToString()` method.

Both will define `toString()` method that returns textual representation
of the given script instance that contains:
* Actual class name
* Identity has code
* List of all the declared non-synthetic fields with their values
* Original source code of the script instance, if available.