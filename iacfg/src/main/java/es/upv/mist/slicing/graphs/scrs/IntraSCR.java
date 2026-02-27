package es.upv.mist.slicing.graphs.scrs;

import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.graphs.icfg.ICFG;
import es.upv.mist.slicing.nodes.GraphNode;
import org.jgrapht.Graph;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A strongly connected region (or component) in a {@link ICFG}.
 *
 * @see AbstractSCR
 */
public class IntraSCR extends AbstractSCR<GraphNode<?>, Arc> {
    private static int nextId = 1;
    private final Set<Integer> topologicalNumberSet = new HashSet<>();

    public IntraSCR(Graph<GraphNode<?>, Arc> base, Set<? extends GraphNode<?>> vertexSubset, Set<? extends Arc> edgeSubset) {
        super(base, vertexSubset, edgeSubset);
        this.id = nextId++;
    }

    public void addTopologicalNumber(Integer topologicalNumber) {
        this.topologicalNumberSet.add(topologicalNumber);
    }

    public String getTopologicalNumber() {
        return this.topologicalNumberSet.stream()
                .map(String::valueOf)
                .collect(Collectors.joining("-"));
    }

    public Set<Integer> getTopologicalNumberSet() {
        return this.topologicalNumberSet;
    }
}
