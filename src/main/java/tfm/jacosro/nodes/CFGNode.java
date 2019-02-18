package tfm.jacosro.nodes;

import org.jetbrains.annotations.NotNull;
import tfm.jacosro.arcs.Arc;
import tfm.jacosro.arcs.ControlFlowArc;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CFGNode<E> extends Node<E> {

    public CFGNode(int id, E code) {
        super(id, code);
    }

    @SafeVarargs
    public final void controlFlowArcFrom(@NotNull CFGNode<E>... nodes) {
        addInArcs(
            Stream.of(nodes)
                .map(node -> new ControlFlowArc<>(node, this))
                .collect(Collectors.toList())
        );
    }

    @SafeVarargs
    public final void controlFlowArcTo(@NotNull CFGNode<E>... nodes) {
        addOutArcs(
            Stream.of(nodes)
                .map(node -> new ControlFlowArc<>(this, node))
                .collect(Collectors.toList())
        );
    }

    public List<Node> getControlFlowDependenceNodes() {
        return inArcs.stream()
                .map(Arc::getSource)
                .sorted()
                .collect(Collectors.toList());
    }

    public List<Node> getControlFlowDependentNodes() {
        return outArcs.stream()
                .map(Arc::getTarget)
                .sorted()
                .collect(Collectors.toList());
    }
}
