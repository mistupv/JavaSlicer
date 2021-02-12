package es.upv.mist.slicing.nodes;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserFieldDeclaration;
import es.upv.mist.slicing.graphs.GraphNodeContentVisitor;
import es.upv.mist.slicing.utils.ASTUtils;
import es.upv.mist.slicing.utils.TriConsumer;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;
import java.util.function.BiConsumer;

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
    protected static final BiConsumer<GraphNode<?>, Expression> BLANK_BICONSUMER = (a, b) -> {};
    protected static final TriConsumer<GraphNode<?>, Expression, Expression> BLANK_TRICONSUMER = (a, b, c) -> {};

    /** The action to perform when a declaration is found. */
    protected final BiConsumer<GraphNode<?>, Expression> declConsumer;
    /** The action to perform when a definition is found. */
    protected final TriConsumer<GraphNode<?>, Expression, Expression> defConsumer;
    /** The action to perform when a usage is found. */
    protected final BiConsumer<GraphNode<?>, Expression> useConsumer;
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
    public VariableVisitor(BiConsumer<GraphNode<?>, Expression> declConsumer, TriConsumer<GraphNode<?>, Expression, Expression> defConsumer, BiConsumer<GraphNode<?>, Expression> useConsumer) {
        this.declConsumer = Objects.requireNonNullElse(declConsumer, BLANK_BICONSUMER);
        this.defConsumer = Objects.requireNonNullElse(defConsumer, BLANK_TRICONSUMER);
        this.useConsumer = Objects.requireNonNullElse(useConsumer, BLANK_BICONSUMER);
    }

    public void visitAsDefinition(Node node, Expression value) {
        visitAsDefinition(node, value, Action.DEFINITION);
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
        String prefix = this.getNamePrefix(n); // Add a prefix to the name ("this." or "CLASS.this.")
        n.setName(new SimpleName(prefix + n.getNameAsString()));
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
                declConsumer.accept(graphNode, n);
                break;
            case DEFINITION:
                assert !definitionStack.isEmpty();
                defConsumer.accept(graphNode, n, definitionStack.peek());
                break;
            case USE:
                useConsumer.accept(graphNode, n);
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
            variable.getNameAsExpression().accept(this, Action.DECLARATION);
            visitAsDefinition(variable.getNameAsExpression(), null); // TODO: add a better initializer
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
        // Target will be used if operator is not '='
        if (n.getOperator() != AssignExpr.Operator.ASSIGN)
            n.getTarget().accept(this, action);
        visitAsDefinition(n.getTarget(), n.getValue(), action);
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
        for (VariableDeclarator v : n.getVariables()) {
            v.getNameAsExpression().accept(this, action.or(Action.DECLARATION));
            v.getInitializer().ifPresent(init -> {
                init.accept(this, action);
                visitAsDefinition(v.getNameAsExpression(), init);
            });
        }
    }

    @Override
    public void visit(FieldDeclaration n, Action action) {
        for (VariableDeclarator v : n.getVariables()) {
            v.getNameAsExpression().accept(this, action.or(Action.DECLARATION));
            v.getInitializer().ifPresent(init -> {
                init.accept(this, action);
                visitAsDefinition(v.getNameAsExpression(), init);
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
        declConsumer.accept(graphNode, new NameExpr(n.getName().getId()));
        defConsumer.accept(graphNode, new NameExpr(n.getName().getId()), null); // TODO: improve initializer
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

    /** Checks whether a NameExpr representing a variable
     * is a Field and returns the correct prefix to reference it */
    protected String getNamePrefix(NameExpr n){
        // There are three constructs whose variable cannot be resolved due to: getNameAsExpression() function
        // FieldDeclaration, VariableDeclarationExpr, and ForEachStmt. They must be treated separately

        // 1. FieldDeclaration
        if (graphNode.getAstNode() instanceof FieldDeclaration)
            return "this.";
        // 2. VariableDeclarationExpr
        if (graphNode.getAstNode() instanceof ExpressionStmt) {
            Expression expr = ((ExpressionStmt) graphNode.getAstNode()).getExpression();
            if (expr instanceof VariableDeclarationExpr)
                return "";
        }
        // 3. ForEachStmt
        if (graphNode.getAstNode() instanceof ForEachStmt)
            throw new RuntimeException("ForEachStmt internal structure is still a mystery for me");// TODO: Execute and see the internal structure

        // The rest of nodes can be resolved properly
        if (n.resolve().isField() && !n.resolve().asField().isStatic()){
            JavaParserFieldDeclaration fieldDeclaration = (JavaParserFieldDeclaration) n.resolve();
            Node decClass = ASTUtils.getClassNode(fieldDeclaration.getVariableDeclarator());
            Node nClass = ASTUtils.getClassNode(n);

            assert (nClass instanceof ClassOrInterfaceDeclaration &&
                    decClass instanceof ClassOrInterfaceDeclaration);

            if (decClass.equals(nClass))
                return "this.";
            return ((ClassOrInterfaceDeclaration) decClass).getNameAsString() + ".this.";
        }
        return "";
    }
}
