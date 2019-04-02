package tfm.variables.actions;

import tfm.nodes.Vertex;

public class VariableWrite extends VariableAction {

    public VariableWrite(Vertex node) {
        super(node);
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
