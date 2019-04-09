package tfm.variables;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import tfm.nodes.Node;
import tfm.variables.actions.VariableDeclaration;
import tfm.variables.actions.VariableUse;
import tfm.variables.actions.VariableDefinition;

import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class VariableSet {

    private Set<Variable> variableSet;

    public VariableSet() {
        variableSet = new HashSet<>();
    }

    public  Optional<Variable> findVariableByName(String name) {
        return variableSet.stream()
                .filter(variable -> Objects.equals(variable.getName(), name))
                .findFirst();
    }

    public  Optional<Variable> findVariableByDeclaration(VariableDeclaration declaration) {
        return variableSet.stream()
                .filter(variable -> Objects.equals(variable.getDeclaration(), declaration))
                .findFirst();
    }
    
    public boolean containsVariable(String name) {
        return findVariableByName(name).isPresent();
    }

    public List<VariableDefinition> getDefinitions(@NonNull String variableName) {
        Optional<Variable> variable = findVariableByName(variableName);

        return variable.isPresent() ? variable.get().getDefinitions() : Collections.emptyList();
    }

    public List<VariableDefinition> getLastDefinitionsOf(@NonNull String variableName, int number) {
        List<VariableDefinition> definitions = getDefinitions(variableName);

        if (definitions.size() <= number)
            return definitions;

        return definitions.stream().skip(Math.max(0, definitions.size() - number)).collect(Collectors.toList());
    }

    public Optional<VariableDefinition> getLastDefinitionOf(@NonNull String variableName) {
        List<VariableDefinition> definitions = getLastDefinitionsOf(variableName, 1);

        return definitions.size() == 1 ? Optional.of(definitions.get(0)) : Optional.empty();
    }

    public Optional<VariableDefinition> getLastDefinitionOf(@NonNull String variableName, Node fromNode) {
        List<VariableDefinition> definitions = getLastDefinitionsOf(variableName, 1);

        return definitions.stream()
                .filter(variableDefinition -> variableDefinition.getNode().getId() < fromNode.getId())
                .max(Comparator.comparingInt(vd -> vd.getNode().getId()));
    }

    public void addUse(String variableName, VariableUse variableUse) {
        findVariableByName(variableName)
                .ifPresent(variable -> variable.addUse(variableUse));
    }

    public void addDefinition(String variableName, VariableDefinition variableDefinition) {
        findVariableByName(variableName)
                .ifPresent(variable -> variable.addDefinition(variableDefinition));
    }

    public Variable addVariable(String variableName, VariableDeclaration variableDeclaration) {
        Variable newVariable = new Variable(variableName, variableDeclaration);

        // check if it already exists
        if (this.variableSet.contains(newVariable)) {
            throw new IllegalStateException("Variable " + variableName + " already exists in VariableSet");
        }

        this.variableSet.add(newVariable);

        return newVariable;
    }

    public Set<Variable> getVariables() {
        return variableSet;
    }
}
