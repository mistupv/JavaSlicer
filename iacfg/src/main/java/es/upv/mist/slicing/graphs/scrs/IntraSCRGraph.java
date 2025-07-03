package es.upv.mist.slicing.graphs.scrs;

import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.graphs.icfg.ICFG;
import es.upv.mist.slicing.nodes.GraphNode;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleDirectedGraph;

import java.util.Set;

/**
 * A condensation of the strongly connected regions of a {@link ICFG}.
 * @see AbstractSCRAlgorithm
 * @see IntraSCR IntraSCR: The component members of this graph.
 */
public class IntraSCRGraph extends SimpleDirectedGraph<IntraSCR, DefaultEdge> {
    public IntraSCRGraph(Graph<GraphNode<?>, Arc> graph) {
        super(DefaultEdge.class);
        new AbstractSCRAlgorithm<GraphNode<?>, Arc, IntraSCR>(graph) {
            @Override
            public IntraSCR newSCR(Graph<GraphNode<?>, Arc> graph, Set<GraphNode<?>> nodeSet, Set<Arc> edgeSet) {
                return new IntraSCR(graph, nodeSet, edgeSet);
            }
        }.copySCRs(this);
    }
}
