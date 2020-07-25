package tfm.utils;

import tfm.graphs.Graph;
import tfm.nodes.GraphNode;
import tfm.slicing.SlicingCriterion;

/** An exception produced when an node cannot be located. */
public class NodeNotFoundException extends RuntimeException {

    public NodeNotFoundException(SlicingCriterion slicingCriterion) {
        super("GraphNode not found for slicing criterion: " + slicingCriterion);
    }

    public NodeNotFoundException(String message) {
        super(message);
    }

    public NodeNotFoundException(GraphNode<?> graphNode) {
        super("Node not found: " + graphNode.toString());
    }

    public NodeNotFoundException(GraphNode<?> graphNode, Graph graph) {
        super(
                String.format("Node %s not found in graph: %s%s",
                    graphNode.toString(),
                    System.lineSeparator(),
                    graph.toString())
        );
    }
}
