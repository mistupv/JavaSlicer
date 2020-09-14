package es.upv.mist.slicing.graphs;

public interface Buildable<A> {
    /** Complete this object by using the data from the argument.
     * This method should only be called once per instance. */
    void build(A arg);
    /** Whether or not this object has been built. */
    boolean isBuilt();
}
