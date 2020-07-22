package tfm.nodes;

import com.github.javaparser.ast.Node;
import tfm.nodes.type.NodeType;

import java.util.List;

public abstract class SyntheticNode<T extends Node> extends GraphNode<T> {
    protected SyntheticNode(NodeType type, String instruction, T astNode) {
        super(type, instruction, astNode);
    }

    protected SyntheticNode(NodeType type, String instruction, T astNode, List<VariableAction> variableActions) {
        super(type, instruction, astNode, variableActions);
    }
}
