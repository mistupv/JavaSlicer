package es.upv.mist.slicing.nodes;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithVariables;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserFieldDeclaration;
import es.upv.mist.slicing.graphs.GraphNodeContentVisitor;
import es.upv.mist.slicing.utils.ASTUtils;
import es.upv.mist.slicing.utils.Logger;
import es.upv.mist.slicing.utils.QuadConsumer;
import es.upv.mist.slicing.utils.TriConsumer;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;

/** A graph node visitor that extracts the actions performed in a given GraphNode. An initial action mode can
 *  be set, to consider variables found a declaration, definition or usage (default).
 *  @see GraphNodeContentVisitor */
public class VariableVisitor extends GraphNodeContentVisitor<VariableVisitor.Action> {
    /** The default action that a found variable performs. */
    public enum Action {
        DECLARATION, DEFINITION, USE;

        /** Whether the action is performed in all executions of the node. */
        private boolean always = true;

        /** Join multiple actions into one. */
        public Action or(Action action) {
            if (action == DECLARATION || this == DECLARATION)
                return DECLARATION;
            if (action == DEFINITION || this == DEFINITION)
                return DEFINITION;
            return USE;
        }

        public void setAlways(boolean always) {
            this.always = always;
        }
    }

    /** A default action to be used as a placeholder when a {@code null} action is received. */
    protected static final TriConsumer<GraphNode<?>, Expression, String> BLANK_TRICONSUMER = (a, b, c) -> {};
    protected static final QuadConsumer<GraphNode<?>, Expression, String, Expression> BLANK_QUADCONSUMER = (a, b, c, d) -> {};

    /** The action to perform when a declaration is found. */
    protected final TriConsumer<GraphNode<?>, Expression, String> declConsumer;
    /** The action to perform when a definition is found. */
    protected final QuadConsumer<GraphNode<?>, Expression, String, Expression> defConsumer;
    /** The action to perform when a usage is found. */
    protected final TriConsumer<GraphNode<?>, Expression, String> useConsumer;
    /** A stack with the last definition expression, to provide it when a variable definition is found. */
    protected final Deque<Expression> definitionStack = new LinkedList<>();

    /** A variable visitor that will add each action to the list of actions of the graph node.
     *  The entry-point for this graph MUST be {@link #startVisit(GraphNode)} or {@link #startVisit(GraphNode, Action)} */
    public VariableVisitor() {
        this(GraphNode::addDeclaredVariable, GraphNode::addDefinedVariable, GraphNode::addUsedVariable);
    }

    /** A variable visitor that will perform the given actions when a variable is found. A node can accept this visitor,
     *  but calls will be ignored if the entry-point is not {@link #startVisit(GraphNode)} or {@link #startVisit(GraphNode, Action)}.
     *  The arguments are the actions to be performed when an action is found in the corresponding node. */
    public VariableVisitor(TriConsumer<GraphNode<?>, Expression, String> declConsumer,
                           QuadConsumer<GraphNode<?>, Expression, String, Expression> defConsumer,
                           TriConsumer<GraphNode<?>, Expression, String> useConsumer) {
        this.declConsumer = Objects.requireNonNullElse(declConsumer, BLANK_TRICONSUMER);
        this.defConsumer = Objects.requireNonNullElse(defConsumer, BLANK_QUADCONSUMER);
        this.useConsumer = Objects.requireNonNullElse(useConsumer, BLANK_TRICONSUMER);
    }

    public void visitAsDefinition(Node node, Expression value, Action action) {
        definitionStack.push(value);
        node.accept(this, action.or(Action.DEFINITION));
        definitionStack.pop();
    }

    @Override
    public void startVisit(GraphNode<?> node) {
        startVisit(node, Action.USE);
    }

    @Override
    public void visit(NameExpr n, Action action) {
        acceptAction(n, action);
    }

    @Override
    public void visit(FieldAccessExpr n, Action action) {
        // Traverse the scope of this variable access
        n.getScope().accept(this, action);
        // Register the field access as action
        Expression scope = n.getScope();
        boolean traverse = true;
        while (traverse) {
            if (scope.isFieldAccessExpr())
                scope = scope.asFieldAccessExpr().getScope();
            else if (scope.isEnclosedExpr())
                scope = scope.asEnclosedExpr().getInner();
            else if (scope.isCastExpr())
                scope = scope.asCastExpr().getExpression();
            else
                traverse = false;
        }
        // Only accept the field access as action if it is a sequence of names (a.b.c.d, this.a.b.c)
        if (scope.isNameExpr() || scope.isThisExpr())
            acceptAction(n, action);
    }

