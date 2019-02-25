package tfm.graphlib.arcs;

import tfm.graphlib.arcs.data.ArcData;
import tfm.graphlib.nodes.Vertex;

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
        return String.format("\"%s\" -> \"%s\"", getFrom().getData(), getTo().getData());
    }
}
