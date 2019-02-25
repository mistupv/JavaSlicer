package tfm.graphlib.nodes;

import tfm.graphlib.arcs.data.ArcData;
import tfm.graphlib.arcs.data.VoidArcData;
import tfm.graphlib.graphs.Graph;

public class CFGVertex extends Vertex {

    public CFGVertex(Graph.VertexId id, String data) {
        super(id, data);
    }
}
