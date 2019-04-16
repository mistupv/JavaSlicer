package tfm.scopes;

import tfm.nodes.Node;
import tfm.variables.actions.VariableAction;
import tfm.variables.actions.VariableDeclaration;
import tfm.variables.actions.VariableDefinition;
import tfm.variables.actions.VariableUse;

import java.util.*;
import java.util.stream.Collectors;

public class VariableScope<N extends Node> extends Scope<N> {

    private Map<String, List<VariableDeclaration<N>>> variableDeclarations;
    private Map<String, List<VariableDefinition<N>>> variableDefinitions;
    private Map<String, List<VariableUse<N>>> variableUses;

    public VariableScope(N root) {
        super(root);

        variableDeclarations = new HashMap<>();
        variableDefinitions = new HashMap<>();
        variableUses = new HashMap<>();
    }

    @Override
    public boolean isVariableDeclared(String variable) {
        return variableDeclarations.containsKey(variable);
    }

    @Override
    public boolean isVariableDefined(String variable) {
        return variableDefinitions.containsKey(variable);
    }

    @Override
    public boolean isVariableUsed(String variable) {
        return variableUses.containsKey(variable);
    }

    @Override
    public List<VariableDeclaration<N>> getVariableDeclarations(String variable) {
        return new ArrayList<>(variableDeclarations.getOrDefault(variable, new ArrayList<>()));
    }

    @Override
    public List<VariableDefinition<N>> getVariableDefinitions(String variable) {
        return new ArrayList<>(variableDefinitions.getOrDefault(variable, new ArrayList<>()));
    }

    @Override
    public List<VariableUse<N>> getVariableUses(String variable) {
        return new ArrayList<>(variableUses.getOrDefault(variable, new ArrayList<>()));
    }

    @Override
    public Set<String> getDeclaredVariables() {
        return new HashSet<>(variableDeclarations.keySet());
    }

    @Override
    public Set<String> getDefinedVariables() {
        return new HashSet<>(variableDefinitions.keySet());
    }

    @Override
    public Set<String> getUsedVariables() {
        return new HashSet<>(variableUses.keySet());
    }

    @Override
    public List<VariableDefinition<N>> getLastDefinitions(String variable) {
        List<VariableDefinition<N>> res = getVariableDefinitions(variable);

        if (res.isEmpty())
            return res;

        return res.subList(res.size() - 1, res.size());
    }

    @Override
    public List<VariableDefinition<N>> getLastDefinitionsBeforeNode(String variable, N node) {
        List<VariableDefinition<N>> res = getVariableDefinitions(variable);

        if (res.isEmpty())
            return res;

        Optional<VariableDefinition<N>> target = res.stream()
                .filter(variableDefinition -> variableDefinition.getNode().getId() <= node.getId())
                .max(Comparator.comparingInt(variableDefinition -> variableDefinition.getNode().getId()));

        return target.map(variableDefinition -> new ArrayList<>(Collections.singletonList(variableDefinition)))
                .orElseGet(ArrayList::new);
    }

    @Override
    public void addVariableDeclaration(String variable, N context) {
        appendValue(variableDeclarations, variable, new VariableDeclaration<>(variable, context));
    }

    @Override
    public void addVariableDefinition(String variable, N context) {
        appendValue(variableDefinitions, variable, new VariableDefinition<>(variable, context));
    }

    @Override
    public void addVariableUse(String variable, N context) {
        appendValue(variableUses, variable, new VariableUse<>(variable, context));
    }

    private <E extends VariableAction<N>> void appendValue(Map<String, List<E>> map, String variable, E action) {
        List<E> value = map.getOrDefault(variable, new ArrayList<>());

        boolean exists = !value.isEmpty();

        value.add(action);

        if (!exists) {
            map.put(variable, value);
        }
    }

    private <E extends VariableAction<N>> void appendValues(Map<String, List<E>> map, String variable, List<E> actions) {
        List<E> value = map.getOrDefault(variable, new ArrayList<>());

        boolean exists = !value.isEmpty();

        value.addAll(actions);

        if (!exists) {
            map.put(variable, value);
        }
    }
}
