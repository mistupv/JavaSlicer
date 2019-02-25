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
        List<Arc> dataFrom = new ArrayList<>();
        List<Arc> dataTo = new ArrayList<>();
        List<Arc> controlFrom = new ArrayList<>();
        List<Arc> controlTo = new ArrayList<>();

        getIncomingArrows().forEach(arrow -> {
            Arc arc = (Arc) arrow;

            if (arc.isDataDependencyArrow()) {
                dataFrom.add(arc);
            } else if (arc.isControlDependencyArrow()) {
                controlFrom.add(arc);
            }

        });

        getOutgoingArrows().forEach(arrow -> {
            Arc arc = (Arc) arrow;

            if (arc.isDataDependencyArrow()) {
                dataTo.add(arc);
            } else if (arc.isControlDependencyArrow()) {
                controlTo.add(arc);
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
