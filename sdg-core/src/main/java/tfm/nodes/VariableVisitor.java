package tfm.nodes;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import tfm.graphs.GraphNodeContentVisitor;
import tfm.utils.ASTUtils;

import java.util.Objects;
import java.util.function.BiConsumer;

/** A graph node visitor that extracts the actions performed in a given GraphNode. An initial action mode can
 *  be set, to consider variables found a declaration, definition or usage (default).
 *  @see GraphNodeContentVisitor */
public class VariableVisitor extends GraphNodeContentVisitor<VariableVisitor.Action> {
    /** The default action that a found variable performs. */
    public enum Action {
        DECLARATION, DEFINITION, USE;

        /** Join multiple actions into one. */
        public Action or(Action action) {
            if (action == DECLARATION || this == DECLARATION)
                return DECLARATION;
            if (action == DEFINITION || this == DEFINITION)
                return DEFINITION;
            return USE;
        }
    }

    /** A default action to be used as a placeholder when a {@code null} action is received. */
    protected static final BiConsumer<NameExpr, GraphNode<?>> BLANK_CONSUMER = (a, b) -> {};

    /** The action to perform when a declaration is found. */
    protected final BiConsumer<NameExpr, GraphNode<?>> declConsumer;
    /** The action to perform when a definition is found. */
    protected final BiConsumer<NameExpr, GraphNode<?>> defConsumer;
    /** The action to perform when a usage is found. */
    protected final BiConsumer<NameExpr, GraphNode<?>> useConsumer;

    /** A variable visitor that will add each action to the list of actions of the graph node.
     *  The entry-point for this graph MUST be {@link #startVisit(GraphNode)} or {@link #startVisit(GraphNode, Action)} */
    public VariableVisitor() {
        this((name, node) -> node.addDeclaredVariable(name),
                (name, node) -> node.addDefinedVariable(name),
                (name, node) -> node.addUsedVariable(name));
    }

    /** A variable visitor that will perform the given actions when a variable is found.
     *  A node can accept this visitor, but calls will be ignored if the entry-point is
     *  not {@link #startVisit(GraphNode)} or {@link #startVisit(GraphNode, Action)} */
    public VariableVisitor(BiConsumer<NameExpr, GraphNode<?>> declConsumer, BiConsumer<NameExpr, GraphNode<?>> defConsumer, BiConsumer<NameExpr, GraphNode<?>> useConsumer) {
        this.declConsumer = Objects.requireNonNullElse(declConsumer, BLANK_CONSUMER);
        this.defConsumer = Objects.requireNonNullElse(defConsumer, BLANK_CONSUMER);
        this.useConsumer = Objects.requireNonNullElse(useConsumer, BLANK_CONSUMER);
    }

    @Override
    public void startVisit(GraphNode<?> node) {
        startVisit(node, Action.USE);
    }

    @Override
    public void visit(NameExpr n, Action action) {
        switch (action) {
            case DECLARATION:
                declConsumer.accept(n, graphNode);
                break;
            case DEFINITION:
                defConsumer.accept(n, graphNode);
                break;
            case USE:
                useConsumer.accept(n, graphNode);
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
        switch (n.getOperator()) {
            case PREFIX_DECREMENT:
            case PREFIX_INCREMENT:
                n.getExpression().accept(this, action);
                n.getExpression().accept(this, action.or(Action.DEFINITION));
                break;
        }
        n.getExpression().accept(this, action);
        switch (n.getOperator()) {
            case POSTFIX_INCREMENT:
            case POSTFIX_DECREMENT:
               n.getExpression().accept(this, action.or(Action.DEFINITION));
                break;
        }
    }

    @Override
    public void visit(VariableDeclarationExpr n, Action action) {
        for (VariableDeclarator v : n.getVariables()) {
            v.getNameAsExpression().accept(this, action.or(Action.DECLARATION));
            if (v.getInitializer().isPresent()) {
                v.getInitializer().get().accept(this, action);
                v.getNameAsExpression().accept(this, Action.DEFINITION);
            }
        }
    }

    // =======================================================================
    // ================================ CALLS ================================
    // =======================================================================

    // If the call will be linked (there is an AST node), skip parameters, add markers and add to node
    // TODO: non-linked calls should mark both parameters and scope as DEF+USE (problem: 'System.out.println(z)')

    @Override
    public void visit(ObjectCreationExpr n, Action arg) {
        if (!visitCall(n, arg))
            super.visit(n, arg);
    }

    @Override
    public void visit(ExplicitConstructorInvocationStmt n, Action arg) {
        if (!visitCall(n, arg))
            super.visit(n, arg);
    }

    @Override
    public void visit(MethodCallExpr n, Action arg) {
        if (!visitCall(n, arg))
            super.visit(n, arg);
    }

    protected boolean visitCall(Resolvable<? extends ResolvedMethodLikeDeclaration> call, Action arg) {
        if (ASTUtils.getResolvedAST(call.resolve()).isEmpty() || graphNode == null)
            return false;
        graphNode.addCallMarker(call, true);
        ASTUtils.getResolvableScope(call).ifPresent(s -> s.accept(this, arg));
        graphNode.addCallMarker(call, false);
        return true;
    }
}
