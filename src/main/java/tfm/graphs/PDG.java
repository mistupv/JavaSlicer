package tfm.graphs;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import tfm.arcs.Arc;
import tfm.arcs.pdg.ControlDependencyArc;
import tfm.arcs.pdg.DataDependencyArc;
import tfm.nodes.GraphNode;
import tfm.slicing.SlicingCriterion;
import tfm.utils.ASTUtils;
import tfm.utils.Logger;
import tfm.utils.NodeNotFoundException;
import tfm.visitors.pdg.PDGBuilder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class PDG extends GraphWithRootNode<MethodDeclaration> implements Sliceable<PDG> {

    private CFG cfg;

    public PDG() {
        super();
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

    public void setCfg(CFG cfg) {
        this.cfg = cfg;
    }

    @Override
    public PDG slice(SlicingCriterion slicingCriterion) {
        Optional<GraphNode<?>> optionalGraphNode = slicingCriterion.findNode(this);

        if (!optionalGraphNode.isPresent()) {
            throw new NodeNotFoundException(slicingCriterion);
        }

        GraphNode node = optionalGraphNode.get();

        // Simply get slice nodes from GraphNode
        Set<Integer> sliceNodes = getSliceNodes(new HashSet<>(), node);

        PDG sliceGraph = new PDG();

        Node astCopy = ASTUtils.cloneAST(node.getAstNode());

        astCopy.accept(new PDGBuilder(sliceGraph), null);

        for (GraphNode<?> sliceNode : sliceGraph.vertexSet()) {
            if (!sliceNodes.contains(sliceNode.getId())) {
                Logger.log("Removing node " + sliceNode.getId());
                sliceNode.getAstNode().removeForced();
                sliceGraph.removeVertex(sliceNode);
            }
        }

        return sliceGraph;
    }

    private Set<Integer> getSliceNodes(Set<Integer> visited, GraphNode<?> root) {
        visited.add(root.getId());

        for (Arc arc : incomingEdgesOf(root)) {
            GraphNode<?> from = this.getEdgeSource(arc);

            if (visited.contains(from.getId())) {
                continue;
            }

            getSliceNodes(visited, from);
        }

        return visited;
    }

    public CFG getCfg() {
        return cfg;
    }
}
