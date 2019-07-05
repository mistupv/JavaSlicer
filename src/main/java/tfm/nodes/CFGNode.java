package tfm.nodes;

import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.Statement;
import edg.graphlib.Arrow;
import tfm.graphs.Graph;

import java.util.stream.Collectors;


public class CFGNode extends Node<Statement> {

    public <N extends Node> CFGNode(N node) {
        super(node);
    }

    public CFGNode(int nextVertexId, String rootNodeData, Statement statement) {
        super(nextVertexId, rootNodeData, statement);
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
