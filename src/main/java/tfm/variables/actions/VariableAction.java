package tfm.variables.actions;

import tfm.nodes.Vertex;

public abstract class VariableAction {

    public enum Actions {
        DECLARATION,
        DEFINITION,
        USE;

        public Actions or(Actions action) {
            if (action == DECLARATION || this == DECLARATION)
                return DECLARATION;

            if (action == DEFINITION || this == DEFINITION)
                return DEFINITION;

            return USE;
        }

        public String toString() {
            return this == DECLARATION ? "declaration" :
                    (this == USE ? "use" : "definition");
        }
    }

    private Vertex node;

    protected VariableAction(Vertex node) {
        this.node = node;
    }

    public Vertex getNode() {
        return node;
    }

    public void setNode(Vertex node) {
        this.node = node;
    }

    public abstract boolean isDeclaration();

    public abstract boolean isWrite();

    public abstract boolean isRead();
}
