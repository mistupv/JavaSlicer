package tfm.scopes;

import tfm.nodes.GraphNode;
import tfm.variables.actions.VariableDefinition;

import java.util.List;
import java.util.stream.Collectors;

public class IfElseScope<N extends GraphNode> extends ScopeHolder<N> {

    private VariableScope<N> expressionScope;
    private ScopeHolder<N> thenScope;
    private ScopeHolder<N> elseScope;

    public IfElseScope(N node) {
        super(node);

        this.expressionScope = new VariableScope<>(node);
        this.thenScope = new ScopeHolder<>(node);
        this.elseScope = new ScopeHolder<>(node);

        addSubscope(expressionScope); // expression
        addSubscope(thenScope); // then
        addSubscope(elseScope); // else
    }

    public VariableScope<N> getExpressionScope() {
        return expressionScope;
    }

    public ScopeHolder<N> getThenScope() {
        return thenScope;
    }

    public ScopeHolder<N> getElseScope() {
        return elseScope;
    }

    @Override
    public void addVariableUse(String variable, N node) {
        getExpressionScope().addVariableUse(variable, node);
    }

    @Override
    public List<VariableDefinition<N>> getFirstDefinitions(String variable) {
        return getSubscopes().stream()
                .flatMap(scope -> scope.getFirstDefinitions(variable).stream())
                .collect(Collectors.toList());
    }

    @Override
    public List<VariableDefinition<N>> getLastDefinitions(String variable) {
        return getSubscopes().stream()
                .flatMap(scope -> scope.getLastDefinitions(variable).stream())
                .collect(Collectors.toList());
    }

    @Override
    public List<VariableDefinition<N>> getLastDefinitionsBeforeNode(String variable, N node) {
        return getSubscopes().stream()
                .flatMap(scope -> scope.getLastDefinitionsBeforeNode(variable, node).stream())
                .collect(Collectors.toList());
    }
}
