package es.upv.mist.slicing.utils;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

/** A static class whose only purpose is storing a type solver for conversions
 *  of ResolvedTypeDeclaration objects into ResolvedType ones. */
public class StaticTypeSolver {
    protected static final CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();

    /** Whether we've added the JRE type solver or not. */
    protected static boolean typeSolverHasJRE = false;

    static {
        StaticJavaParser.getConfiguration().setSymbolResolver(new JavaSymbolSolver(combinedTypeSolver));
    }

    /** Append a type solver (typically a {@link com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver JavaParserTypeSolver}. */
    public static void addTypeSolver(TypeSolver typeSolver) {
        combinedTypeSolver.add(typeSolver);
    }

    /** Append a {@link ReflectionTypeSolver} to the type solver, JRE only.
     *  This operation can only be performed once, subsequent invocations will
     *  be discarded. */
    public static void addTypeSolverJRE() {
        addTypeSolverJRE(true);
    }

    /** Append a {@link ReflectionTypeSolver} to the type solver.
     *  This operation can only be performed once, subsequent invocations will
     *  be discarded.  */
    public static void addTypeSolverJRE(boolean jreOnly) {
        if (!typeSolverHasJRE) {
            combinedTypeSolver.add(new ReflectionTypeSolver(true));
            typeSolverHasJRE = true;
        }
    }

    /** Obtain the type solver. This resulting object should not be manually modified. */
    protected static TypeSolver getTypeSolver() {
        return combinedTypeSolver;
    }
}
