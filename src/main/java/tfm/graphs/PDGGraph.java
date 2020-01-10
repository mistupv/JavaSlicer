package tfm.graphs;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import edg.graphlib.Arrow;
import org.jetbrains.annotations.NotNull;
import tfm.arcs.Arc;
import tfm.arcs.data.ArcData;
import tfm.arcs.pdg.ControlDependencyArc;
import tfm.arcs.pdg.DataDependencyArc;
import tfm.nodes.GraphNode;
import tfm.slicing.SlicingCriterion;
import tfm.utils.ASTUtils;
import tfm.utils.Logger;
import tfm.utils.NodeNotFoundException;
import tfm.visitors.pdg.PDGBuilder;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The <b>Program Dependence Graph</b> represents the statements of a method in
 * a graph, connecting statements according to their {@link ControlDependencyArc control}
 * and {@link DataDependencyArc data} relationships. You can build one manually or use
 * the {@link tfm.visitors.pdg.PDGBuilder PDGBuilder}.
 * @see tfm.exec.Config Config (for the available variations of the PDG)
 */
public class PDGGraph extends Graph {
    public static boolean isRanked = false, isSorted = false;

    private CFGGraph cfgGraph;

    public PDGGraph() {
        setRootVertex(new GraphNode<>(getNextVertexId(), getRootNodeData(), new MethodDeclaration()));
    }

    public PDGGraph(CFGGraph cfgGraph) {
        this();
        this.cfgGraph = cfgGraph;
    }

    protected String getRootNodeData() {
        return "Entry";
    }

    public GraphNode<?> addNode(GraphNode<?> node) {
        GraphNode<?> vertex = new GraphNode<>(node);
        super.addVertex(vertex);

        return vertex;
    }

    @Override
    public <ASTNode extends Node> GraphNode<ASTNode> addNode(String instruction, ASTNode node) {
        return addNode(getNextVertexId(), instruction, node);
    }

    public <ASTNode extends Node> GraphNode<ASTNode> addNode(int id, String instruction, ASTNode node) {
        GraphNode<ASTNode> vertex = new GraphNode<>(id, instruction, node);
        super.addVertex(vertex);

        return vertex;
    }

    @SuppressWarnings("unchecked")
    private void addArc(Arc<? extends ArcData> arc) {
        super.addEdge((Arrow<String, ArcData>) arc);
    }

    public void addControlDependencyArc(GraphNode<?> from, GraphNode<?> to) {
        ControlDependencyArc controlDependencyArc = new ControlDependencyArc(from, to);

        this.addArc(controlDependencyArc);
    }

    public void addDataDependencyArc(GraphNode<?> from, GraphNode<?> to, String variable) {
        DataDependencyArc dataDataDependencyArc = new DataDependencyArc(from, to, variable);

        this.addArc(dataDataDependencyArc);
    }

    public Set<GraphNode<?>> getNodesAtLevel(int level) {
        return getVerticies().stream()
                .map(vertex -> (GraphNode<?>) vertex)
                .filter(node -> getLevelOf(node) == level)
                .collect(Collectors.toSet());
    }

    public int getLevels() {
        return getVerticies().stream()
                .map(vertex -> (GraphNode<?>) vertex)
                .max(Comparator.comparingInt(this::getLevelOf))
                .map(node -> getLevelOf(node) + 1)
                .orElse(0);
    }

    public int getLevelOf(int nodeId) {
        return findNodeById(nodeId)
                .map(this::getLevelOf)
                .orElseThrow(() -> new NodeNotFoundException("Node with id " + nodeId + " not found in PDG graph"));
    }

    public int getLevelOf(@NotNull GraphNode<?> node) {
        Optional<ControlDependencyArc> optionalControlDependencyArc = node.getIncomingArcs().stream()
                .filter(Arc::isControlDependencyArrow)
                .filter(a -> a.getFromNode().getId() < node.getId())
                .findFirst()
                .map(arc -> (ControlDependencyArc) arc);

        if (!optionalControlDependencyArc.isPresent()) {
            return 0;
        }

        GraphNode<?> parent = optionalControlDependencyArc.get().getFromNode();

        return 1 + getLevelOf(parent);
    }

    public void setCfgGraph(CFGGraph cfgGraph) {
        this.cfgGraph = cfgGraph;
    }

    @Override
    public String toGraphvizRepresentation() {
        String lineSep = System.lineSeparator();

        String nodesDeclaration = getNodes().stream()
                .sorted(Comparator.comparingInt(GraphNode::getId))
                .map(GraphNode::toGraphvizRepresentation)
                .collect(Collectors.joining(lineSep));

        StringBuilder rankedNodes = new StringBuilder();

        // No level 0 is needed (only one node)
        for (int i = 0; i < getLevels(); i++) {
            Set<GraphNode<?>> levelNodes = getNodesAtLevel(i);

            if (levelNodes.size() <= 1) {
                continue;
            }

            // rank same
            if (isRanked) {
                rankedNodes.append("{ rank = same; ")
                        .append(levelNodes.stream()
                                .map(node -> String.valueOf(node.getId()))
                                .collect(Collectors.joining(";")))
                        .append(" }")
                        .append(lineSep);
            }

            // invisible arrows for ordering
            if (isSorted) {
                rankedNodes.append(levelNodes.stream()
                        .sorted(Comparator.comparingInt(GraphNode::getId))
                        .map(node -> String.valueOf(node.getId()))
                        .collect(Collectors.joining(" -> ")))
                        .append("[style = invis];")
                        .append(lineSep);
            }
        }

        String arrows =
                getArcs().stream()
                        .sorted(Comparator.comparingInt(arrow -> ((GraphNode<?>) arrow.getFrom()).getId()))
                        .map(Arc::toGraphvizRepresentation)
                        .collect(Collectors.joining(lineSep));


        return "digraph g{" + lineSep +
                "splines=true;" + lineSep +
                nodesDeclaration + lineSep +
                arrows + lineSep +
                rankedNodes.toString() +
                "}";
    }

    @Override
    public PDGGraph slice(SlicingCriterion slicingCriterion) {
        Optional<GraphNode<?>> optionalGraphNode = slicingCriterion.findNode(this);

        if (!optionalGraphNode.isPresent()) {
            throw new NodeNotFoundException(slicingCriterion);
        }

        GraphNode<?> node = optionalGraphNode.get();

        // Simply get slice nodes from GraphNode
        Set<Integer> sliceNodes = getSliceNodes(new HashSet<>(), node);

        PDGGraph sliceGraph = new PDGGraph();

        Node astCopy = ASTUtils.cloneAST(node.getAstNode());

        astCopy.accept(new PDGBuilder(sliceGraph), sliceGraph.getRootNode());

        for (GraphNode<?> sliceNode : sliceGraph.getNodes()) {
            if (!sliceNodes.contains(sliceNode.getId())) {
                Logger.log("Removing node " + sliceNode.getId());
                sliceNode.getAstNode().removeForced();
                sliceGraph.removeNode(sliceNode);
            }
        }

        return sliceGraph;
    }

    private Set<Integer> getSliceNodes(Set<Integer> visited, GraphNode<?> root) {
        visited.add(root.getId());

        for (Arc<ArcData> arc : root.getIncomingArcs()) {
            GraphNode<?> from = arc.getFromNode();

            if (visited.contains(from.getId()))
                continue;

            getSliceNodes(visited, from);
        }

        return visited;
    }

    public CFGGraph getCfgGraph() {
        return cfgGraph;
    }
}
