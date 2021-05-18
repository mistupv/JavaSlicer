package es.upv.mist.slicing.arcs.pdg;

import es.upv.mist.slicing.graphs.exceptionsensitive.ConditionalControlDependencyBuilder;

/**
 * An arc that represents conditional control dependency (CCD): a node {@code a}
 * is conditionally control dependent on a pair of nodes {@code b}, {@code c},
 * if the presence of {@code a} allows the execution of {@code c} when {@code b}
 * is executed and the absence of {@code a} prevents it. </br>
 * The representation is done by connecting {@code a} to {@code c} with a {@link CC2 CC2}
 * arc, and {@code a} to {@code b} with a {@link CC1 CC1} arc.
 * @see ConditionalControlDependencyBuilder
 */
public abstract class ConditionalControlDependencyArc extends ControlDependencyArc {
    /**
     * Currently only used in exception handling, connecting a
     * {@link com.github.javaparser.ast.stmt.CatchClause CatchClause} to statements
     * that execute after its {@link com.github.javaparser.ast.stmt.TryStmt TryStmt}.
     * @see ConditionalControlDependencyArc
     */
    public static class CC1 extends ConditionalControlDependencyArc {
    }

    /**
     * Currently only used in exception handling, connecting a
     * {@link com.github.javaparser.ast.stmt.CatchClause CatchClause} to statements
     * that may throw exceptions inside its corresponding {@link com.github.javaparser.ast.stmt.TryStmt TryStmt}.
     * @see ConditionalControlDependencyArc
     */
    public static class CC2 extends ConditionalControlDependencyArc {
    }
}
