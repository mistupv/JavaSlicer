package tfm.variables;

import tfm.variables.actions.VariableDeclaration;
import tfm.variables.actions.VariableRead;
import tfm.variables.actions.VariableWrite;

import java.util.ArrayList;
import java.util.List;

public class Variable<T> {
    private VariableDeclaration<T> declaration;
    private String name;
    private List<VariableWrite<T>> writes;
    private List<VariableRead<T>> reads;

    public Variable(VariableDeclaration<T> variableDeclaration, String name) {
        this.declaration = variableDeclaration;
        this.name = name;
        this.writes = new ArrayList<>();
        this.reads = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void addWrite(VariableWrite<T> declaration) {
        this.writes.add(declaration);
    }

    public void addWrites(List<VariableWrite<T>> declarations) {
        this.writes.addAll(declarations);
    }

    public void addRead(VariableRead<T> uses) {
        this.reads.add(uses);
    }

    public void addReads(List<VariableRead<T>> uses) {
        this.reads.addAll(uses);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof Variable)) {
            return false;
        }

        Variable other = (Variable) o;

        return name.equals(other.name) && declaration.equals(other.declaration);
    }

    @Override
    public String toString() {
        return String.format("Variable %s declared on vertex %s", name, declaration.getNode().getId());
    }

    public List<VariableWrite<T>> getWrites() {
        return writes;
    }

    public List<VariableRead<T>> getReads() {
        return reads;
    }

    public VariableDeclaration<T> getDeclaration() {
        return declaration;
    }
}
