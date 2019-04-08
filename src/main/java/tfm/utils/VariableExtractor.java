package tfm.utils;

import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.jetbrains.annotations.NotNull;
import tfm.variables.actions.VariableAction.Actions;

import java.util.*;
import java.util.function.BiConsumer;

public class VariableExtractor {

    public static class Result {

        private Map<String, List<Actions>> variableActions;

        public Result() {
            variableActions = new HashMap<>();
        }

        private void addVariableAction(String variable, Actions action) {
            List<Actions> actions = variableActions.getOrDefault(variable, new ArrayList<>());

            actions.add(action);

            if (!variableActions.containsKey(variable)) {
                variableActions.put(variable, actions);
            }
        }

        public Map<String, List<Actions>> getVariableActions() {
            return variableActions;
        }

        public Set<String> variableNames() {
            return variableActions.keySet();
        }

        public boolean containsVariable(String variable) {
            return variableActions.containsKey(variable);
        }

        public List<Actions> getActionsForVariable(String variable) {
            return variableActions.getOrDefault(variable, new ArrayList<>());
        }

        public void forEach(BiConsumer<String, List<Actions>> action) {
            variableActions.forEach(action);
        }
    }


}
