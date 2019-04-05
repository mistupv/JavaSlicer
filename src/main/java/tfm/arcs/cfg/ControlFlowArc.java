package tfm.arcs.cfg;

import tfm.arcs.Arc;
import tfm.arcs.data.VoidArcData;
import tfm.nodes.Node;

public class ControlFlowArc extends Arc<VoidArcData> {

    public ControlFlowArc(Node from, Node to) {
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

}
