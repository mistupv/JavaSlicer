package tfm.utils;

import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.visitor.GenericVisitorAdapter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.jetbrains.annotations.NotNull;
import tfm.variables.Variable;
import tfm.variables.VariableSet;
import tfm.variables.actions.VariableAction.Actions;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

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
        expression.accept(variableVisitor, Actions.READ);

        return variableVisitor.result;
    }

    private static class VariableVisitor extends VoidVisitorAdapter<Actions> {

        private Result result;

        private VariableVisitor() {
            this.result = new Result();
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
            System.out.println("On ConditionalExpr: [" + n + "]");
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
            System.out.println("On FieldAccessExpr: [" + n + "]");
            n.getScope().accept(this, action.or(Actions.READ));
        }

//        @Override
//        public void visit(InstanceOfExpr n, Actions action) {
//            ??
//        }

        // ???
        @Override
        public void visit(MethodCallExpr n, Actions action) {
            System.out.println("On MethodCallExpr: [" + n + "]");
            n.getScope().ifPresent(expression -> expression.accept(this, action.or(Actions.READ)));
            n.getArguments().forEach(expression -> expression.accept(this, action.or(Actions.READ)));
        }

        @Override
        public void visit(NameExpr n, Actions action) {
            System.out.println("On NameExpr. Found variable " + n.getNameAsString() + " and action " + action);

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
            System.out.println("On UnaryExpr: [" + n + "]");
            n.getExpression().accept(this, action.or(Actions.READ));
            n.getExpression().accept(this, action.or(Actions.WRITE));
        }

        @Override
        public void visit(VariableDeclarationExpr n, Actions action) {
            System.out.println("On VariableDeclarationExpr: [" + n + "]");
            n.getVariables()
                    .forEach(variableDeclarator -> {
                        variableDeclarator.getNameAsExpression().accept(this, action.or(Actions.DECLARE));
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
