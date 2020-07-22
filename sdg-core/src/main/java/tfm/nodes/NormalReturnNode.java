package tfm.nodes;

import com.github.javaparser.ast.expr.MethodCallExpr;
import tfm.nodes.type.NodeType;

public class NormalReturnNode extends ReturnNode {
    public NormalReturnNode(MethodCallExpr astNode) {
        super(NodeType.METHOD_CALL_NORMAL_RETURN, "normal return", astNode);
    }
}
