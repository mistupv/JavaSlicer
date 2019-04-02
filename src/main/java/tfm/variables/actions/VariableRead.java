package tfm.variables.actions;

import tfm.nodes.Vertex;

public class VariableRead extends VariableAction {

    public VariableRead(Vertex node) {
        super(node);
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
