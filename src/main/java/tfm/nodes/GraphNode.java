package tfm.nodes;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.Statement;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import tfm.utils.Utils;
import tfm.variables.VariableExtractor;

import java.util.*;

public class GraphNode<N extends Node> {

    private int id;
    private String instruction;

    protected N astNode;

    protected Set<String> declaredVariables;
    protected Set<String> definedVariables;
    protected Set<String> usedVariables;

    public <N1 extends GraphNode<N>> GraphNode(N1 node) {
        this(
                node.getId(),
                node.getInstruction(),
                node.getAstNode(),
                node.getDeclaredVariables(),
                node.getDefinedVariables(),
                node.getUsedVariables()
        );
    }

    public GraphNode(int id, String instruction, @NotNull N astNode) {
        this(
                id,
                instruction,
                astNode,
                Utils.emptySet(),
                Utils.emptySet(),
                Utils.emptySet()
        );
    }

    public GraphNode(
                int id,
                String instruction,
                @NonNull N astNode,
                Set<String> declaredVariables,
                Set<String> definedVariables,
                Set<String> usedVariables
    ) {
        this.id = id;
        this.instruction = instruction;
        this.astNode = astNode;

        this.declaredVariables = declaredVariables;
        this.definedVariables = definedVariables;
        this.usedVariables = usedVariables;

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
        return String.format("GraphNode{id: %s, instruction: '%s'}",
                getId(),
                getInstruction()
        );
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

        GraphNode<?> other = (GraphNode<?>) o;

        return Objects.equals(getId(), other.getId())
                && Objects.equals(getInstruction(), other.getInstruction())
                && Objects.equals(astNode, other.astNode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId(), getInstruction(), getAstNode());
    }

    public String toGraphvizRepresentation() {
        String text = getInstruction().replace("\\", "\\\\")
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

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }
}
