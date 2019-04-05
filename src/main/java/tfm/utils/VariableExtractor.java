package tfm.utils;

import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.jetbrains.annotations.NotNull;
import tfm.variables.actions.VariableAction.Actions;

import java.util.*;
import java.util.function.BiConsumer;

public class VariableExtractor {

    public static class Result implements Iterable<Map.Entry<String, List<Actions>>> {

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

        public Set<String> variableNames() {
            return variableActions.keySet();
        }

        public Collection<List<Actions>> variableActions() {
            return variableActions.values();
        }

        public void forEach(BiConsumer<String, List<Actions>> action) {
            variableActions.forEach(action);
        }

        @NotNull
        @Override
        public Iterator<Map.Entry<String, List<Actions>>> iterator() {
            return variableActions.entrySet().iterator();
        }
    }

    private VariableExtractor() {
    }

    public static Result extractFrom(Expression expression) {
        VariableVisitor variableVisitor = new VariableVisitor();
        expression.accept(variableVisitor, Actions.USE);

        return variableVisitor.result;
    }

    private static class VariableVisitor extends VoidVisitorAdapter<Actions> {

        private Result result;

        private VariableVisitor() {
            this.result = new Result();
        }

        @Override
        public void visit(ArrayAccessExpr n, Actions action) {
            Logger.log("On ArrayAccessExpr: [" + n + "]");
            n.getName().accept(this, action.or(Actions.USE));
            n.getIndex().accept(this, action.or(Actions.USE));
        }

        @Override
        public void visit(AssignExpr n, Actions action) {
            Logger.log("On AssignExpr: [" + n + "]");
            n.getTarget().accept(this, action.or(Actions.DEFINITION));
            n.getValue().accept(this, action.or(Actions.USE));
        }

        @Override
        public void visit(BinaryExpr n, Actions action) {
            Logger.log("On BinaryExpr: [" + n + "]");
            n.getLeft().accept(this, action.or(Actions.USE));
            n.getRight().accept(this, action.or(Actions.USE));
        }

        @Override
        public void visit(CastExpr n, Actions action) {
            Logger.log("On CastExpr: [" + n + "]");
            n.getExpression().accept(this, action.or(Actions.USE));
        }

        @Override
        public void visit(ConditionalExpr n, Actions action) {
            Logger.log("On ConditionalExpr: [" + n + "]");
            n.getCondition().accept(this, action.or(Actions.USE));
            n.getThenExpr().accept(this, action.or(Actions.USE));
            n.getElseExpr().accept(this, action.or(Actions.USE));
        }

        @Override
        public void visit(EnclosedExpr n, Actions action) {
            Logger.log("On EnclosedExpr: [" + n + "]");
            n.getInner().accept(this, action.or(Actions.USE));
        }

        @Override
        public void visit(FieldAccessExpr n, Actions action) {
            Logger.log("On FieldAccessExpr: [" + n + "]");
            n.getScope().accept(this, action.or(Actions.USE));
        }

//        @Override
//        public void visit(InstanceOfExpr n, Actions action) {
//            ??
//        }

        // ???
        @Override
        public void visit(MethodCallExpr n, Actions action) {
            Logger.log("On MethodCallExpr: [" + n + "]");
            n.getScope().ifPresent(expression -> expression.accept(this, action.or(Actions.USE)));
            n.getArguments().forEach(expression -> expression.accept(this, action.or(Actions.USE)));
        }

        @Override
        public void visit(NameExpr n, Actions action) {
            Logger.log("On NameExpr. Found variable " + n.getNameAsString() + " and action " + action);

            String variableName = n.getNameAsString();

            result.addVariableAction(variableName, action);
        }

//        @Override
//        public void visit(NormalAnnotationExpr n, Actions action) {
//            ??
//        }

//        @Override
//        public void visit(SingleMemberAnnotationExpr n, Actions action) {
//            ??
//        }

        @Override
        public void visit(UnaryExpr n, Actions action) {
            Logger.log("On UnaryExpr: [" + n + "]");
            n.getExpression().accept(this, action.or(Actions.USE));
            n.getExpression().accept(this, action.or(Actions.DEFINITION));
        }

        @Override
        public void visit(VariableDeclarationExpr n, Actions action) {
            Logger.log("On VariableDeclarationExpr: [" + n + "]");
            n.getVariables()
                    .forEach(variableDeclarator -> {
                        variableDeclarator.getNameAsExpression().accept(this, action.or(Actions.DECLARATION)); // Declaration of the variable
                        variableDeclarator.getInitializer()
                                .ifPresent(expression -> {
                                    variableDeclarator.getNameAsExpression().accept(this, action.or(Actions.DEFINITION)); // Definition of the variable (it is initialized)
                                    expression.accept(this, action.or(Actions.USE)); // Use of the variables in the initialization
                                });
                    });
        }

        @Override
        public void visit(SwitchExpr n, Actions action) {
            Logger.log("On SwitchExpr: [" + n + "]");
            n.getSelector().accept(this, action.or(Actions.USE));
        }
    }

}
