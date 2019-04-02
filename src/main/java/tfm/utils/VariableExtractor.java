package tfm.utils;

import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.variables.Variable;
import tfm.variables.actions.VariableAction.Actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VariableExtractor {

    public static class Result {

        public Map<String, List<Actions>> variableActions;

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
    }

    private VariableExtractor() {
    }

    public static Result parse(Expression expression) {
        VariableVisitor variableVisitor = new VariableVisitor();
        expression.accept(variableVisitor, Actions.UNKNOWN);

        return variableVisitor.result;
    }

    private static class VariableVisitor extends VoidVisitorAdapter<Actions> {

        private Result result;

        private VariableVisitor() {
            result = new Result();
        }

        @Override
        public void visit(ArrayAccessExpr n, Actions action) {
            System.out.println("On ArrayAccessExpr: [" + n + "]");
            n.getName().accept(this, action.or(Actions.READ));
            n.getIndex().accept(this, action.or(Actions.READ));
        }

        @Override
        public void visit(AssignExpr n, Actions action) {
            System.out.println("On AssignExpr: [" + n + "]");
            n.getTarget().accept(this, action.or(Actions.WRITE));
            n.getValue().accept(this, action.or(Actions.READ));
        }

        @Override
        public void visit(BinaryExpr n, Actions action) {
            System.out.println("On BinaryExpr: [" + n + "]");
            n.getLeft().accept(this, action.or(Actions.READ));
            n.getRight().accept(this, action.or(Actions.READ));
        }

        @Override
        public void visit(CastExpr n, Actions action) {
            System.out.println("On CastExpr: [" + n + "]");
            n.getExpression().accept(this, action.or(Actions.READ));
        }

        @Override
        public void visit(ConditionalExpr n, Actions action) {
            System.out.println("On CondtionalExpr: [" + n + "]");
            n.getCondition().accept(this, action.or(Actions.READ));
            n.getThenExpr().accept(this, action.or(Actions.READ));
            n.getElseExpr().accept(this, action.or(Actions.READ));
        }

        @Override
        public void visit(EnclosedExpr n, Actions action) {
            System.out.println("On EnclosedExpr: [" + n + "]");
            n.getInner().accept(this, action.or(Actions.READ));
        }

        @Override
        public void visit(FieldAccessExpr n, Actions action) {
//            System.out.println("On FieldAccessExpr: [" + n + "]");
//            n.getScope().accept(this, action.or(Actions.READ)); todo: accessing a field of a variable is a READ??
        }

//        @Override
//        public void visit(InstanceOfExpr n, Actions action) {
//            ??
//        }

        // ???
        @Override
        public void visit(MethodCallExpr n, Actions action) {
            System.out.println("On MethodCallExpr: [" + n + "]");
//            n.getScope().ifPresent(expression -> expression.accept(this, action.or(Actions.READ))); todo: accessing a field of a variable is a READ??
            n.getArguments().forEach(expression -> expression.accept(this, action.or(Actions.READ)));
        }

        @Override
        public void visit(NameExpr n, Actions action) {
            System.out.println("On NameExpr. Found variable " + n.getNameAsString() + " and action " + action);
            result.addVariableAction(n.getNameAsString(), action);
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
            System.out.println("On UnaryExpr: [" + n + "]");
            n.getExpression().accept(this, action.or(Actions.READ));
            n.getExpression().accept(this, action.or(Actions.WRITE));
        }

        @Override
        public void visit(VariableDeclarationExpr n, Actions action) {
            System.out.println("On VariableDeclarationExpr: [" + n + "]");
            n.getVariables()
                    .forEach(variableDeclarator -> {
                        variableDeclarator.getNameAsExpression().accept(this, action.or(Actions.WRITE));
                        variableDeclarator.getInitializer()
                                .ifPresent(expression -> expression.accept(this, action.or(Actions.READ)));
                    });
        }

        @Override
        public void visit(SwitchExpr n, Actions action) {
            System.out.println("On SwitchExpr: [" + n + "]");
            n.getSelector().accept(this, action.or(Actions.READ));
        }
    }

}
