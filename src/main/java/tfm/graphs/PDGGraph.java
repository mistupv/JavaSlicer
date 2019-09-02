package tfm.graphs;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.Statement;
import edg.graphlib.Arrow;
import tfm.arcs.Arc;
import tfm.arcs.pdg.ControlDependencyArc;
import tfm.arcs.pdg.DataDependencyArc;
import tfm.nodes.CFGNode;
import tfm.nodes.GraphNode;
import tfm.nodes.PDGNode;
import tfm.slicing.SlicingCriterion;
import tfm.utils.ASTUtils;
import tfm.utils.Logger;
import tfm.utils.NodeNotFoundException;
import tfm.utils.Utils;
import tfm.visitors.PDGCFGVisitor;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class PDGGraph extends Graph<PDGNode<?>> {

    private CFGGraph cfgGraph;

    public PDGGraph() {
        setRootVertex(new PDGNode<>(getNextVertexId(), getRootNodeData(), new EmptyStmt()));
    }

    public PDGGraph(CFGGraph cfgGraph) {
        this();
        this.cfgGraph = cfgGraph;
    }

    protected String getRootNodeData() {
        return "Entry";
    }

    public PDGNode addNode(PDGNode<?> node) {
        PDGNode<?> vertex = new PDGNode<>(node);
        super.addVertex(vertex);

        return vertex;
    }

    @Override
    public <ASTNode extends Node> PDGNode<ASTNode> addNode(String instruction, ASTNode node) {
        return addNode(getNextVertexId(), instruction, node);
    }

    public <ASTNode extends Node> PDGNode<ASTNode> addNode(int id, String instruction, ASTNode node) {
        PDGNode<ASTNode> vertex = new PDGNode<>(id, instruction, node);
        super.addVertex(vertex);

        return vertex;
    }

    @SuppressWarnings("unchecked")
    private void addArc(Arc arc) {
        super.addEdge(arc);
    }

    public void addControlDependencyArc(PDGNode from, PDGNode to) {
        ControlDependencyArc controlDependencyArc = new ControlDependencyArc(from, to);

        this.addArc(controlDependencyArc);
    }

    public void addDataDependencyArc(PDGNode from, PDGNode to, String variable) {
        DataDependencyArc dataDataDependencyArc = new DataDependencyArc(from, to, variable);

        this.addArc(dataDataDependencyArc);
    }

    public Set<PDGNode> getNodesAtLevel(int level) {
        return getVerticies().stream()
                .map(vertex -> (PDGNode) vertex)
                .filter(node -> node.getLevel() == level)
                .collect(Collectors.toSet());
    }

    public int getLevels() {
        return getVerticies().stream()
                .map(vertex -> (PDGNode) vertex)
                .max(Comparator.comparingInt(PDGNode::getLevel))
                .map(PDGNode::getLevel)
                .get() + 1;
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
            Set<PDGNode> levelNodes = getNodesAtLevel(i);

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
                        .sorted(Comparator.comparingInt(PDGNode::getId))
                        .map(node -> String.valueOf(node.getId()))
                        .collect(Collectors.joining(" -> ")))
                    .append("[style = invis];")
                    .append(lineSep);
        }

        String arrows =
                getArcs().stream()
                        .sorted(Comparator.comparingInt(arrow -> ((GraphNode) arrow.getFrom()).getId()))
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
        Optional<PDGNode<?>> optionalPDGNode = slicingCriterion.findNode(this);

        if (!optionalPDGNode.isPresent()) {
            throw new NodeNotFoundException(slicingCriterion);
        }

        PDGNode node = optionalPDGNode.get();

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

        // Simply get slice nodes from PDGNode
        Set<Integer> sliceNodes = getSliceNodes(new HashSet<>(), node);

        PDGGraph sliceGraph = new PDGGraph();

        Node astCopy = ASTUtils.cloneAST(node.getAstNode());

        astCopy.accept(new PDGCFGVisitor(sliceGraph), sliceGraph.getRootNode());

        for (PDGNode sliceNode : sliceGraph.getNodes()) {
            if (!sliceNodes.contains(sliceNode.getId())) {
                Logger.log("Removing node " + sliceNode.getId());
                sliceNode.getAstNode().removeForced();
                sliceGraph.removeNode(sliceNode);
            }
        }

//        for (Arc arc : getArcs()) {
//            Optional<PDGNode> fromOptional = sliceGraph.findNodeById(arc.getFromNode().getId());
//            Optional<PDGNode> toOptional = sliceGraph.findNodeById(arc.getToNode().getId());
//
//            if (fromOptional.isPresent() && toOptional.isPresent()) {
//                PDGNode from = fromOptional.get();
//                PDGNode to = toOptional.get();
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

    private Set<Integer> getSliceNodes(Set<Integer> visited, PDGNode<?> root) {
        visited.add(root.getId());

        for (Arrow arrow : root.getIncomingArrows()) {
            Arc arc = (Arc) arrow;

            PDGNode<?> from = (PDGNode) arc.getFromNode();

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
