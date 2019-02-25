package tfm.graphlib.arcs.pdg;

import tfm.graphlib.arcs.Arc;
import tfm.graphlib.arcs.data.VoidArcData;
import tfm.graphlib.nodes.Vertex;

public class ControlDependencyArc extends Arc<VoidArcData> {

    public ControlDependencyArc(Vertex from, Vertex to) {
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
}
