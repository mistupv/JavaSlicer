package tfm.variables.actions;

import tfm.nodes.Vertex;

public class VariableRead<T> extends VariableAction<T> {

    public VariableRead(Vertex node, T value) {
        super(node, value);
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
