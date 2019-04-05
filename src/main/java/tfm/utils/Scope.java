package tfm.utils;

import tfm.nodes.Node;

public class Scope {

    private Node parent;

    public Scope(Node parent) {
        this.parent = parent;
    }

    public Node getParent() {
        return parent;
    }

    @Override
    public int hashCode() {
        return parent.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof Scope))
            return false;

        Scope other = (Scope) o;

        return this.parent.equals(other.parent);
    }

    @Override
    public String toString() {
        return parent.getName();
    }
}
