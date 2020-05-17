package tfm.graphs.pdg;

import com.github.javaparser.ast.body.MethodDeclaration;
import tfm.arcs.Arc;
import tfm.arcs.pdg.ControlDependencyArc;
import tfm.arcs.pdg.DataDependencyArc;
import tfm.graphs.GraphWithRootNode;
import tfm.graphs.Sliceable;
import tfm.graphs.cfg.CFG;
import tfm.nodes.GraphNode;
import tfm.slicing.Slice;
import tfm.slicing.SlicingCriterion;
import tfm.utils.NodeNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The <b>Program Dependence Graph</b> represents the statements of a method in
 * a graph, connecting statements according to their {@link ControlDependencyArc control}
 * and {@link DataDependencyArc data} relationships. You can build one manually or use
 * the {@link PDGBuilder PDGBuilder}.
 * The variations of the PDG are represented as child types.
 */
public class PDG extends GraphWithRootNode<MethodDeclaration> implements Sliceable {
    private boolean built = false;
    private CFG cfg;

    public PDG() {
        this(new CFG());
    }

    public PDG(CFG cfg) {
        super();
        this.cfg = cfg;
    }

    public void addControlDependencyArc(GraphNode<?> from, GraphNode<?> to) {
        this.addEdge(from, to, new ControlDependencyArc());
    }

    public void addDataDependencyArc(GraphNode<?> from, GraphNode<?> to, String variable) {
        this.addEdge(from, to, new DataDependencyArc(variable));
    }

    @Override
    public Slice slice(SlicingCriterion slicingCriterion) {
        Optional<GraphNode<?>> node = slicingCriterion.findNode(this);
        if (!node.isPresent())
            throw new NodeNotFoundException(slicingCriterion);
        Slice slice = new Slice();
        getSliceNodes(slice, node.get());
        return slice;
    }

    protected void getSliceNodes(Slice slice, GraphNode<?> node) {
        slice.add(node);

        for (Arc arc : incomingEdgesOf(node)) {
            GraphNode<?> from = getEdgeSource(arc);
            if (slice.contains(from))
                continue;
            getSliceNodes(slice, from);
        }
    }

    public CFG getCfg() {
        return cfg;
    }

    @Override
    public void build(MethodDeclaration method) {
        new PDGBuilder(this).createFrom(method);
        built = true;
    }

    @Override
    public boolean isBuilt() {
        return built;
    }

    public List<GraphNode<?>> findDeclarationsOfVariable(String variable) {
        return vertexSet().stream()
                .filter(node -> node.getDeclaredVariables().contains(variable))
                .collect(Collectors.toList());
    }
}
