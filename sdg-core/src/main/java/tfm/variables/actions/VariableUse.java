package tfm.variables.actions;

import tfm.nodes.GraphNode;

public class VariableUse<N extends GraphNode> extends VariableAction<N> {

    public VariableUse(String variable, N node) {
        super(variable, node);
    }

    @Override
    public boolean isDeclaration() {
        return false;
    }

    @Override
    public boolean isWrite() {
        return false;
    }

    @Override
    public boolean isRead() {
        return true;
    }
}
