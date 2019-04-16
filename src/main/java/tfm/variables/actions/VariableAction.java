package tfm.variables.actions;

import tfm.nodes.Node;

public abstract class VariableAction<N extends Node> {

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
    private N node;
    private String variable;

    protected VariableAction(String variable, N node) {
        this.variable = variable;
        this.node = node;
    }

    public String getVariable() {
        return variable;
    }

    public N getNode() {
        return node;
    }

    public abstract boolean isDeclaration();

    public abstract boolean isWrite();

    public abstract boolean isRead();
}
