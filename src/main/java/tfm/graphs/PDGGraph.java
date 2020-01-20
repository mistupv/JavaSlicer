package tfm.graphs;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.jgrapht.io.DOTExporter;
import tfm.arcs.Arc;
import tfm.arcs.pdg.ControlDependencyArc;
import tfm.arcs.pdg.DataDependencyArc;
import tfm.nodes.GraphNode;
import tfm.slicing.SlicingCriterion;
import tfm.utils.ASTUtils;
import tfm.utils.Logger;
import tfm.utils.NodeNotFoundException;
import tfm.visitors.pdg.PDGBuilder;

import java.util.*;

public class PDGGraph extends GraphWithRootNode implements Sliceable<PDGGraph> {

    private CFGGraph cfgGraph;

    public PDGGraph() {
        super();
    }

    @Override
    protected GraphNode<?> buildRootNode() {
        return new GraphNode<>(getNextVertexId(), "ENTER", new MethodDeclaration());
    }

    public PDGGraph(CFGGraph cfgGraph) {
        super();
        this.cfgGraph = cfgGraph;
    }

    public void addControlDependencyArc(GraphNode<?> from, GraphNode<?> to) {
        this.addEdge(from, to, new ControlDependencyArc());
    }

    public void addDataDependencyArc(GraphNode<?> from, GraphNode<?> to, String variable) {
        this.addEdge(from, to, new DataDependencyArc(variable));
    }

    public void setCfgGraph(CFGGraph cfgGraph) {
        this.cfgGraph = cfgGraph;
    }

    @Override
    public PDGGraph slice(SlicingCriterion slicingCriterion) {
        Optional<GraphNode<?>> optionalGraphNode = slicingCriterion.findNode(this);

        if (!optionalGraphNode.isPresent()) {
            throw new NodeNotFoundException(slicingCriterion);
        }

        GraphNode node = optionalGraphNode.get();

        // Simply get slice nodes from GraphNode
        Set<Integer> sliceNodes = getSliceNodes(new HashSet<>(), node);

        PDGGraph sliceGraph = new PDGGraph();

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

    public CFGGraph getCfgGraph() {
        return cfgGraph;
    }
}
