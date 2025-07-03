package es.upv.mist.slicing.graphs.scrs;

import es.upv.mist.slicing.graphs.CallGraph;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.util.Set;

/**
 * A condensation of the strongly connected regions of a {@link CallGraph}.
 * @see AbstractSCRAlgorithm
 * @see CallSCR CallSCR: The component members of this graph.
 */
public class CallSCRGraph extends SimpleDirectedGraph<CallSCR, DefaultEdge> {
    public CallSCRGraph(Graph<CallGraph.Vertex, CallGraph.Edge<?>> graph) {
        super(DefaultEdge.class);
        new AbstractSCRAlgorithm<CallGraph.Vertex, CallGraph.Edge<?>, CallSCR>(graph) {
            @Override
            public CallSCR newSCR(Graph<CallGraph.Vertex, CallGraph.Edge<?>> graph, Set<CallGraph.Vertex> nodeSet, Set<CallGraph.Edge<?>> edgeSet) {
                return new CallSCR(graph, nodeSet, edgeSet);
            }
        }.copySCRs(this);
    }
}
