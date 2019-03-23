package tfm.arcs;

import tfm.arcs.data.ArcData;
import tfm.nodes.Vertex;

public abstract class Arc<D extends ArcData> extends edg.graphlib.Arrow<String, D> {

    @SuppressWarnings("unchecked")
    public Arc(Vertex from, Vertex to) {
        super((edg.graphlib.Vertex<String, D>) from, (edg.graphlib.Vertex<String, D>) to);
    }

    public abstract boolean isControlFlowArrow();

    public abstract boolean isControlDependencyArrow();

    public abstract boolean isDataDependencyArrow();

    @Override
    public String toString() {
        return String.format("Arc{data: %s, %s -> %s}",
                getData(),
                getFrom(),
                getTo()
        );
    }

    public String toGraphvizRepresentation() {
        Vertex from = (Vertex) getFrom();
        Vertex to = (Vertex) getTo();

        return String.format("\"%s: %s\" -> \"%s: %s\"",
                from.getId(), from.getData(),
                to.getId(), to.getData());
    }
}
