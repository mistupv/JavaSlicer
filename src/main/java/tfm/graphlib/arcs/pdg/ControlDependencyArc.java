package tfm.graphlib.arcs.pdg;

import tfm.graphlib.arcs.Arc;
import tfm.graphlib.arcs.data.ArcData;
import tfm.graphlib.nodes.Vertex;

public class ControlDependencyArc extends Arc<ArcData> {

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

    @Override
    public String toString() {
        return String.format("ControlDependencyArc{%s -> %s}",
                ((Vertex) getFrom()).getId(),
                ((Vertex) getTo()).getId()
        );
    }
}
