package es.upv.mist.slicing.graphs.scrs;

import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.graphs.icfg.ICFG;
import es.upv.mist.slicing.nodes.GraphNode;
import org.jgrapht.Graph;

import java.util.Set;

/**
 * A strongly connected region (or component) in a {@link ICFG}.
 * @see AbstractSCR
 */
public class IntraSCR extends AbstractSCR<GraphNode<?>, Arc> {
    private static int nextId = 1;

    public IntraSCR(Graph<GraphNode<?>, Arc> base, Set<? extends GraphNode<?>> vertexSubset, Set<? extends Arc> edgeSubset) {
        super(base, vertexSubset, edgeSubset);
        this.id = nextId++;
    }
}
