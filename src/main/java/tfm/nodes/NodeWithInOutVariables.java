package tfm.nodes;

import com.github.javaparser.ast.Node;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.*;

public class NodeWithInOutVariables<N extends Node> extends GraphNode<N> {

    protected Map<String, InOutVariableNode> inVariablesMap;
    protected Map<String, InOutVariableNode> outVariablesMap;

    protected NodeWithInOutVariables(int id, String representation, N node) {
        super(id, representation, node);

        this.inVariablesMap = new HashMap<>();
        this.outVariablesMap = new HashMap<>();
    }

    protected NodeWithInOutVariables(int id, String representation, @NonNull N node, Set<String> declaredVariables, Set<String> definedVariables, Set<String> usedVariables) {
        super(id, representation, node, declaredVariables, definedVariables, usedVariables);

        this.inVariablesMap = new HashMap<>();
        this.outVariablesMap = new HashMap<>();
    }

    public Set<String> getInVariables() {
        return inVariablesMap.keySet();
    }

    public Optional<InOutVariableNode> getInNode(String variable) {
        return Optional.ofNullable(inVariablesMap.get(variable));
    }

    public Set<String> getOutVariables() {
        return outVariablesMap.keySet();
    }

    public Optional<InOutVariableNode> getOutNode(String variable) {
        return Optional.ofNullable(outVariablesMap.get(variable));
    }

    public void addInVariable(String variable, InOutVariableNode node) {
        this.inVariablesMap.put(variable, node);
    }

    public void addOutVariable(String variable, InOutVariableNode node) {
        this.outVariablesMap.put(variable, node);
    }
}
