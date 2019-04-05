package tfm.variables.actions;

import tfm.nodes.Node;

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

    private Node node;

    protected VariableAction(Node node) {
        this.node = node;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public abstract boolean isDeclaration();

    public abstract boolean isWrite();

    public abstract boolean isRead();
}
