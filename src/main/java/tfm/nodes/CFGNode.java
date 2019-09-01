package tfm.nodes;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.Statement;

import java.util.stream.Collectors;


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
                getIncomingArrows().stream().map(arrow -> arrow.getFrom().getName()).collect(Collectors.toList()),
                getOutgoingArrows().stream().map(arrow -> arrow.getTo().getName()).collect(Collectors.toList())
        );
    }
}
