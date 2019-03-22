package tfm.graphlib.utils;

import tfm.graphlib.nodes.Vertex;

public class Scope {

    private Vertex parent;

    public Scope(Vertex parent) {
        this.parent = parent;
    }

    public Vertex getParent() {
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
