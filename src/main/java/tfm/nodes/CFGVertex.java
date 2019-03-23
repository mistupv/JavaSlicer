package tfm.nodes;

import tfm.arcs.data.VoidArcData;
import tfm.graphs.Graph;

public class CFGVertex extends Vertex {

    public CFGVertex(Graph.VertexId id, String data) {
        super(id, data);
    }
}
