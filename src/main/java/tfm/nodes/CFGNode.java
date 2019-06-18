package tfm.nodes;

import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.Statement;
import tfm.graphs.Graph;


public class CFGNode extends Node {

    public <N extends Node> CFGNode(N node) {
        super(node);
    }

    public CFGNode(int nextVertexId, String rootNodeData, Statement statement) {
        super(nextVertexId, rootNodeData, statement);
    }
}
