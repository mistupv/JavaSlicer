package tfm.nodes;

import com.github.javaparser.ast.expr.MethodCallExpr;
import tfm.nodes.type.NodeType;

public abstract class ReturnNode extends SyntheticNode<MethodCallExpr> {
    protected ReturnNode(NodeType type, String instruction, MethodCallExpr astNode) {
        super(type, instruction, astNode);
    }
}
