package tfm.variables.actions;

import tfm.nodes.Node;

public class VariableDefinition extends VariableAction {

    public VariableDefinition(Node node) {
        super(node);
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
