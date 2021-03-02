package es.upv.mist.slicing.nodes;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import es.upv.mist.slicing.graphs.GraphNodeContentVisitor;
import es.upv.mist.slicing.nodes.io.CallNode;
import es.upv.mist.slicing.utils.ASTUtils;
import es.upv.mist.slicing.utils.Logger;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static es.upv.mist.slicing.graphs.cfg.CFGBuilder.VARIABLE_NAME_OUTPUT;

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

    /** The action to perform when a declaration is found. */
    protected final DeclarationConsumer declConsumer;
    /** The action to perform when a definition is found. */
    protected final DefinitionConsumer defConsumer;
    /** The action to perform when a usage is found. */
    protected final UsageConsumer useConsumer;
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
    public VariableVisitor(DeclarationConsumer declConsumer, DefinitionConsumer defConsumer, UsageConsumer useConsumer) {
        this.declConsumer = Objects.requireNonNullElse(declConsumer, DeclarationConsumer.defaultConsumer());
        this.defConsumer = Objects.requireNonNullElse(defConsumer, DefinitionConsumer.defaultConsumer());
        this.useConsumer = Objects.requireNonNullElse(useConsumer, UsageConsumer.defaultConsumer());
    }

    public void visitAsDefinition(Node node, Expression value, Action action) {
        definitionStack.push(value);
        node.accept(this, action.or(Action.DEFINITION));
        definitionStack.pop();
    }

    @Override
    public void startVisit(GraphNode<?> node) {
        startVisit(node, Action.USE);
        groupActionsByRoot(node);
    }

    @Override
    public void visit(NameExpr n, Action action) {
        acceptAction(n, action);
    }

    @Override
    public void visit(ThisExpr n, Action arg) {
        acceptAction(n, arg);
    }

    @Override
    public void visit(FieldAccessExpr n, Action action) {
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
        // Otherwise, we traverse the scope to handle other structures (calls, arrays, etc).
        if (scope.isNameExpr() || scope.isThisExpr())
            acceptAction(n, action); // a.b.c.d
        else
            n.getScope().accept(this, action); // this.call().c.d
    }

    protected void acceptAction(Expression n, Action action) {
        switch (action) {
            case DECLARATION:
                declConsumer.acceptDeclaration(graphNode, n, getRealName(n));
                break;
            case DEFINITION:
                assert !definitionStack.isEmpty();
                defConsumer.acceptDefinition(graphNode, n, getRealName(n), definitionStack.peek());
                break;
            case USE:
                useConsumer.acceptUsage(graphNode, n, getRealName(n));
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
            declConsumer.acceptDeclaration(graphNode, null, variable.getNameAsString());
            // ForEach initializes to each value of the iterable, but that expression is not available.
            defConsumer.acceptDefinition(graphNode, null, variable.getNameAsString(), null);
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
        for (VariableDeclarator v : n.getVariables()) {
            String realName;
            realName = v.getNameAsString();
            declConsumer.acceptDeclaration(graphNode, null, realName);
            v.getInitializer().ifPresent(init -> {
                init.accept(this, action);
                defConsumer.acceptDefinition(graphNode, null, realName, init);
            });
        }
    }

    @Override
    public void visit(FieldDeclaration n, Action action) {
        for (VariableDeclarator v : n.getVariables()) {
            String realName;
            realName = getRealNameForFieldDeclaration(v);
            declConsumer.acceptDeclaration(graphNode, null, realName);
            Expression init = v.getInitializer().orElseGet(() -> ASTUtils.initializerForField(n));
            init.accept(this, action);
            defConsumer.acceptDefinition(graphNode, null, realName, init);
        }
    }

    @Override
    public void visit(CatchClause n, Action arg) {
        n.getParameter().accept(this, arg.or(Action.DECLARATION));
        n.getBody().accept(this, arg);
    }

    @Override
    public void visit(Parameter n, Action arg) {
        declConsumer.acceptDeclaration(graphNode, null, n.getNameAsString());
        defConsumer.acceptDefinition(graphNode, null, n.getNameAsString(), null); // TODO: improve initializer
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
        String realName4this = getFQClassName(n) + ".this";
        if (visitCall(n, arg)) {
            declareThis(n);
            super.visit(n, arg);
            // Define this even when the super() call is not resolved.
            graphNode.addDefinedVariable(null, realName4this, null);
        } else {
            // A node defines -output-
            var defOutput = new VariableAction.Definition(null, VARIABLE_NAME_OUTPUT, graphNode);
            var defOutputMov = new VariableAction.Movable(defOutput, CallNode.Return.create(n));
            graphNode.addActionsForCall(List.of(defOutputMov), n, false);
            // The container of the call defines this and then uses -output-
            graphNode.addActionsAfterCall(n,
                    new VariableAction.Definition(null, realName4this, graphNode),
                    new VariableAction.Usage(null, VARIABLE_NAME_OUTPUT, graphNode));
        }
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
        if (call instanceof ExplicitConstructorInvocationStmt) {
            declareThis((ExplicitConstructorInvocationStmt) call);
        }
        ASTUtils.getResolvableScope(call).ifPresent(s -> s.accept(this, arg));
        graphNode.addCallMarker(call, false);
        return false;
    }

    /** Adds a declaration for the variable 'this'. */
    protected void declareThis(ExplicitConstructorInvocationStmt call) {
        String variableName = getFQClassName(call) + ".this";
        declConsumer.acceptDeclaration(graphNode, null, variableName);
    }

    /** Obtains the fully qualified class name of the class that contains an AST node. */
    protected String getFQClassName(Node node) {
        // Known limitation: anonymous classes do not have a FQ class name.
        return ASTUtils.getClassNode(node).getFullyQualifiedName().orElseThrow();
    }

    /** Prepends [declaring class name].this. to the name of the given variable declarator. */
    protected String getRealNameForFieldDeclaration(VariableDeclarator decl) {
        return ASTUtils.getClassNode(decl).getFullyQualifiedName().orElseThrow() + ".this." + decl.getNameAsString();
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
        } else if (n.isThisExpr()) {
            var hasTypeName = n.asThisExpr().getTypeName().isPresent();
            return (hasTypeName ? n.asThisExpr().resolve().getQualifiedName() : getFQClassName(n)) + ".this";
        }
        return n.toString();
    }

    /** Generate the correct prefix for a NameExpr. Only works for non-static fields. */
    protected String getNamePrefix(NameExpr n) {
        // We only care about non-static fields
        ResolvedValueDeclaration resolved = n.resolve();
        if (!resolved.isField() || resolved.asField().isStatic())
            return "";
        // Obtain the class where the field is declared
        ResolvedTypeDeclaration declaringType = resolved.asField().declaringType();
        // Generate the full prefix
        return declaringType.getQualifiedName() + ".this.";
    }

    /** Extracts the parent elements affected by each variable action (e.g. an action on a.x affects a).
     *  When multiple compatible actions (same root and action) are found, only one parent element is generated. */
    protected void groupActionsByRoot(GraphNode<?> graphNode) {
        VariableAction lastRootAction = null;
        for (int i = 0; i < graphNode.variableActions.size(); i++) {
            VariableAction action = graphNode.variableActions.get(i);
            if (action.isRootAction() || action.isDeclaration() ||
                    action instanceof VariableAction.CallMarker) {
                if (lastRootAction != null) {
                    graphNode.variableActions.add(i, lastRootAction);
                    i++;
                    lastRootAction = null;
                }
                continue;
            }
            if (lastRootAction == null) {
                // generate our own root action
                lastRootAction = action.getRootAction();
                lastRootAction.addObjectField(action.getVariable());
                // It can be representing a fieldAccessExpr or a fieldDeclaration
                // in the first, we can use the expression to obtain the 'type.this' or 'object_name'
                // in the second, the expression is null but we can extract 'type.this' from realName
            } else {
                // Check if action matches the previously generated root action
                if (VariableAction.rootMatches(action, lastRootAction)
                        && VariableAction.typeMatches(action, lastRootAction)) {
                    lastRootAction.addObjectField(action.getVariable());
                } else {
                    // No match: add the root before the current element and update counter
                    graphNode.variableActions.add(i, lastRootAction);
                    i++;
                    // generate our own root action
                    lastRootAction = action.getRootAction();
                    lastRootAction.addObjectField(action.getVariable());
                }
            }
        }
        // Append the last root action if there is any!
        if (lastRootAction != null)
            graphNode.variableActions.add(lastRootAction);
    }

    @FunctionalInterface
    public interface DeclarationConsumer {
        void acceptDeclaration(GraphNode<?> graphNode, Expression variable, String realName);

        static DeclarationConsumer defaultConsumer() {
            return (a, b, c) -> {};
        }
    }

    @FunctionalInterface
    public interface DefinitionConsumer {
        void acceptDefinition(GraphNode<?> graphNode, Expression variable, String realName, Expression valueAssigned);

        static DefinitionConsumer defaultConsumer() {
            return (a, b, c, d) -> {};
        }
    }

    @FunctionalInterface
    public interface UsageConsumer {
        void acceptUsage(GraphNode<?> graphNode, Expression variable, String realName);

        static UsageConsumer defaultConsumer() {
            return (a, b, c) -> {};
        }
    }
}
