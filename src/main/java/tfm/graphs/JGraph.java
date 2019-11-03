package tfm.graphs;

import org.jgrapht.graph.DefaultDirectedGraph;
import tfm.arcs.Arc;
import tfm.nodes.JNode;

public class JGraph extends DefaultDirectedGraph<JNode<?>, Arc<?>> {

    public JGraph() {
        super(null, null, false);
    }
}
