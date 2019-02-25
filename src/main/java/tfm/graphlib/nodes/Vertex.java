package tfm.graphlib.nodes;

import tfm.graphlib.arcs.data.ArcData;
import tfm.graphlib.graphs.Graph;

import java.util.stream.Collectors;

public class Vertex extends edg.graphlib.Vertex<String, ArcData> {

    public Vertex(Graph.VertexId id, String instruction) {
        super(id.toString(), instruction);
    }

    public int getId() {
        return Integer.parseInt(getName());
    }

    public String toString() {
        return String.format("Vertex{id: %s, data: '%s', in: %s, out: %s}",
                getName(),
                getData(),
                getIncomingArrows().stream().map(arrow -> arrow.getFrom().getName()).collect(Collectors.toList()),
                getOutgoingArrows().stream().map(arc -> arc.getTo().getName()).collect(Collectors.toList()));
    }
}
