package tfm.jacosro.arcs;

import tfm.jacosro.nodes.Node;

public class ControlDependencyArc<E extends Node> extends Arc<E> {

    public ControlDependencyArc(E source, E target) {
        super(source, target);
    }
}
