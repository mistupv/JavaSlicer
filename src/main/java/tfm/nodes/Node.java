package tfm.nodes;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.Statement;
import edg.graphlib.Vertex;
import org.checkerframework.checker.nullness.qual.NonNull;
import tfm.arcs.data.ArcData;
import tfm.graphs.Graph;
import tfm.variables.VariableExtractor;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class Node extends Vertex<String, ArcData> {

    private Statement statement;

    protected Set<String> declaredVariables;
    protected Set<String> definedVariables;
    protected Set<String> usedVariables;

    public <N extends Node> Node(N node) {
        this(node.getId(), node.getData(), node.getStatement());
    }

    public Node(int id, String representation, @NonNull Statement statement) {
        super(String.valueOf(id), representation);

        this.statement = statement;

        this.declaredVariables = new HashSet<>();
        this.definedVariables = new HashSet<>();
        this.usedVariables = new HashSet<>();

        extractVariables(statement);
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

    public Statement getStatement() {
        return statement;
    }

    public Optional<Integer> getFileLineNumber() {
        return statement.getBegin().isPresent() ? Optional.of(statement.getBegin().get().line) : Optional.empty();
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
                && Objects.equals(getIncomingArrows(), other.getIncomingArrows())
                && Objects.equals(getOutgoingArrows(), other.getOutgoingArrows())
                && Objects.equals(statement, other.statement);
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
}