    protected void acceptAction(Expression n, Action action) {

        switch (action) {
            case DECLARATION:
                declConsumer.accept(graphNode, n, getRealName(n));
                break;
            case DEFINITION:
                assert !definitionStack.isEmpty();
                defConsumer.accept(graphNode, n, getRealName(n), definitionStack.peek());
                break;
            case USE:
                useConsumer.accept(graphNode, n, getRealName(n));
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

    // Modified traversal (there may be variable definitions or declarations)
    @Override
    public void visit(ForEachStmt n, Action action) {
        n.getIterable().accept(this, Action.USE);
        for (VariableDeclarator variable : n.getVariable().getVariables()) {
            declConsumer.accept(graphNode, null, variable.getNameAsString());
            // ForEach initializes to each value of the iterable, but that expression is not available.
            defConsumer.accept(graphNode, null, variable.getNameAsString(), null);
        }
    }

    @Override
    public void visit(ConditionalExpr n, Action arg) {
        n.getCondition().accept(this, arg);
        boolean always = arg.always;
        arg.setAlways(false);
        n.getThenExpr().accept(this, arg);
        n.getElseExpr().accept(this, arg);
        arg.setAlways(always);
    }

    @Override
    public void visit(BinaryExpr n, Action arg) {
        n.getLeft().accept(this, arg);
        switch (n.getOperator()) {
            case OR:
            case AND:
                boolean always = arg.always;
                arg.setAlways(false);
                n.getRight().accept(this, arg);
                arg.setAlways(always);
            default:
                n.getRight().accept(this, arg);
        }
        n.getComment().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(AssignExpr n, Action action) {
        // Value is always visited first since uses occur before definitions
        n.getValue().accept(this, action);
        // Target will be used if operator is not '='
        if (n.getOperator() != AssignExpr.Operator.ASSIGN)
            n.getTarget().accept(this, action);
        visitAsDefinition(n.getTarget(), n.getValue(), action);
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
                visitAsDefinition(n.getExpression(), null, action); // TODO: improve initializer
                break;
        }
        n.getExpression().accept(this, action);
        switch (n.getOperator()) {
            case POSTFIX_INCREMENT:
            case POSTFIX_DECREMENT:
                visitAsDefinition(n.getExpression(), null, action); // TODO: improve initializer
                break;
        }
    }

    @Override
    public void visit(VariableDeclarationExpr n, Action action) {
        visitVarDeclaration(n, action);
    }

    @Override
    public void visit(FieldDeclaration n, Action action) {
        visitVarDeclaration(n, action);
    }

    protected void visitVarDeclaration(NodeWithVariables<?> declaration, Action action) {
        for (VariableDeclarator v : declaration.getVariables()) {
            declConsumer.accept(graphNode, null, v.getNameAsString());
            v.getInitializer().ifPresent(init -> {
                init.accept(this, action);
                defConsumer.accept(graphNode, null, v.getNameAsString(), init);
            });
        }
    }

    @Override
    public void visit(CatchClause n, Action arg) {
        n.getParameter().accept(this, arg.or(Action.DECLARATION));
        n.getBody().accept(this, arg);
    }

    @Override
    public void visit(Parameter n, Action arg) {
        declConsumer.accept(graphNode, null, n.getNameAsString());
        defConsumer.accept(graphNode, null, n.getNameAsString(), null); // TODO: improve initializer
    }

    // =======================================================================
    // ================================ CALLS ================================
    // =======================================================================

    // If the call will be linked (there is an AST node), skip parameters, add markers and add to node
    // TODO: non-linked calls should mark both parameters and scope as DEF+USE (problem: 'System.out.println(z)')

    @Override
    public void visit(ObjectCreationExpr n, Action arg) {
        if (visitCall(n, arg))
            super.visit(n, arg);
    }

    @Override
    public void visit(ExplicitConstructorInvocationStmt n, Action arg) {
        if (visitCall(n, arg))
            super.visit(n, arg);
    }

    @Override
    public void visit(MethodCallExpr n, Action arg) {
        if (visitCall(n, arg))
            super.visit(n, arg);
    }

    /** Tries to resolve and add the corresponding call markers. */
    protected boolean visitCall(Resolvable<? extends ResolvedMethodLikeDeclaration> call, Action arg) {
        if (ASTUtils.shouldVisitArgumentsForMethodCalls(call, graphNode))
            return true;
        graphNode.addCallMarker(call, true);
        ASTUtils.getResolvableScope(call).ifPresent(s -> s.accept(this, arg));
        graphNode.addCallMarker(call, false);
        return false;
    }

    /** Obtain the prefixed name of a variable, to improve matching of variables
     *  that point to the same object, such as 'x' and 'this.x'. */
    protected String getRealName(Expression n) {
        if (n.isNameExpr()) {
            try {
                return getNamePrefix(n.asNameExpr()) + n.toString();
            } catch (UnsolvedSymbolException e) {
                Logger.log("Unable to resolve symbol " + e.getName());
            }
        }
        return n.toString();
    }

    /** Generate the correct prefix for a NameExpr. Only works for non-static fields. */
    protected String getNamePrefix(NameExpr n) {
        // We only care about non-static fields
        ResolvedValueDeclaration resolved = n.resolve();
        if (!resolved.isField() || resolved.asField().isStatic())
            return "";
        // Obtain the class where the field is declared and the current class
        JavaParserFieldDeclaration field = (JavaParserFieldDeclaration) resolved.asField();
        Node decClass = ASTUtils.getClassNode(field.getVariableDeclarator());
        Node nClass = ASTUtils.getClassNode(n);
        // If the classes match, the prefix can be simplified
        if (decClass.equals(nClass))
            return "this.";
        // Full prefix
        return ((ClassOrInterfaceDeclaration) decClass).getNameAsString() + ".this.";
    }
}
