package tfm.nodes;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.Set;
import java.util.function.Consumer;

public class VariableVisitor extends VoidVisitorAdapter<VariableVisitor.Action> {
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

    protected final Consumer<NameExpr> onUse;
    protected final Consumer<NameExpr> onDef;
    protected final Consumer<NameExpr> onDecl;

    public VariableVisitor(Consumer<NameExpr> onUse, Consumer<NameExpr> onDef, Consumer<NameExpr> onDecl) {
        this.onUse = onUse;
        this.onDecl = onDecl;
        this.onDef = onDef;
    }

    public void search(Node node) {
        node.accept(this, Action.USE);
    }

    @Override
    public void visit(NameExpr n, Action action) {
        switch (action) {
            case DECLARATION:
                onDecl.accept(n);
                break;
            case DEFINITION:
                onDef.accept(n);
                break;
            case USE:
                onUse.accept(n);
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    // Transparently traversed (they contain no statements):
    // AssertStmt, BreakStmt, ContinueStmt, EmptyStmt, ExplicitConstructorInvocationStmt,
    // ReturnStmt, ExpressionStmt, LocalClassDeclarationStmt
    // Transparently traversed (they contain only USE expressions):
    // ArrayAccessExpr, BinaryExpr, ConditionalExpr, EnclosedExpr

    // Not traversed at all (they only contain statements)
    @Override
    public void visit(BlockStmt n, Action arg) {
    }

    @Override
    public void visit(TryStmt n, Action arg) {
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

    @Override
    public void visit(DoStmt n, Action arg) {
        n.getCondition().accept(this, Action.USE);
    }

    @Override
    public void visit(ForStmt n, Action arg) {
        n.getCompare().ifPresent(expression -> expression.accept(this, Action.USE));
    }

    @Override
    public void visit(WhileStmt n, Action arg) {
        n.getCondition().accept(this, Action.USE);
    }

    @Override
    public void visit(IfStmt n, Action arg) {
        n.getCondition().accept(this, Action.USE);
    }

    @Override
    public void visit(SwitchEntryStmt n, Action arg) {
        n.getLabel().ifPresent(expression -> expression.accept(this, Action.USE));
    }

    @Override
    public void visit(SwitchStmt n, Action arg) {
        n.getSelector().accept(this, Action.USE);
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
