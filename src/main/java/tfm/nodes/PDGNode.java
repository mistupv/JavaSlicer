package tfm.nodes;

import tfm.arcs.Arc;
import tfm.graphs.Graph;

import java.util.ArrayList;
import java.util.List;

public class PDGNode extends Node {

    public PDGNode(Graph.NodeId id, String data) {
        super(id, data);
    }

    public String toString() {
        List<Integer> dataFrom = new ArrayList<>();
        List<Integer> dataTo = new ArrayList<>();
        List<Integer> controlFrom = new ArrayList<>();
        List<Integer> controlTo = new ArrayList<>();

        getIncomingArrows().forEach(arrow -> {
            Arc arc = (Arc) arrow;
            Node from = (Node) arc.getFrom();

            if (arc.isDataDependencyArrow()) {
                dataFrom.add(from.getId());
            } else if (arc.isControlDependencyArrow()) {
                controlFrom.add(from.getId());
            }

        });

        getOutgoingArrows().forEach(arrow -> {
            Arc arc = (Arc) arrow;
            Node to = (Node) arc.getTo();

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
}
