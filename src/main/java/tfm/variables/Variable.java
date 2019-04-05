package tfm.variables;

import tfm.variables.actions.VariableDeclaration;
import tfm.variables.actions.VariableUse;
import tfm.variables.actions.VariableDefinition;

import java.util.ArrayList;
import java.util.List;

public class Variable {
    private VariableDeclaration declaration; // The declaration holds the scope, so its part of the id of the variable
    private String name; // In addition to the name
//    private Type type;
    private List<VariableDefinition> definitions;
    private List<VariableUse> uses;

    Variable(String name, VariableDeclaration variableDeclaration) {
        this.declaration = variableDeclaration;
        this.name = name;
        this.definitions = new ArrayList<>();
        this.uses = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    void addDefinition(VariableDefinition declaration) {
        this.definitions.add(declaration);
    }

    void addUse(VariableUse uses) {
        this.uses.add(uses);
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

    public List<VariableDefinition> getDefinitions() {
        return definitions;
    }

    public List<VariableUse> getUses() {
        return uses;
    }

    public VariableDeclaration getDeclaration() {
        return declaration;
    }
}
