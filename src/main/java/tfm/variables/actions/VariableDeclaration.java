package tfm.variables.actions;

import tfm.nodes.Node;

public class VariableDeclaration<N extends Node> extends VariableAction<N> {

    public VariableDeclaration(String variable, N node) {
        super(variable, node);
    }

    @Override
    public boolean isDeclaration() {
        return true;
    }

    @Override
    public boolean isWrite() {
        return false;
    }

    @Override
    public boolean isRead() {
        return false;
    }
}
