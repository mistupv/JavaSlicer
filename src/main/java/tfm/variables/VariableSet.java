package tfm.variables;

import org.checkerframework.checker.nullness.qual.NonNull;
import tfm.variables.actions.VariableDeclaration;
import tfm.variables.actions.VariableWrite;

import java.util.*;

public class VariableSet {

    private Set<Variable> variableSet;

    public VariableSet() {
        variableSet = new HashSet<>();
    }

    public <T> Optional<Variable<T>> findVariableByName(String name) {
        return variableSet.stream()
                .filter(variable -> Objects.equals(variable.getName(), name))
                .findFirst()
                .map(variable -> (Variable<T>) variable);
    }

    public <T> Optional<Variable<T>> findVariableByDeclaration(VariableDeclaration<T> declaration) {
        return variableSet.stream()
                .filter(variable -> Objects.equals(variable.getDeclaration(), declaration))
                .findFirst()
                .map(variable -> (Variable<T>) variable);
    }

    public void addVariable(Variable<?> variable) {
        this.variableSet.add(variable);
    }


    public <T> Optional<VariableWrite<T>> getLastWriteOf(@NonNull Variable<T> variable) {
        List<VariableWrite<T>> writes = variable.getWrites();

        if (writes.isEmpty())
            return Optional.empty();

        return Optional.of(writes.get(writes.size() - 1));
    }

    public <T> Optional<VariableWrite<T>> getLastWriteOf(@NonNull String variableName) {
        Optional<Variable<T>> variable = findVariableByName(variableName);

        if (!variable.isPresent())
            return Optional.empty();

        return getLastWriteOf(variable.get());
    }
}
