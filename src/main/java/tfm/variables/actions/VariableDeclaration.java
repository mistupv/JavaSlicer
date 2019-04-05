package tfm.variables.actions;

import tfm.nodes.Node;

public class VariableDeclaration extends VariableAction {

    public VariableDeclaration(Node node) {
        super(node);
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
