package tfm.graphs;

public interface Buildable<A> {
    void build(A arg);
    boolean isBuilt();
}
