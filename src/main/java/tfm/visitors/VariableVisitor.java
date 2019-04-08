package tfm.visitors;

import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.checkerframework.checker.nullness.qual.NonNull;
import tfm.utils.Logger;

import java.util.*;

import static tfm.variables.actions.VariableAction.Actions;

public class VariableVisitor extends VoidVisitorAdapter<Actions> {

    private OnVariableDeclarationListener onVariableDeclarationListener;
    private OnVariableDefinitionListener onVariableDefinitionListener;
    private OnVariableUseListener onVariableUseListener;

    public VariableVisitor() {
    }

    public VariableVisitor setOnVariableDeclaration(@NonNull OnVariableDeclarationListener listener) {
        this.onVariableDeclarationListener = listener;
        return this;
    }

    public VariableVisitor setOnVariableDefinition(@NonNull OnVariableDefinitionListener listener) {
        this.onVariableDefinitionListener = listener;
        return this;
    }

    public VariableVisitor setOnVariableUse(@NonNull OnVariableUseListener listener) {
        this.onVariableUseListener = listener;
        return this;
    }

    // Start point
    public void visit(@NonNull Expression expression) {
        expression.accept(this, Actions.USE);
    }

    public void visit(@NonNull Statement statement) {
        statement.accept(this, Actions.USE);
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

        String variable = n.getNameAsString();

        switch (action) {
            case DECLARATION:
                if (this.onVariableDeclarationListener != null)
                    this.onVariableDeclarationListener.onVariableDeclaration(variable);

                break;
            case DEFINITION:
                if (this.onVariableDefinitionListener != null)
                    this.onVariableDefinitionListener.onVariableDefinition(variable);

                break;
            default:
                if (this.onVariableUseListener != null)
                    this.onVariableUseListener.onVariableUse(variable);

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

    @FunctionalInterface
    public interface OnVariableDeclarationListener {
        void onVariableDeclaration(String variable);
    }

    @FunctionalInterface
    public interface OnVariableDefinitionListener {
        void onVariableDefinition(String variable);
    }

    @FunctionalInterface
    public interface OnVariableUseListener {
        void onVariableUse(String variable);
    }
}
