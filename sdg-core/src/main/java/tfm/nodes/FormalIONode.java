package tfm.nodes;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import tfm.nodes.type.NodeType;

import java.util.Set;

public class FormalIONode extends IONode<MethodDeclaration> {
    protected static final Set<NodeType> VALID_NODE_TYPES = Set.of(NodeType.FORMAL_IN, NodeType.FORMAL_OUT);

    protected FormalIONode(NodeType type, MethodDeclaration astNode, Parameter parameter) {
        super(type, createLabel(type, parameter), astNode, parameter);
        if (!VALID_NODE_TYPES.contains(type))
            throw new IllegalArgumentException("Illegal type for formal-in/out node");
    }

    protected static String createLabel(NodeType type, Parameter param) {
        switch (type) {
            case FORMAL_IN:
                return String.format("%s %s = %2$s_in", param.getTypeAsString(), param.getNameAsString());
            case FORMAL_OUT:
                return String.format("%s %s_out = %2$s", param.getTypeAsString(), param.getNameAsString());
            default:
                throw new IllegalStateException("Invalid NodeType for formal-in/out node: " + type);
        }
    }

    public static FormalIONode createFormalIn(MethodDeclaration methodDeclaration, Parameter parameter) {
        FormalIONode node = new FormalIONode(NodeType.FORMAL_IN, methodDeclaration, parameter);
        node.addDeclaredVariable(parameter.getNameAsExpression());
        node.addDefinedVariable(parameter.getNameAsExpression());
        return node;
    }

    public static FormalIONode createFormalOut(MethodDeclaration methodDeclaration, Parameter parameter) {
        FormalIONode node = new FormalIONode(NodeType.FORMAL_OUT, methodDeclaration, parameter);
        node.addUsedVariable(parameter.getNameAsExpression());
        return node;
    }
}
