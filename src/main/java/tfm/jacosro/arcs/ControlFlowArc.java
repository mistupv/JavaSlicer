package tfm.jacosro.arcs;

import tfm.jacosro.nodes.Node;

public class ControlFlowArc<E extends Node> extends Arc<E> {

    public ControlFlowArc(E source, E target) {
        super(source, target);
    }
}
