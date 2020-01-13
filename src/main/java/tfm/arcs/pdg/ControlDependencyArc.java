package tfm.arcs.pdg;

import tfm.arcs.Arc;
import tfm.arcs.data.ArcData;
import tfm.nodes.GraphNode;

/**
 * An arc used in the {@link tfm.graphs.PDG} and {@link tfm.graphs.SDG}
 * used to represent control dependence between two nodes. The traditional definition of
 * control dependence is: a node {@code a} is <it>control dependent</it> on node
 * {@code b} if and only if {@code b} alters the number of times {@code a} is executed.
 */
public class ControlDependencyArc extends Arc<ArcData> {

    public ControlDependencyArc(GraphNode<?> from, GraphNode<?> to) {
        super(from, to);
    }

    @Override
    public boolean isControlFlowArrow() {
        return false;
    }

    @Override
    public boolean isExecutableControlFlowArrow() {
        return false;
    }

    @Override
    public boolean isControlDependencyArrow() {
        return true;
    }

    @Override
    public boolean isDataDependencyArrow() {
        return false;
    }

    @Override
    public String toString() {
        return String.format("ControlDependencyArc{%s -> %s}",
                ((GraphNode<?>) getFrom()).getId(),
                ((GraphNode<?>) getTo()).getId()
        );
    }
}
