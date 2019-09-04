package tfm.nodes;

import com.github.javaparser.ast.Node;

import java.util.stream.Collectors;

@Deprecated
public class CFGNode<N extends Node> extends GraphNode<N> {

    public <N1 extends GraphNode<N>> CFGNode(N1 node) {
        super(node);
    }

    public CFGNode(int nextVertexId, String rootNodeData, N node) {
        super(nextVertexId, rootNodeData, node);
    }

    @Override
    public String toString() {
        return String.format("CFGNode{id: %s, in: %s, out: %s",
                getId(),
                getIncomingArcs().stream().map(arc -> arc.getFromNode().getId()).collect(Collectors.toList()),
                getOutgoingArcs().stream().map(arc -> arc.getToNode().getId()).collect(Collectors.toList())
        );
    }
}
