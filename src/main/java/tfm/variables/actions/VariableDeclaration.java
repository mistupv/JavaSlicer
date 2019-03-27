package tfm.variables.actions;

import tfm.nodes.Vertex;

public class VariableDeclaration<T> extends VariableWrite<T> {

    public VariableDeclaration(Vertex node, T value) {
        super(node, value);
    }
}
