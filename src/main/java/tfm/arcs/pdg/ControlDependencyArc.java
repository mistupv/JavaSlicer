package tfm.arcs.pdg;

import tfm.arcs.Arc;

/**
 * An arc used in the {@link tfm.graphs.PDG} and {@link tfm.graphs.SDG}
 * used to represent control dependence between two nodes. The traditional definition of
 * control dependence is: a node {@code a} is <it>control dependent</it> on node
 * {@code b} if and only if {@code b} alters the number of times {@code a} is executed.
 */
public class ControlDependencyArc extends Arc {
}
