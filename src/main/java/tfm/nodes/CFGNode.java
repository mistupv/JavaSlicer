package tfm.nodes;

import tfm.graphs.Graph;

public class CFGNode extends Node {

    public CFGNode(Graph.NodeId id, String data, int fileNumber) {
        super(id, data, fileNumber);
    }
}
