package tfm.variables.actions;

import tfm.nodes.Vertex;

public class VariableDefinition extends VariableAction {

    public VariableDefinition(Vertex node) {
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
