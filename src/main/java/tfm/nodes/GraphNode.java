package tfm.nodes;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.Statement;
import edg.graphlib.Arrow;
import edg.graphlib.Vertex;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import tfm.arcs.Arc;
import tfm.arcs.data.ArcData;
import tfm.utils.Utils;
import tfm.variables.VariableExtractor;

import java.util.*;
import java.util.stream.Collectors;

public class GraphNode<N extends Node> extends Vertex<String, ArcData> {

    private int id;

    protected N astNode;

    protected Set<String> declaredVariables;
    protected Set<String> definedVariables;
    protected Set<String> usedVariables;

    public <N1 extends GraphNode<N>> GraphNode(N1 node) {
        this(
                node.getId(),
                node.getData(),
                node.getAstNode(),
                node.getIncomingArcs(),
                node.getOutgoingArcs(),
                node.getDeclaredVariables(),
                node.getDefinedVariables(),
                node.getUsedVariables()
        );
    }

    public GraphNode(int id, String representation, @NotNull N astNode) {
        this(
                id,
                representation,
                astNode,
                Utils.emptyList(),
                Utils.emptyList(),
                Utils.emptySet(),
                Utils.emptySet(),
                Utils.emptySet()
        );
    }

    public GraphNode(
                int id,
                String representation,
                @NonNull N astNode,
                Collection<? extends Arrow<String, ArcData>> incomingArcs,
                Collection<? extends Arrow<String, ArcData>> outgoingArcs,
                Set<String> declaredVariables,
                Set<String> definedVariables,
                Set<String> usedVariables
    ) {
        super(null, representation);

        this.id = id;

        this.astNode = astNode;

        this.declaredVariables = declaredVariables;
        this.definedVariables = definedVariables;
        this.usedVariables = usedVariables;

        this.setIncomingArcs(incomingArcs);
        this.setOutgoingArcs(outgoingArcs);

        if (astNode instanceof Statement) {
            extractVariables((Statement) astNode);
        }
    }

    private void extractVariables(@NonNull Statement statement) {
        new VariableExtractor()
                .setOnVariableDeclarationListener(variable -> this.declaredVariables.add(variable))
                .setOnVariableDefinitionListener(variable -> this.definedVariables.add(variable))
                .setOnVariableUseListener(variable -> this.usedVariables.add(variable))
                .visit(statement);
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public String toString() {
        return String.format("GraphNode{id: %s, data: '%s', in: %s, out: %s}",
                getId(),
                getData(),
                getIncomingArcs().stream().map(arc -> arc.getFromNode().getId()).collect(Collectors.toList()),
                getOutgoingArcs().stream().map(arc -> arc.getToNode().getId()).collect(Collectors.toList()));
    }

    public N getAstNode() {
        return astNode;
    }

    public void setAstNode(N node) {
        this.astNode = node;
    }

    public Optional<Integer> getFileLineNumber() {
        return astNode.getBegin()
                .map(begin -> begin.line);
    }

    public void addDeclaredVariable(String variable) {
        declaredVariables.add(variable);
    }

    public void addDefinedVariable(String variable) {
        definedVariables.add(variable);
    }

    public void addUsedVariable(String variable) {
        usedVariables.add(variable);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof GraphNode))
            return false;

        GraphNode other = (GraphNode) o;

        return Objects.equals(getData(), other.getData())
                && Objects.equals(astNode, other.astNode);
//                && Objects.equals(getIncomingArrows(), other.getIncomingArrows())
//                && Objects.equals(getOutgoingArrows(), other.getOutgoingArrows())
//                && Objects.equals(getName(), other.getName()) ID IS ALWAYS UNIQUE, SO IT WILL NEVER BE THE SAME
    }

    public String toGraphvizRepresentation() {
        String text = getData().replace("\\", "\\\\")
                .replace("\"", "\\\"");
        return String.format("%s[label=\"%s: %s\"];", getId(), getId(), text);
    }

    public Set<String> getDeclaredVariables() {
        return declaredVariables;
    }

    public Set<String> getDefinedVariables() {
        return definedVariables;
    }

    public Set<String> getUsedVariables() {
        return usedVariables;
    }

    public List<Arc<ArcData>> getIncomingArcs() {
        return super.getIncomingArrows().stream()
                .map(arrow -> (Arc<ArcData>) arrow)
                .collect(Collectors.toList());
    }

    public List<Arc<ArcData>> getOutgoingArcs() {
        return super.getOutgoingArrows().stream()
                .map(arrow -> (Arc<ArcData>) arrow)
                .collect(Collectors.toList());
    }

    public <A extends Arrow<String, ArcData>, C extends Collection<A>> void setIncomingArcs(C arcs) {
        for (A arc : arcs) {
            this.addIncomingEdge(arc.getFrom(), arc.getCost());
        }
    }

    public <A extends Arrow<String, ArcData>, C extends Collection<A>> void setOutgoingArcs(C arcs) {
        for (A arc : arcs) {
            this.addOutgoingEdge(arc.getTo(), arc.getCost());
        }
    }

    /**
     * Deprecated. Use getIncomingArcs instead
     * @throws UnsupportedOperationException
     */
    @Deprecated
    @Override
    public List<Arrow<String, ArcData>> getIncomingArrows() {
        return super.getIncomingArrows();
    }

    /**
     * Deprecated. Use getOutgoingArcs instead
     * @throws UnsupportedOperationException
     */
    @Deprecated
    @Override
    public List<Arrow<String, ArcData>> getOutgoingArrows() {
        return super.getOutgoingArrows();
    }
}
