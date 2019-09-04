package tfm.graphs;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.EmptyStmt;
import edg.graphlib.Arrow;
import tfm.arcs.Arc;
import tfm.arcs.cfg.ControlFlowArc;
import tfm.nodes.GraphNode;
import tfm.slicing.SlicingCriterion;
import tfm.utils.NodeNotFoundException;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class CFGGraph extends Graph {

    public CFGGraph() {
        super();
        setRootVertex(new GraphNode<>(getNextVertexId(), getRootNodeData(), new EmptyStmt()));
    }

    @Override
    public <ASTNode extends Node> GraphNode<ASTNode> addNode(String instruction, ASTNode node) {
        GraphNode<ASTNode> vertex = new GraphNode<>(getNextVertexId(), instruction, node);
        this.addVertex(vertex);

        return vertex;
    }


    protected String getRootNodeData() {
        return "Start";
    }

    @SuppressWarnings("unchecked")
    public void addControlFlowEdge(GraphNode from, GraphNode to) {
        super.addEdge((Arrow) new ControlFlowArc(from, to));
    }

    @Override
    public String toGraphvizRepresentation() {
        String lineSep = System.lineSeparator();

        String nodes = getNodes().stream()
                .sorted(Comparator.comparingInt(GraphNode::getId))
                .map(node -> String.format("%s [label=\"%s: %s\"]", node.getId(), node.getId(), node.getData()))
                .collect(Collectors.joining(lineSep));

        String arrows =
                getArrows().stream()
                        .sorted(Comparator.comparingInt(arrow -> ((GraphNode) arrow.getFrom()).getId()))
                        .map(arrow -> ((Arc) arrow).toGraphvizRepresentation())
                        .collect(Collectors.joining(lineSep));

        return "digraph g{" + lineSep +
                nodes + lineSep +
                arrows + lineSep +
                "}";
    }

    @Override
    public Graph slice(SlicingCriterion slicingCriterion) {
        return this;
    }

    public Set<GraphNode<?>> findLastDefinitionsFrom(GraphNode<?> startNode, String variable) {
//        Logger.log("=======================================================");
//        Logger.log("Starting from " + startNode);
//        Logger.log("Looking for variable " + variable);
//        Logger.log(cfgGraph.toString());
        
        if (!this.contains(startNode)) {
            throw new NodeNotFoundException(startNode, this);
        }
        
        return findLastDefinitionsFrom(new HashSet<>(), startNode, startNode, variable);
    }

    private Set<GraphNode<?>> findLastDefinitionsFrom(Set<Integer> visited, GraphNode<?> startNode, GraphNode<?> currentNode, String variable) {
        visited.add(currentNode.getId());

//        Logger.log("On " + currentNode);

        Set<GraphNode<?>> res = new HashSet<>();

        for (Arc<?> arc : currentNode.getIncomingArcs()) {
            ControlFlowArc controlFlowArc = (ControlFlowArc) arc;

            GraphNode<?> from = controlFlowArc.getFromNode();

//            Logger.log("Arrow from node: " + from);

            if (!Objects.equals(startNode, from) && visited.contains(from.getId())) {
//                Logger.log("It's already visited. Continuing...");
                continue;
            }

            if (from.getDefinedVariables().contains(variable)) {
//                Logger.log("Contains defined variable: " + variable);
                res.add(from);
            } else {
//                Logger.log("Doesn't contain the variable, searching inside it");
                res.addAll(findLastDefinitionsFrom(visited, startNode, from, variable));
            }
        }

//        Logger.format("Done with node %s", currentNode.getId());

        return res;
    }
}
