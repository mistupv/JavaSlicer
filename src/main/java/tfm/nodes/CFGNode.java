package tfm.nodes;

import com.github.javaparser.ast.stmt.Statement;
import tfm.graphs.Graph;


public class CFGNode extends Node {

    public CFGNode(Graph.NodeId id, String data, Statement statement) {
        super(id, data, statement);
    }
}
