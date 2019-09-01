package tfm.arcs.cfg;

import tfm.arcs.Arc;
import tfm.arcs.data.VoidArcData;
import tfm.nodes.GraphNode;

public class ControlFlowArc extends Arc<VoidArcData> {

    public ControlFlowArc(GraphNode from, GraphNode to) {
        super(from, to);
    }

    @Override
    public boolean isControlFlowArrow() {
        return true;
    }

    @Override
    public boolean isControlDependencyArrow() {
        return false;
    }

    @Override
    public boolean isDataDependencyArrow() {
        return false;
    }

    @Override
    public String toString() {
        return String.format("ControlFlowArc{%s -> %s}",
                getFromNode().getId(),
                getToNode().getId()
        );
    }

}
