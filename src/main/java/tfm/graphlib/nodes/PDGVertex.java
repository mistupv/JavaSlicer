package tfm.graphlib.nodes;

import tfm.graphlib.arcs.Arc;
import tfm.graphlib.arcs.data.ArcData;
import tfm.graphlib.graphs.Graph;

import java.util.ArrayList;
import java.util.List;

public class PDGVertex extends Vertex {

    public PDGVertex(Graph.VertexId id, String data) {
        super(id, data);
    }

    public String toString() {
        List<Integer> dataFrom = new ArrayList<>();
        List<Integer> dataTo = new ArrayList<>();
        List<Integer> controlFrom = new ArrayList<>();
        List<Integer> controlTo = new ArrayList<>();

        getIncomingArrows().forEach(arrow -> {
            Arc arc = (Arc) arrow;
            Vertex from = (Vertex) arc.getFrom();

            if (arc.isDataDependencyArrow()) {
                dataFrom.add(from.getId());
            } else if (arc.isControlDependencyArrow()) {
                controlFrom.add(from.getId());
            }

        });

        getOutgoingArrows().forEach(arrow -> {
            Arc arc = (Arc) arrow;
            Vertex to = (Vertex) arc.getTo();

            if (arc.isDataDependencyArrow()) {
                dataTo.add(to.getId());
            } else if (arc.isControlDependencyArrow()) {
                controlTo.add(to.getId());
            }

        });

        return String.format("PDGVertex{id: %s, data: %s, dataFrom: %s, dataTo: %s, controlFrom: %s, controlTo: %s}",
                getId(),
                getData(),
                dataFrom,
                dataTo,
                controlFrom,
                controlTo
        );
    }
}
