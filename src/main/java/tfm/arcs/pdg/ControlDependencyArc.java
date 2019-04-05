package tfm.arcs.pdg;

import tfm.arcs.Arc;
import tfm.arcs.data.ArcData;
import tfm.nodes.Node;

public class ControlDependencyArc extends Arc<ArcData> {

    public ControlDependencyArc(Node from, Node to) {
        super(from, to);
    }

    @Override
    public boolean isControlFlowArrow() {
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
                ((Node) getFrom()).getId(),
                ((Node) getTo()).getId()
        );
    }
}
