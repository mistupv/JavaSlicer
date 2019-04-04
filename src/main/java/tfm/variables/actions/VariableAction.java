package tfm.variables.actions;

import com.sun.org.apache.regexp.internal.RE;
import tfm.nodes.Vertex;
import tfm.utils.Scope;

public abstract class VariableAction {

    public enum Actions {
        DECLARE,
        WRITE,
        READ;

        public Actions or(Actions action) {
            if (action == DECLARE || this == DECLARE)
                return DECLARE;

            if (action == WRITE || this == WRITE)
                return WRITE;

            return READ;
        }

        public String toString() {
            return this == DECLARE ? "declare" :
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
