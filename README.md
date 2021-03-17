# Java SDG Slicer

A program slicer for Java, based on the system dependence graph (SDG). *Program slicing* is a software analysis technique to extract the subset of statements that are relevant to the value of a variable in a specific statement (the *slicing criterion*). The subset of statements is called a *slice*, and it can be used for debugging, parallelization, clone detection, etc. This repository contains two modules:

* `sdg-core`, a library that obtains slices from Java source code via the SDG, a data structure that represents statements as nodes and their dependencies as arcs.
* `sdg-cli`, a command line client for `sdg-core`, which takes as input a Java program and the slicing criterion, and outputs the corresponding slice.

Warning: all method calls must resolve to a method declaration. If your Java program requires additional libraries, their source code must be available and included in the analysis with the `-i` option. Any method call that cannot be resolved will result in a runtime error.

## Quick start

### Build the project

JavaSDGSlicer manages its dependencies through maven, so you need to have the JDK (&ge;11) and Maven installed, then run 
```
mvn package
```

A fat jar containing all the project's dependencies can be then located at `./sdg-cli/target/sdg-cli-{version}-jar-with-dependencies.jar`.

### Slice a Java program

The slicing criterion can be specified with the flag `-c {file}#{line}:{var}`, where the file, line and variable can be specified. If the variable appears multiple times in the given line, all of them will be selected.

If we wish to slice following program with respect to variable `sum` in line 11, 

```java=
public class Example {
    public static void main(String[] args) {
        int sum = 0;
        int prod = 0;
        int i;
        int n = 10;
        for (i = 0; i < 10; i++) {
            sum += 1;
            prod += n;
        }
        System.out.println(sum);
        System.out.println(prod);
    }
}
```
The program can be saved to `Example.java`, and the slicer run with:

```
java -jar sdg-cli.jar -c Example.java#11:sum -t SDG
```

A more detailed description of the available options can be seen with:

```
java -jar sdg-cli.jar --help
```

#### A note on third party libraries

Our slicer requires the input Java program to be compilable, so all libraries must be provided using the `-i` flag. For the cases where the source code is not available, you may include the required libraries in the Java classpath by using the following call:

```
java -cp sdg-cli.jar:your-libraries.jar es.upv.slicing.cli.Slicer -c Example.java#11:sum -t SDG
```

This approach produces lower quality slices, as the contents of the library calls are unknown.

## Library usage

A good usage example of `sdg-core` to obtain a slice from source code is available at [Slicer.java#slice()](/sdg-cli/src/main/java/tfm/cli/Slicer.java#L204), where the following steps are performed:

1. JavaParser is configured to (a) resolve calls in the JRE and the user-defined libraries, and to (b) ignore comments.
2. The user-defined Java files are parsed to build a list of `CompilationUnit`s.
3. The SDG is created based on that list. The kind of SDG created depends on a flag.
4. A `SlicingCriterion` is created, from the input arguments, and the slice is obtained.
5. The slice is converted to a list of `CompilationUnit` (each representing a file).
6. The contents of each `CompilationUnit` are dumped to their corresponding file.

If the graph is of interest, it can be outputted in `dot` or PDF format via `SDGLog#generateImages()`, as can be seen in [PHPSlice.java#124](/sdg-cli/src/main/java/tfm/cli/PHPSlice.java#L124) (this class presents a frontend for an unreleased web Java slicer).

## Missing Java features

* Object-oriented features: abstract classes, interfaces, class, method and field inheritance, anonymous classes, lambdas.
* Parallel features: threads, shared memory, synchronized methods, etc.
* Exception handling: `finally`, try with resources.
