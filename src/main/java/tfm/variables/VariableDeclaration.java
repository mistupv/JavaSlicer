package tfm.variables;

import tfm.nodes.Vertex;

public class VariableDeclaration<T> extends VariableAction<T> {

    public VariableDeclaration(Vertex node, T value) {
        super(node, value);
    }

    @Override
    public boolean isDeclaration() {
        return true;
    }

    @Override
    public boolean isUse() {
        return false;
    }
}
