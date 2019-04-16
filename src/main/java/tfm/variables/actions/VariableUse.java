package tfm.variables.actions;

import tfm.nodes.Node;

public class VariableUse<N extends Node> extends VariableAction<N> {

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
