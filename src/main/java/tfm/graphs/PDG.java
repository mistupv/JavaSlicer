package tfm.graphs;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import edg.graphlib.Arrow;
import org.jetbrains.annotations.NotNull;
import tfm.arcs.Arc;
import tfm.arcs.data.ArcData;
import tfm.arcs.pdg.ControlDependencyArc;
import tfm.arcs.pdg.DataDependencyArc;
import tfm.graphs.CFG.ACFG;
import tfm.nodes.GraphNode;
import tfm.slicing.SlicingCriterion;
import tfm.utils.ASTUtils;
import tfm.utils.NodeNotFoundException;

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
 * The variations of the PDG are represented as child types.
 */
public class PDG extends Graph {
    public static boolean isRanked = false, isSorted = false;

    private CFG cfg;

    public PDG() {
        setRootVertex(new GraphNode<>(getNextVertexId(), getRootNodeData(), new MethodDeclaration()));
    }

    public PDG(CFG cfg) {
        this();
        this.cfg = cfg;
    }

    protected String getRootNodeData() {
        return "Entry";
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

    public void setCfg(CFG cfg) {
        this.cfg = cfg;
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
    public Set<Integer> slice(SlicingCriterion slicingCriterion) {
        Optional<GraphNode<?>> node = slicingCriterion.findNode(this);
        if (!node.isPresent())
            throw new NodeNotFoundException(slicingCriterion);
        Set<Integer> visited = new HashSet<>();
        getSliceNodes(visited, node.get());
        return visited;
    }

    protected void getSliceNodes(Set<Integer> visited, GraphNode<?> node) {
        visited.add(node.getId());

        for (Arc<ArcData> arc : node.getIncomingArcs()) {
            GraphNode<?> from = arc.getFromNode();

            if (visited.contains(from.getId()))
                continue;

            getSliceNodes(visited, from);
        }
    }

    public CFG getCfg() {
        return cfg;
    }

    public static class APDG extends PDG {
        public APDG() {
            super();
        }

        public APDG(ACFG acfg) {
            super(acfg);
        }
    }

    public static class PPDG extends APDG {
        public PPDG() {
            super();
        }

        public PPDG(ACFG acfg) {
            super(acfg);
        }

        @Override
        protected void getSliceNodes(Set<Integer> visited, GraphNode<?> node) {
            visited.add(node.getId());

            for (Arc<ArcData> arc : node.getIncomingArcs()) {
                GraphNode<?> from = arc.getFromNode();

                if (visited.contains(from.getId()))
                    continue;

                getSliceNodesPPDG(visited, from);
            }
        }

        protected void getSliceNodesPPDG(Set<Integer> visited, GraphNode<?> node) {
            visited.add(node.getId());

            if (ASTUtils.isPseudoPredicate(node.getAstNode()))
                return;

            for (Arc<ArcData> arc : node.getIncomingArcs()) {
                GraphNode<?> from = arc.getFromNode();

                if (visited.contains(from.getId()))
                    continue;

                getSliceNodesPPDG(visited, from);
            }
        }
    }
}
