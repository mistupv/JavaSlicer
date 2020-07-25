package tfm.nodes;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ForEachStmt;
import tfm.graphs.GraphNodeContentVisitor;

import java.util.Set;

public class VariableVisitor extends GraphNodeContentVisitor<VariableVisitor.Action> {
    enum Action {
        DECLARATION,
        DEFINITION,
        USE;

        public Action or(Action action) {
            if (action == DECLARATION || this == DECLARATION)
                return DECLARATION;
            if (action == DEFINITION || this == DEFINITION)
                return DEFINITION;
            return USE;
        }
    }

    protected static final Set<UnaryExpr.Operator> PREFIX_CHANGE = Set.of(
            UnaryExpr.Operator.PREFIX_DECREMENT, UnaryExpr.Operator.PREFIX_INCREMENT);
    protected static final Set<UnaryExpr.Operator> POSTFIX_CHANGE = Set.of(
            UnaryExpr.Operator.POSTFIX_DECREMENT, UnaryExpr.Operator.POSTFIX_INCREMENT);

    @Override
    public void startVisit(GraphNode<?> node) {
        startVisit(node, Action.USE);
    }

    @Override
    public void visit(NameExpr n, Action action) {
        switch (action) {
            case DECLARATION:
                graphNode.addDeclaredVariable(n);
                break;
            case DEFINITION:
                graphNode.addDefinedVariable(n);
                break;
            case USE:
                graphNode.addUsedVariable(n);
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    // Partially traversed (only expressions that may contain variables are traversed)
    @Override
    public void visit(CastExpr n, Action action) {
        n.getExpression().accept(this, action);
    }

    @Override
    public void visit(FieldAccessExpr n, Action action) {
        n.getScope().accept(this, action);
    }

    // Modified traversal (there may be variable definitions or declarations)
    @Override
    public void visit(ForEachStmt n, Action action) {
        n.getIterable().accept(this, Action.USE);
        for (VariableDeclarator variable : n.getVariable().getVariables()) {
            variable.getNameAsExpression().accept(this, Action.DECLARATION);
            variable.getNameAsExpression().accept(this, Action.DEFINITION);
        }
    }

    @Override
    public void visit(AssignExpr n, Action action) {
        // Target will be used if operator is not '='
        if (n.getOperator() != AssignExpr.Operator.ASSIGN)
            n.getTarget().accept(this, action);
        n.getTarget().accept(this, action.or(Action.DEFINITION));
        n.getValue().accept(this, action);
    }

    @Override
    public void visit(UnaryExpr n, Action action) {
        // ++a -> USAGE (get value), DEFINITION (add 1), USAGE (get new value)
        // a++ -> USAGE (get value), DEFINITION (add 1)
        // any other UnaryExpr (~, !, -) -> USAGE
        if (PREFIX_CHANGE.contains(n.getOperator())) {
            n.getExpression().accept(this, action);
            n.getExpression().accept(this, action.or(Action.DEFINITION));
        }
        n.getExpression().accept(this, action);
        if (POSTFIX_CHANGE.contains(n.getOperator()))
            n.getExpression().accept(this, action.or(Action.DEFINITION));
    }

    @Override
    public void visit(VariableDeclarationExpr n, Action action) {
        for (VariableDeclarator v : n.getVariables()) {
            v.getNameAsExpression().accept(this, action.or(Action.DECLARATION));
            if (v.getInitializer().isPresent()) {
                v.getNameAsExpression().accept(this, Action.DEFINITION);
                v.getInitializer().get().accept(this, action);
            }
        }
    }

    @Override
    public void visit(MethodCallExpr n, Action action) {
        n.getScope().ifPresent(expression -> expression.accept(this, action));
        for (Expression e : n.getArguments()) {
            e.accept(this, action);
            if (e.isNameExpr() || e.isFieldAccessExpr())
                e.accept(this, action.or(Action.DEFINITION));
        }
    }
}
