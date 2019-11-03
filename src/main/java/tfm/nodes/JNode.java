package tfm.nodes;

import com.github.javaparser.ast.Node;
import org.checkerframework.checker.nullness.qual.NonNull;
import tfm.utils.Utils;
import tfm.variables.VariableExtractor;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class JNode<ASTNode extends Node> {

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRepresentation() {
        return representation;
    }

    public void setRepresentation(String representation) {
        this.representation = representation;
    }

    public ASTNode getAstNode() {
        return astNode;
    }

    public void setAstNode(ASTNode astNode) {
        this.astNode = astNode;
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

    private int id;
    private String representation;

    protected ASTNode astNode;
    protected Set<String> declaredVariables;
    protected Set<String> definedVariables;
    protected Set<String> usedVariables;


    public JNode(@NonNull JNode<ASTNode> node) {
        this(node.id, node.representation, node.astNode, node.declaredVariables, node.definedVariables, node.usedVariables);
    }

    public JNode(int id, @NonNull String representation, @NonNull ASTNode astNode) {
        this(id, representation, astNode, Utils.emptySet(), Utils.emptySet(), Utils.emptySet());

        extractVariables(astNode);
    }

    public JNode(int id, @NonNull String representation, @NonNull ASTNode astNode, Set<String> declaredVariables, Set<String> definedVariables, Set<String> usedVariables) {
        this.id = id;
        this.representation = representation;
        this.astNode = astNode;
        this.declaredVariables = declaredVariables;
        this.definedVariables = definedVariables;
        this.usedVariables = usedVariables;
    }

    private void extractVariables(ASTNode astNode) {
        new VariableExtractor()
                .setOnVariableDeclarationListener(variable -> this.declaredVariables.add(variable))
                .setOnVariableDefinitionListener(variable -> this.definedVariables.add(variable))
                .setOnVariableUseListener(variable -> this.usedVariables.add(variable))
                .visit(astNode);
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

    public Optional<Integer> getFileLineNumber() {
        return astNode.getBegin()
                .map(begin -> begin.line);
    }

    @Override
    public int hashCode() {
        return id + astNode.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof JNode)) {
            return false;
        }

        JNode<?> jNode = (JNode) obj;

        return jNode.id == id && Objects.equals(jNode.astNode, astNode);
    }

    @Override
    public String toString() {
        return String.format("JNode{id: %s, repr: %s, astNodeClass: %s}",
                id,
                representation,
                astNode.getClass().getName()
        );
    }

    public String toGraphvizRepresentation() {
        return String.format("%s[label=\"%s: %s\"];", getId(), getId(), getRepresentation());
    }
}
