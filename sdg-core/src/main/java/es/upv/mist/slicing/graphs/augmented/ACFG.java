package es.upv.mist.slicing.graphs.augmented;

import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.arcs.cfg.ControlFlowArc;
import es.upv.mist.slicing.graphs.cfg.CFG;
import es.upv.mist.slicing.graphs.cfg.CFGBuilder;
import es.upv.mist.slicing.nodes.GraphNode;

/**
 * An <b>augmented</b> version of the {@link CFG}. Its corresponding builder is the
 * {@link ACFGBuilder}, and the main difference is the ability to properly handle
 * unconditional jumps ({@link com.github.javaparser.ast.stmt.SwitchStmt switch},
 * {@link com.github.javaparser.ast.stmt.BreakStmt break}, {@link com.github.javaparser.ast.stmt.ContinueStmt continue},
 * etc.) by using {@link ControlFlowArc.NonExecutable non-executable
 * control flow arcs}. Any dependence graph built on top of this one should use the
 * {@link PPDG} as its program dependence graph; otherwise more instructions will
 * be included than necessary.
 * @see ControlFlowArc.NonExecutable
 */
public class ACFG extends CFG {
    public void addNonExecutableControlFlowArc(GraphNode<?> from, GraphNode<?> to) {
        addControlFlowArc(from, to, new ControlFlowArc.NonExecutable());
    }

    @Override
    protected CFGBuilder newCFGBuilder() {
        return new ACFGBuilder(this);
    }

    /** TODO: the definition and check of pseudo-predicate for the PPDG is better as 2 outgoing, 1 non-executable
     * Discerns whether a node contained in this graph is a pseudo-predicate or not.
     * Pseudo-predicates have one (and only one) outgoing non-executable control flow arc.
     */
    public boolean isPseudoPredicate(GraphNode<?> node) {
        return outgoingEdgesOf(node).stream().filter(Arc::isNonExecutableControlFlowArc).count() == 1;
    }
}
