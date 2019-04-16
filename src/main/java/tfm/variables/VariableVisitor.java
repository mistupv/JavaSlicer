package tfm.variables;

import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.checkerframework.checker.nullness.qual.NonNull;
import tfm.utils.Logger;
import tfm.variables.actions.VariableAction;

abstract class VariableVisitor extends VoidVisitorAdapter<VariableAction.Actions> {

    // Start point
    public void visit(@NonNull Expression expression) {
        expression.accept(this, VariableAction.Actions.USE);
    }

//    public void visit(@NonNull Statement statement) {
//        statement.accept(this, Actions.USE);
//    }

    @Override
    public void visit(ArrayAccessExpr n, VariableAction.Actions action) {
        // Logger.log("On ArrayAccessExpr: [" + n + "]");
        n.getName().accept(this, action.or(VariableAction.Actions.USE));
        n.getIndex().accept(this, action.or(VariableAction.Actions.USE));
    }

    @Override
    public void visit(AssignExpr n, VariableAction.Actions action) {
        // Logger.log("On AssignExpr: [" + n + "]");
        n.getTarget().accept(this, action.or(VariableAction.Actions.DEFINITION));
        n.getValue().accept(this, action.or(VariableAction.Actions.USE));
    }

    @Override
    public void visit(BinaryExpr n, VariableAction.Actions action) {
        // Logger.log("On BinaryExpr: [" + n + "]");
        n.getLeft().accept(this, action.or(VariableAction.Actions.USE));
        n.getRight().accept(this, action.or(VariableAction.Actions.USE));
    }

    @Override
    public void visit(CastExpr n, VariableAction.Actions action) {
        // Logger.log("On CastExpr: [" + n + "]");
        n.getExpression().accept(this, action.or(VariableAction.Actions.USE));
    }

    @Override
    public void visit(ConditionalExpr n, VariableAction.Actions action) {
        // Logger.log("On ConditionalExpr: [" + n + "]");
        n.getCondition().accept(this, action.or(VariableAction.Actions.USE));
        n.getThenExpr().accept(this, action.or(VariableAction.Actions.USE));
        n.getElseExpr().accept(this, action.or(VariableAction.Actions.USE));
    }

    @Override
    public void visit(EnclosedExpr n, VariableAction.Actions action) {
        // Logger.log("On EnclosedExpr: [" + n + "]");
        n.getInner().accept(this, action.or(VariableAction.Actions.USE));
    }

    @Override
    public void visit(FieldAccessExpr n, VariableAction.Actions action) {
        // Logger.log("On FieldAccessExpr: [" + n + "]");
        n.getScope().accept(this, action.or(VariableAction.Actions.USE));
    }

//        @Override
//        public void visit(InstanceOfExpr n, Actions action) {
//            ??
//        }

    // ???
    @Override
    public void visit(MethodCallExpr n, VariableAction.Actions action) {
        // Logger.log("On MethodCallExpr: [" + n + "]");
        n.getScope().ifPresent(expression -> expression.accept(this, action.or(VariableAction.Actions.USE)));
        n.getArguments().forEach(expression -> expression.accept(this, action.or(VariableAction.Actions.USE)));
    }

    @Override
    public void visit(NameExpr n, VariableAction.Actions action) {
        // Logger.log("On NameExpr. Found variable " + n.getNameAsString() + " and action " + action);

        String variable = n.getNameAsString();

        switch (action) {
            case DECLARATION:
                onVariableDeclaration(variable);
                break;
            case DEFINITION:
                onVariableDefinition(variable);
                break;
            default:
                onVariableUse(variable);
                break;
        }

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
    public void visit(UnaryExpr n, VariableAction.Actions action) {
        // Logger.log("On UnaryExpr: [" + n + "]");
        n.getExpression().accept(this, action.or(VariableAction.Actions.USE));
        n.getExpression().accept(this, action.or(VariableAction.Actions.DEFINITION));
    }

    @Override
    public void visit(VariableDeclarationExpr n, VariableAction.Actions action) {
        // Logger.log("On VariableDeclarationExpr: [" + n + "]");
        n.getVariables()
                .forEach(variableDeclarator -> {
                    variableDeclarator.getNameAsExpression().accept(this, action.or(VariableAction.Actions.DECLARATION)); // Declaration of the variable
                    variableDeclarator.getInitializer()
                            .ifPresent(expression -> {
                                variableDeclarator.getNameAsExpression().accept(this, action.or(VariableAction.Actions.DEFINITION)); // Definition of the variable (it is initialized)
                                expression.accept(this, action.or(VariableAction.Actions.USE)); // Use of the variables in the initialization
                            });
                });
    }

    @Override
    public void visit(SwitchExpr n, VariableAction.Actions action) {
        // Logger.log("On SwitchExpr: [" + n + "]");
        n.getSelector().accept(this, action.or(VariableAction.Actions.USE));
    }

    abstract void onVariableUse(@NonNull String variable);
    abstract void onVariableDefinition(@NonNull String variable);
    abstract void onVariableDeclaration(@NonNull String variable);
}
