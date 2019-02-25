package tfm.graphlib.arcs.cfg;

import tfm.graphlib.arcs.Arc;
import tfm.graphlib.arcs.data.VoidArcData;
import tfm.graphlib.nodes.Vertex;

public class ControlFlowArc extends Arc<VoidArcData> {

    public ControlFlowArc(Vertex from, Vertex to) {
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
