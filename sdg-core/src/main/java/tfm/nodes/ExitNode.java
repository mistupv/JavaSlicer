package tfm.nodes;

import com.github.javaparser.ast.body.MethodDeclaration;
import tfm.nodes.type.NodeType;

public class ExitNode extends SyntheticNode<MethodDeclaration> {
    public ExitNode(MethodDeclaration astNode) {
        super(NodeType.METHOD_EXIT, "Exit", astNode);
    }

    protected ExitNode(NodeType type, String instruction, MethodDeclaration astNode) {
        super(type, instruction, astNode);
    }
}
