package tfm.graphs;

import com.github.javaparser.ast.Node;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.io.DOTExporter;
import tfm.arcs.Arc;
import tfm.nodes.GraphNode;
import tfm.utils.NodeFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 *
 * */
public abstract class Graph extends DefaultDirectedGraph<GraphNode<?>, Arc> {

    private int nextVertexId = 0;

    public Graph() {
        super(null, null, false);
    }

    private <ASTNode extends Node> GraphNode<ASTNode> addNode(GraphNode<ASTNode> node) {
        this.addVertex(node);

        return node;
    }

    private <ASTNode extends Node> GraphNode<ASTNode> addNode(int id, String instruction, ASTNode node) {
        GraphNode<ASTNode> newNode = NodeFactory.graphNode(id, instruction, node);

        return this.addNode(newNode);
    }

    public <ASTNode extends Node> GraphNode<ASTNode> addNode(String instruction, ASTNode node) {
        return this.addNode(getNextVertexId(), instruction, node);
    }

    /**
     * Adds the given node to the graph.
     *
     * One must be careful with this method, as the given node will have
     * an id corresponding to the graph in which it was created, and may not fit
     * in the current graph.
     *
     * @param node the node to add to the graph
     * @param copyId whether to copy the node id or generate a new one
     * @return the node instance added to the graph
     */
    public <ASTNode extends Node> GraphNode<ASTNode> addNode(GraphNode<ASTNode> node, boolean copyId) {
        GraphNode<ASTNode> copy = NodeFactory.computedGraphNode(
                copyId ? node.getId() : getNextVertexId(),
                node.getInstruction(),
                node.getAstNode(),
                node.getDeclaredVariables(),
                node.getDefinedVariables(),
                node.getUsedVariables()
        );

        this.addVertex(copy);

        return copy;
    }

    @SuppressWarnings("unchecked")
    public <ASTNode extends Node> Optional<GraphNode<ASTNode>> findNodeByASTNode(ASTNode astNode) {
        return vertexSet().stream()
                .filter(node -> Objects.equals(node.getAstNode(), astNode))
                .findFirst()
                .map(node -> (GraphNode<ASTNode>) node);
    }

    public Optional<GraphNode<?>> findNodeById(int id) {
        return vertexSet().stream()
                .filter(node -> Objects.equals(node.getId(), id))
                .findFirst();
    }

    @Override
    public String toString() {
        return vertexSet().stream()
                .sorted(Comparator.comparingInt(GraphNode::getId))
                .map(GraphNode::toString)
                .collect(Collectors.joining(System.lineSeparator()));
    }

    protected synchronized int getNextVertexId() {
        return nextVertexId++;
    }

    public List<GraphNode<?>> findDeclarationsOfVariable(String variable) {
        return vertexSet().stream()
                .filter(node -> node.getDeclaredVariables().contains(variable))
                .collect(Collectors.toList());
    }

    public boolean isEmpty() {
        return this.vertexSet().isEmpty();
    }

    public DOTExporter<GraphNode<?>, Arc> getDOTExporter() {
        return new DOTExporter<>(
                graphNode -> String.valueOf(graphNode.getId()),
                GraphNode::getInstruction,
                Arc::getLabel,
                null,
                Arc::getDotAttributes);
    }

    /**
     * Modifies a current node in the graph by the changes done in the MutableGraphNode instance
     * inside the function passed as parameter
     *
     * @param id the id of the node to be modified
     * @param modifyFn a consumer which takes a MutableGraphNode as parameter
     */
    public <ASTNode extends Node> void modifyNode(int id, Consumer<MutableGraphNode<ASTNode>> modifyFn) {
        this.findNodeById(id).ifPresent(node -> {
            Set<Arc> incomingArcs = new HashSet<>(incomingEdgesOf(node));
            Set<Arc> outgoingArcs = new HashSet<>(outgoingEdgesOf(node));

            this.removeVertex(node);

            MutableGraphNode<ASTNode> modifiedNode = new MutableGraphNode<>((GraphNode<ASTNode>) node);

            modifyFn.accept(modifiedNode);

            GraphNode<ASTNode> newNode = modifiedNode.toGraphNode();

            this.addVertex(newNode);

            for (Arc incomingArc : incomingArcs) {
                GraphNode<?> from = getEdgeSource(incomingArc);
                this.addEdge(from, newNode, incomingArc);
            }

            for (Arc outgoingArc : outgoingArcs) {
                GraphNode<?> to = getEdgeTarget(outgoingArc);
                this.addEdge(newNode, to, outgoingArc);
            }
        });
    }

    public static class MutableGraphNode<ASTNode extends Node> {
        private int id;
        private String instruction;
        private ASTNode astNode;
        private Set<String> declaredVariables;
        private Set<String> definedVariables;
        private Set<String> usedVariables;

        private boolean mustCompute;

        MutableGraphNode(GraphNode<ASTNode> node) {
            this.id = node.getId();
            this.instruction = node.getInstruction();
            this.astNode = node.getAstNode();
            this.declaredVariables = node.getDeclaredVariables();
            this.definedVariables = node.getDefinedVariables();
            this.usedVariables = node.getUsedVariables();
        }

        GraphNode<ASTNode> toGraphNode() {
            return mustCompute
                    ? NodeFactory.graphNode(id, instruction, astNode)
                    : NodeFactory.computedGraphNode(
                        id,
                        instruction,
                        astNode,
                        declaredVariables,
                        definedVariables,
                        usedVariables
                    );
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getInstruction() {
            return instruction;
        }

        public void setInstruction(String instruction) {
            this.instruction = instruction;
        }

        public ASTNode getAstNode() {
            return astNode;
        }

        public void setAstNode(ASTNode astNode) {
            this.astNode = astNode;

            // If the AST node changes, we need to compute all variables for it
            mustCompute = true;
        }
    }
}
