package tfm.variables;

import tfm.variables.actions.VariableDeclaration;
import tfm.variables.actions.VariableRead;
import tfm.variables.actions.VariableWrite;

import java.util.ArrayList;
import java.util.List;

public class Variable {
    private VariableDeclaration declaration;
    private String name;
//    private Type type;
    private List<VariableWrite> writes;
    private List<VariableRead> reads;

    Variable(String name, VariableDeclaration variableDeclaration) {
        this.declaration = variableDeclaration;
        this.name = name;
        this.writes = new ArrayList<>();
        this.reads = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    void addWrite(VariableWrite declaration) {
        this.writes.add(declaration);
    }

    void addRead(VariableRead uses) {
        this.reads.add(uses);
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
    public int hashCode() {
        return name.hashCode() + declaration.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Variable %s declared on vertex %s", name, declaration.getNode().getId());
    }

    public List<VariableWrite> getWrites() {
        return writes;
    }

    public List<VariableRead> getReads() {
        return reads;
    }

    public VariableDeclaration getDeclaration() {
        return declaration;
    }
}
