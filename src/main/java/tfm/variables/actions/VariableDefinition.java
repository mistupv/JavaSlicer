package tfm.variables.actions;

import tfm.nodes.GraphNode;

public class VariableDefinition<N extends GraphNode> extends VariableAction<N> {

    public VariableDefinition(String variable, N node) {
        super(variable, node);
    }

    @Override
    public boolean isDeclaration() {
        return false;
    }

    @Override
    public boolean isWrite() {
        return true;
    }

    @Override
    public boolean isRead() {
        return false;
    }
}
