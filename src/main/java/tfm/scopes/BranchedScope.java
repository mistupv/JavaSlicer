package tfm.scopes;

import tfm.nodes.Node;
import tfm.variables.actions.VariableDefinition;

import java.util.List;
import java.util.stream.Collectors;

public class BranchedScope<N extends Node> extends ScopeHolder<N> {

    public BranchedScope(N node) {
        super(node);
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
