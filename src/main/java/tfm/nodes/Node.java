package tfm.nodes;

import com.github.javaparser.ast.stmt.Statement;
import edg.graphlib.Arrow;
import edg.graphlib.Vertex;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import tfm.arcs.data.ArcData;
import tfm.utils.Utils;
import tfm.variables.VariableExtractor;

import java.util.*;
import java.util.stream.Collectors;

public class Node<ASTNode extends com.github.javaparser.ast.Node> extends Vertex<String, ArcData> {

    private ASTNode astNode;

    protected Set<String> declaredVariables;
    protected Set<String> definedVariables;
    protected Set<String> usedVariables;

    public <N extends Node<ASTNode>> Node(N node) {
        this(
                node.getId(),
                node.getData(),
                node.getAstNode(),
                node.getIncomingArrows(),
                node.getOutgoingArrows(),
                node.getDeclaredVariables(),
                node.getDefinedVariables(),
                node.getUsedVariables()
        );
    }

    public Node(int id, String representation, @NotNull ASTNode astNode) {
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

    public Node(
                int id,
                String representation,
                @NonNull ASTNode astNode,
                Collection<? extends Arrow<String, ArcData>> incomingArcs,
                Collection<? extends Arrow<String, ArcData>> outgoingArcs,
                Set<String> declaredVariables,
                Set<String> definedVariables,
                Set<String> usedVariables
    ) {
        super(String.valueOf(id), representation);

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

    public int getId() {
        return Integer.parseInt(getName());
    }

    public String toString() {
        return String.format("Node{id: %s, data: '%s', in: %s, out: %s}",
                getName(),
                getData(),
                getIncomingArrows().stream().map(arrow -> arrow.getFrom().getName()).collect(Collectors.toList()),
                getOutgoingArrows().stream().map(arc -> arc.getTo().getName()).collect(Collectors.toList()));
    }

    public ASTNode getAstNode() {
        return astNode;
    }

    public Optional<Integer> getFileLineNumber() {
        return astNode.getBegin().isPresent() ? Optional.of(astNode.getBegin().get().line) : Optional.empty();
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

        if (!(o instanceof Node))
            return false;

        Node other = (Node) o;

        return Objects.equals(getData(), other.getData())
//                && Objects.equals(getIncomingArrows(), other.getIncomingArrows())
//                && Objects.equals(getOutgoingArrows(), other.getOutgoingArrows())
                && Objects.equals(astNode, other.astNode);
                // && Objects.equals(getName(), other.getName()) ID IS ALWAYS UNIQUE, SO IT WILL NEVER BE THE SAME
    }

    public String toGraphvizRepresentation() {
        return String.format("%s[label=\"%s: %s\"];", getId(), getId(), getData());
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
}
