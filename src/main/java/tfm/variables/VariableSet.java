package tfm.variables;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import tfm.variables.actions.VariableDeclaration;
import tfm.variables.actions.VariableUse;
import tfm.variables.actions.VariableDefinition;

import java.util.*;

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

    public  Optional<VariableDefinition> getLastDefinitionOf(@NonNull String variableName) {
        Optional<Variable> variable = findVariableByName(variableName);

        if (!variable.isPresent())
            return Optional.empty();

        List<VariableDefinition> writes = variable.get().getDefinitions();

        if (writes.isEmpty())
            return Optional.empty();

        return Optional.of(writes.get(writes.size() - 1));
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
