package tfm.variables;

import tfm.nodes.Vertex;

public class VariableUse<T> extends VariableAction<T> {

    public VariableUse(Vertex node, T value) {
        super(node, value);
    }

    @Override
    public boolean isDeclaration() {
        return false;
    }

    @Override
    public boolean isUse() {
        return true;
    }
}
