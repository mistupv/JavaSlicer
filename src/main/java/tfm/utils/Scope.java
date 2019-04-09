package tfm.utils;

import tfm.nodes.Node;
import tfm.variables.actions.VariableDeclaration;
import tfm.variables.actions.VariableDefinition;
import tfm.variables.actions.VariableUse;

import java.util.*;

public class Scope {

    private Map<String, List<VariableDeclaration>> variableDeclarations;
    private Map<String, List<VariableDefinition>> variableDefinitions;
    private Map<String, List<VariableUse>> variableUses;

    private Node node;

    public Scope(Node node) {
        this.node = node;

        variableDeclarations = new HashMap<>();
        variableDefinitions = new HashMap<>();
        variableUses = new HashMap<>();
    }

    public Scope(Node node, Scope scope) {
        this.node = node;

        variableDeclarations = new HashMap<>(scope.variableDeclarations);
        variableDefinitions = new HashMap<>(scope.variableDefinitions);
        variableUses = new HashMap<>(scope.variableUses);
    }

    public Node getNode() {
        return node;
    }

    public List<VariableDeclaration> getDeclarationsOf(String variable) {
        return variableDeclarations.getOrDefault(variable, new ArrayList<>());
    }

    public List<VariableDefinition> getDefinitionsOf(String variable) {
        return variableDefinitions.getOrDefault(variable, new ArrayList<>());
    }

    public List<VariableUse> getUsesOf(String variable) {
        return variableUses.getOrDefault(variable, new ArrayList<>());
    }

    public Set<String> getDeclaredVariables() {
        return variableDeclarations.keySet();
    }

    public Optional<VariableDefinition> getLastDefinitionOf(String variable) {
        List<VariableDefinition> definitions = getDefinitionsOf(variable);

        if (definitions.isEmpty())
            return Optional.empty();

        return Optional.of(definitions.get(definitions.size() - 1));
    }
}
