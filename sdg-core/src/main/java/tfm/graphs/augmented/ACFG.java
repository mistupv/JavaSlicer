package tfm.graphs.augmented;

import tfm.arcs.Arc;
import tfm.arcs.cfg.ControlFlowArc;
import tfm.graphs.cfg.CFG;
import tfm.graphs.cfg.CFGBuilder;
import tfm.nodes.GraphNode;

/**
 * An <b>augmented</b> version of the {@link CFG}. Its corresponding builder is the
 * {@link ACFGBuilder}, and the main difference is the ability to properly handle
 * unconditional jumps ({@link com.github.javaparser.ast.stmt.SwitchStmt switch},
 * {@link com.github.javaparser.ast.stmt.BreakStmt break}, {@link com.github.javaparser.ast.stmt.ContinueStmt continue},
 * etc.) by using {@link tfm.arcs.cfg.ControlFlowArc.NonExecutable non-executable
 * control flow arcs}. Any dependence graph built on top of this one should use the
 * {@link PPDG} as its program dependence graph; otherwise more instructions will
 * be included than necessary.
 * @see tfm.arcs.cfg.ControlFlowArc.NonExecutable
 */
public class ACFG extends CFG {
    public void addNonExecutableControlFlowEdge(GraphNode<?> from, GraphNode<?> to) {
        addControlFlowEdge(from, to, new ControlFlowArc.NonExecutable());
    }

    @Override
    protected CFGBuilder newCFGBuilder() {
        return new ACFGBuilder(this);
    }

    /**
     * Discerns whether a node contained in this graph is a pseudo-predicate or not.
     * Pseudo-predicates have one (and only one) outgoing non-executable control flow arc.
     */
    public boolean isPseudoPredicate(GraphNode<?> node) {
        return outgoingEdgesOf(node).stream().filter(Arc::isNonExecutableControlFlowArc).count() == 1;
    }
}
