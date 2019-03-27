package tfm.variables.actions;

import tfm.nodes.Vertex;

public class VariableWrite<T> extends VariableAction<T> {

    public VariableWrite(Vertex node, T value) {
        super(node, value);
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
