package tfm.graphs;

import edg.graphlib.Arrow;
import tfm.arcs.cfg.ControlFlowArc;
import tfm.nodes.CFGNode;

public abstract class CFGGraph extends Graph<CFGNode> {

    public CFGGraph() {
        super();
        setRootVertex(new CFGNode(NodeId.getVertexId(), getRootNodeData()));
    }

    @Override
    public CFGNode addNode(String instruction) {
        CFGNode vertex = new CFGNode(NodeId.getVertexId(), instruction);
        this.addVertex(vertex);

        return vertex;
    }

    protected abstract String getRootNodeData();

    @SuppressWarnings("unchecked")
    public void addControlFlowEdge(CFGNode from, CFGNode to) {
        super.addEdge((Arrow) new ControlFlowArc(from, to));
    }
}
