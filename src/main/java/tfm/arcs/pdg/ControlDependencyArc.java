package tfm.arcs.pdg;

import tfm.arcs.Arc;
import tfm.arcs.data.ArcData;
import tfm.nodes.Vertex;

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
