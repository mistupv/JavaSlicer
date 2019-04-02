package tfm.variables.actions;

import com.sun.org.apache.regexp.internal.RE;
import tfm.nodes.Vertex;
import tfm.utils.Scope;

public abstract class VariableAction {

    public enum Actions {
        UNKNOWN,
        READ,
        WRITE;

        public Actions or(Actions action) {
            if (action == WRITE || this == WRITE)
                return WRITE;

            return READ;
        }

        public String toString() {
            return this == UNKNOWN ? "unknown" :
                    (this == READ ? "read" : "write");
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

    public abstract boolean isWrite();

    public abstract boolean isRead();
}
