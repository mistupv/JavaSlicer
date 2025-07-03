package es.upv.mist.slicing.graphs.scrs;

import es.upv.mist.slicing.graphs.CallGraph;
import org.jgrapht.Graph;

import java.util.Set;

/**
 * A strongly connected region (or component) on a {@link CallGraph}.
 * @see AbstractSCR
 */
public class CallSCR extends AbstractSCR<CallGraph.Vertex, CallGraph.Edge<?>> {
    private static int nextId = 1;

    public CallSCR(Graph<CallGraph.Vertex, CallGraph.Edge<?>> base, Set<? extends CallGraph.Vertex> vertexSubset, Set<? extends CallGraph.Edge<?>> edgeSubset) {
        super(base, vertexSubset, edgeSubset);
        this.id = nextId++;
    }
}
