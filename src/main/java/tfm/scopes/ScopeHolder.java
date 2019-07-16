package tfm.scopes;

import tfm.nodes.Node;
import tfm.variables.actions.VariableDeclaration;
import tfm.variables.actions.VariableDefinition;
import tfm.variables.actions.VariableUse;

import java.util.*;
import java.util.stream.Collectors;

public class ScopeHolder<N extends Node> extends Scope<N> {

    private Queue<Scope<N>> subscopes;

    public ScopeHolder(N root) {
        super(root);

        subscopes = Collections.asLifoQueue(new ArrayDeque<>());
    }

    private Optional<Scope<N>> getLastScope() {
        if (subscopes.isEmpty())
            return Optional.empty();

        return Optional.of(subscopes.peek());
    }

    @Override
    public void addVariableDeclaration(String variable, N context) {
        Optional<Scope<N>> optionalScope = getLastScope();

        boolean newScope = !optionalScope.isPresent();

        Scope<N> scope = optionalScope.orElse(new VariableScope<>(context));

        scope.addVariableDeclaration(variable, context);

        if (newScope) {
            addSubscope(scope);
        }
    }

    @Override
    public void addVariableDefinition(String variable, N context) {
        Optional<Scope<N>> optionalScope = getLastScope();

        boolean newScope = !optionalScope.isPresent();

        Scope<N> scope = optionalScope.orElse(new VariableScope<>(context));

        scope.addVariableDefinition(variable, context);

        if (newScope) {
            addSubscope(scope);
        }
    }

    @Override
    public void addVariableUse(String variable, N context) {
        Optional<Scope<N>> optionalScope = getLastScope();

        boolean newScope = !optionalScope.isPresent();

        Scope<N> scope = optionalScope.orElse(new VariableScope<>(context));

        scope.addVariableUse(variable, context);

        if (newScope) {
            addSubscope(scope);
        }
    }

    public Queue<Scope<N>> getSubscopes() {
        return subscopes;
    }

    public void addSubscope(Scope<N> subscope) {
        subscopes.add(subscope);
    }

    @Override
    public boolean isVariableDeclared(String variable) {
        return subscopes.stream().anyMatch(subscope -> subscope.isVariableDeclared(variable));
    }

    @Override
    public boolean isVariableDefined(String variable) {
        return subscopes.stream().anyMatch(subscope -> subscope.isVariableDefined(variable));
    }

    @Override
    public boolean isVariableUsed(String variable) {
        return subscopes.stream().anyMatch(subscope -> subscope.isVariableUsed(variable));
    }

    @Override
    public List<VariableDeclaration<N>> getVariableDeclarations(String variable) {
        return subscopes.stream()
                .flatMap(scope -> scope.getVariableDeclarations(variable).stream())
                .collect(Collectors.toList());
    }

    @Override
    public List<VariableDefinition<N>> getVariableDefinitions(String variable) {
        return subscopes.stream()
                .flatMap(scope -> scope.getVariableDefinitions(variable).stream())
                .collect(Collectors.toList());
    }

    @Override
    public List<VariableUse<N>> getVariableUses(String variable) {
        return subscopes.stream()
                .flatMap(scope -> scope.getVariableUses(variable).stream())
                .collect(Collectors.toList());
    }

    @Override
    public Set<String> getDeclaredVariables() {
        return subscopes.stream()
                .flatMap(scope -> scope.getDeclaredVariables().stream())
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getDefinedVariables() {
        return subscopes.stream()
                .flatMap(scope -> scope.getDefinedVariables().stream())
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getUsedVariables() {
        return subscopes.stream()
                .flatMap(scope -> scope.getUsedVariables().stream())
                .collect(Collectors.toSet());
    }

    @Override
    public List<VariableUse<N>> getVariableUsesBeforeNode(String variable, N node) {
        return subscopes.stream()
                .filter(_scope -> _scope.isVariableUsed(variable) && _scope.root.getId() <= node.getId())
                .flatMap(scope -> scope.getVariableUsesBeforeNode(variable, node).stream())
                .collect(Collectors.toList());
    }

    @Override
    public List<VariableDefinition<N>> getFirstDefinitions(String variable) {
        Optional<Scope<N>> scope = subscopes.stream()
                .filter(_scope -> _scope.isVariableDefined(variable))
                .reduce((first, second) -> second);

        if (!scope.isPresent())
            return new ArrayList<>(0);

        return scope.get().getFirstDefinitions(variable);
    }

    @Override
    public List<VariableDefinition<N>> getLastDefinitions(String variable) {
        Optional<Scope<N>> scope = subscopes.stream()
                .filter(_scope -> _scope.isVariableDefined(variable))
                .findFirst();

        if (!scope.isPresent())
            return new ArrayList<>(0);

        return scope.get().getLastDefinitions(variable);
    }

    @Override
    public List<VariableDefinition<N>> getLastDefinitionsBeforeNode(String variable, N node) {
        Optional<Scope<N>> scope = subscopes.stream()
                .filter(_scope -> _scope.isVariableDefined(variable) && _scope.root.getId() <= node.getId())
                .findFirst();

        if (!scope.isPresent())
            return new ArrayList<>(0);

        return scope.get().getLastDefinitions(variable);
    }


}
