package tfm.variables.actions;

import tfm.nodes.Vertex;

public class VariableUse extends VariableAction {

    public VariableUse(Vertex node) {
        super(node);
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
