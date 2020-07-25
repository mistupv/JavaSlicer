package tfm.nodes;

import com.github.javaparser.ast.body.MethodDeclaration;
import tfm.nodes.type.NodeType;

public class NormalExitNode extends ExitNode {
    public NormalExitNode(MethodDeclaration astNode) {
        super(NodeType.METHOD_NORMAL_EXIT, "normal exit", astNode);
    }
}
