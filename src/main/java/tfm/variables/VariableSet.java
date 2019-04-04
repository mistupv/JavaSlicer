package tfm.variables;

import org.checkerframework.checker.nullness.qual.NonNull;
import tfm.variables.actions.VariableDeclaration;
import tfm.variables.actions.VariableRead;
import tfm.variables.actions.VariableWrite;

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

    public  Optional<VariableWrite> getLastWriteOf(@NonNull String variableName) {
        Optional<Variable> variable = findVariableByName(variableName);

        if (!variable.isPresent())
            return Optional.empty();

        List<VariableWrite> writes = variable.get().getWrites();

        if (writes.isEmpty())
            return Optional.empty();

        return Optional.of(writes.get(writes.size() - 1));
    }

    public void addRead(String variableName, VariableRead variableRead) {
        findVariableByName(variableName)
                .ifPresent(variable -> variable.addRead(variableRead));
    }

    public void addWrite(String variableName, VariableWrite variableWrite) {
        findVariableByName(variableName)
                .ifPresent(variable -> variable.addWrite(variableWrite));
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
}
