package tfm.graphs;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import edg.graphlib.Arrow;
import org.jetbrains.annotations.NotNull;
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
import java.util.stream.Collectors;

public class PDGGraph extends Graph {

    private CFGGraph cfgGraph;

    public PDGGraph() {
        super();
    }

    public PDGGraph(CFGGraph cfgGraph) {
        super();
        this.cfgGraph = cfgGraph;
    }

    public GraphNode<?> getRootNode() {
        Iterator<GraphNode<?>> iterator = this.getNodesAtLevel(0).iterator();

        if (iterator.hasNext()) {
            return iterator.next();
        }

        return null;
    }

    public void addControlDependencyArc(GraphNode<?> from, GraphNode<?> to) {
        this.addEdge(from, to);
    }

    public void addDataDependencyArc(GraphNode<?> from, GraphNode<?> to, String variable) {
        this.addEdge(from, to, new DataDependencyArc(variable));
    }

    public Set<GraphNode<?>> getNodesAtLevel(int level) {
        return getNodes().stream()
                .filter(node -> getLevelOf(node) == level)
                .collect(Collectors.toSet());
    }

    public int getLevels() {
        return getNodes().stream()
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
        Optional<ControlDependencyArc> optionalControlDependencyArc = incomingEdgesOf(node).stream()
                .filter(Arc::isControlDependencyArrow)
                .findFirst()
                .map(arc -> (ControlDependencyArc) arc);

        if (!optionalControlDependencyArc.isPresent()) {
            return 0;
        }

        GraphNode<?> parent = this.getEdgeSource(optionalControlDependencyArc.get());

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
            rankedNodes.append("{ rank = same; ")
                    .append(levelNodes.stream()
                        .map(node -> String.valueOf(node.getId()))
                        .collect(Collectors.joining(";")))
                    .append(" }")
                    .append(lineSep);

            // invisible arrows for ordering
            rankedNodes.append(levelNodes.stream()
                        .sorted(Comparator.comparingInt(GraphNode::getId))
                        .map(node -> String.valueOf(node.getId()))
                        .collect(Collectors.joining(" -> ")))
                    .append("[style = invis];")
                    .append(lineSep);
        }

        String arrows =
                getArcs().stream()
                        .sorted(Comparator.comparingInt(arc -> this.getEdgeSource(arc).getId()))
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

        GraphNode node = optionalGraphNode.get();

//        // DEPRECATED - Find CFGNode and find last definition of variable
//        CFGNode cfgNode = this.cfgGraph.findNodeByASTNode(node.getAstNode())
//                .orElseThrow(() -> new NodeNotFoundException("CFGNode not found"));
//
//        Set<CFGNode<?>> definitionNodes = Utils.findLastDefinitionsFrom(cfgNode, slicingCriterion.getVariable());
//
//        Logger.format("Slicing node: %s", node);
//
//        // Get slice nodes from definition nodes
//        Set<Integer> sliceNodes = definitionNodes.stream()
//                .flatMap(definitionNode -> getSliceNodes(new HashSet<>(), this.findNodeByASTNode(definitionNode.getAstNode()).get()).stream())
//                .collect(Collectors.toSet());
//
//        sliceNodes.add(node.getId());

        // Simply get slice nodes from GraphNode
        Set<Integer> sliceNodes = getSliceNodes(new HashSet<>(), node);

        PDGGraph sliceGraph = new PDGGraph();

        Node astCopy = ASTUtils.cloneAST(node.getAstNode());

        astCopy.accept(new PDGBuilder(sliceGraph), sliceGraph.getNodesAtLevel(0).iterator().next());

        for (GraphNode<?> sliceNode : sliceGraph.getNodes()) {
            if (!sliceNodes.contains(sliceNode.getId())) {
                Logger.log("Removing node " + sliceNode.getId());
                sliceNode.getAstNode().removeForced();
                sliceGraph.removeNode(sliceNode);
            }
        }

//        for (Arc arc : getArcs()) {
//            Optional<GraphNode> fromOptional = sliceGraph.findNodeById(arc.getFromNode().getId());
//            Optional<GraphNode> toOptional = sliceGraph.findNodeById(arc.getToNode().getId());
//
//            if (fromOptional.isPresent() && toOptional.isPresent()) {
//                GraphNode from = fromOptional.get();
//                GraphNode to = toOptional.get();
//
//                if (arc.isControlDependencyArrow()) {
//                    sliceGraph.addControlDependencyArc(from, to);
//                } else {
//                    DataDependencyArc dataDependencyArc = (DataDependencyArc) arc;
//                    sliceGraph.addDataDependencyArc(from, to, dataDependencyArc.getData().getVariables().get(0));
//                }
//            }
//        }

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
