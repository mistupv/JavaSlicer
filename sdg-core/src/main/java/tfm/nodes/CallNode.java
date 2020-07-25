package tfm.nodes;

import com.github.javaparser.ast.expr.MethodCallExpr;
import tfm.nodes.type.NodeType;

import java.util.LinkedList;

public class CallNode extends SyntheticNode<MethodCallExpr> {
    public CallNode(MethodCallExpr astNode) {
        super(NodeType.METHOD_CALL, "CALL " + astNode.toString(), astNode, new LinkedList<>());
    }
}
