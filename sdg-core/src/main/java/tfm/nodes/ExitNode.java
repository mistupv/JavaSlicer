package tfm.nodes;

import com.github.javaparser.ast.body.MethodDeclaration;
import tfm.nodes.type.NodeType;

import java.util.LinkedList;

public class ExitNode extends SyntheticNode<MethodDeclaration> {
    public ExitNode(MethodDeclaration astNode) {
        super(NodeType.METHOD_EXIT, "Exit", astNode, new LinkedList<>());
    }

    protected ExitNode(NodeType type, String instruction, MethodDeclaration astNode) {
        super(type, instruction, astNode, new LinkedList<>());
    }
}
