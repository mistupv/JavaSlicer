package tfm.scopes;

import tfm.nodes.Node;
import tfm.variables.actions.VariableDeclaration;
import tfm.variables.actions.VariableDefinition;
import tfm.variables.actions.VariableUse;

import java.util.List;
import java.util.Set;

public abstract class Scope<N extends Node> {

    protected N root;

    protected Scope(N root) {
        this.root = root;
    }

    public N getRoot() {
        return root;
    }

    public abstract void addVariableDeclaration(String variable, N context);
    public abstract void addVariableDefinition(String variable, N context);
    public abstract void addVariableUse(String variable, N context);

    public abstract boolean isVariableDeclared(String variable);
    public abstract boolean isVariableDefined(String variable);
    public abstract boolean isVariableUsed(String variable);

    public abstract List<VariableDeclaration<N>> getVariableDeclarations(String variable);
    public abstract List<VariableDefinition<N>> getVariableDefinitions(String variable);
    public abstract List<VariableUse<N>> getVariableUses(String variable);

    public abstract Set<String> getDeclaredVariables();
    public abstract Set<String> getDefinedVariables();
    public abstract Set<String> getUsedVariables();


    public abstract List<VariableUse<N>> getVariableUsesBeforeNode(String variable, N node);

    public abstract List<VariableDefinition<N>> getFirstDefinitions(String variable);
    public abstract List<VariableDefinition<N>> getLastDefinitions(String variable);
    public abstract List<VariableDefinition<N>> getLastDefinitionsBeforeNode(String variable, N node);
}

