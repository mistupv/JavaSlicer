package tfm.nodes;

import com.github.javaparser.ast.Node;
import edg.graphlib.Arrow;
import org.checkerframework.checker.nullness.qual.NonNull;
import tfm.arcs.Arc;
import tfm.arcs.data.ArcData;
import tfm.arcs.pdg.ControlDependencyArc;
import tfm.utils.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PDGNode<N extends Node> extends GraphNode<N> {

    public PDGNode(int id, String data, N node) {
        super(id, data, node);
    }

    public PDGNode(int id, String representation, @NonNull N node, Collection<? extends Arrow<String, ArcData>> incomingArcs, Collection<? extends Arrow<String, ArcData>> outgoingArcs, Set<String> declaredVariables, Set<String> definedVariables, Set<String> usedVariables) {
        super(id, representation, node, incomingArcs, outgoingArcs, declaredVariables, definedVariables, usedVariables);
    }

    public <N1 extends GraphNode<N>> PDGNode(N1 node) {
        super(node);
    }

    public String toString() {
        List<Integer> dataFrom = new ArrayList<>();
        List<Integer> dataTo = new ArrayList<>();
        List<Integer> controlFrom = new ArrayList<>();
        List<Integer> controlTo = new ArrayList<>();

        getIncomingArrows().forEach(arrow -> {
            Logger.log(arrow);
            Arc arc = (Arc) arrow;
            GraphNode from = (GraphNode) arc.getFrom();

            if (arc.isDataDependencyArrow()) {
                dataFrom.add(from.getId());
            } else if (arc.isControlDependencyArrow()) {
                controlFrom.add(from.getId());
            }

        });

        getOutgoingArrows().forEach(arrow -> {
            Arc arc = (Arc) arrow;
            GraphNode to = (GraphNode) arc.getTo();

            if (arc.isDataDependencyArrow()) {
                dataTo.add(to.getId());
            } else if (arc.isControlDependencyArrow()) {
                controlTo.add(to.getId());
            }

        });

        return String.format("PDGNode{id: %s, data: %s, dataFrom: %s, dataTo: %s, controlFrom: %s, controlTo: %s}",
                getId(),
                getData(),
                dataFrom,
                dataTo,
                controlFrom,
                controlTo
        );
    }

    public List<ControlDependencyArc> getControlDependencies() {
        return getIncomingArrows().stream()
                .filter(arrow -> ((Arc) arrow).isControlDependencyArrow())
                .map(arc -> (ControlDependencyArc) arc)
                .collect(Collectors.toList());
    }

    public int getLevel() {
        return getLevel(this);
    }

    private int getLevel(PDGNode node) {
        List<ControlDependencyArc> dependencies = node.getControlDependencies();

        if (dependencies.isEmpty())
            return 0;

        return 1 + getLevel((PDGNode) dependencies.get(0).getFrom());
    }
}
