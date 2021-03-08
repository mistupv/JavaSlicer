package es.upv.mist.slicing.nodes;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithArguments;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.AssociableToAST;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;
import es.upv.mist.slicing.graphs.ClassGraph;
import es.upv.mist.slicing.graphs.ExpressionObjectTreeFinder;
import es.upv.mist.slicing.graphs.GraphNodeContentVisitor;
import es.upv.mist.slicing.nodes.VariableAction.ObjectTree;
import es.upv.mist.slicing.nodes.io.ActualIONode;
import es.upv.mist.slicing.nodes.io.CallNode;
import es.upv.mist.slicing.utils.ASTUtils;
import es.upv.mist.slicing.utils.Logger;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static es.upv.mist.slicing.graphs.cfg.CFGBuilder.VARIABLE_NAME_OUTPUT;
import static es.upv.mist.slicing.nodes.VariableVisitor.Action.*;

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

    /** A stack with the last definition expression, to provide it when a variable definition is found. */
    protected final Deque<Expression> definitionStack = new LinkedList<>();
    /** If this stack is non-empty, every action created must be of type Movable, and its real node must be
     *  the top of this stack. Used for actual-in nodes. */
    protected final Deque<SyntheticNode<?>> realNodeStack = new LinkedList<>();

    public void visitAsDefinition(Node node, Expression value, Action action) {
        definitionStack.push(value);
        node.accept(this, action.or(DEFINITION));
        definitionStack.pop();
    }

    @Override
    public void startVisit(GraphNode<?> node) {
        startVisit(node, USE);
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
    public void visit(SuperExpr n, Action arg) {
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
        acceptAction(n, getRealName(n), action, false);
    }

    protected void acceptAction(String realName, Action action) {
        acceptAction(null, realName, action, false);
    }

    protected void acceptAction(Expression n, String realName, Action action) {
        acceptAction(n, realName, action, false);
    }

    protected void acceptActionNullDefinition(String realName) {
        acceptAction(null, realName, DEFINITION, true);
    }

    protected void acceptAction(Expression n, String realName, Action action, boolean canDefBeNull) {
        VariableAction va;
        switch (action) {
            case DECLARATION:
                va = new VariableAction.Declaration(n, realName, graphNode);
                break;
            case DEFINITION:
                assert !definitionStack.isEmpty() || canDefBeNull;
                va = new VariableAction.Definition(n, realName, graphNode, definitionStack.peek());
                break;
            case USE:
                va = new VariableAction.Usage(n, realName, graphNode);
                break;
            default:
                throw new UnsupportedOperationException();
        }
        if (!realNodeStack.isEmpty()) {
            realNodeStack.peek().addVariableAction(va);
            va = new VariableAction.Movable(va, realNodeStack.peek());
        }
        graphNode.addVariableAction(va);
    }

    // Partially traversed (only expressions that may contain variables are traversed)
    @Override
    public void visit(CastExpr n, Action action) {
        n.getExpression().accept(this, action);
    }

    // Modified traversal (there may be variable definitions or declarations)
    @Override
    public void visit(ReturnStmt n, Action arg) {
        super.visit(n, arg);
        if (n.getExpression().isPresent()) {
            definitionStack.push(n.getExpression().get());
            acceptAction(VARIABLE_NAME_OUTPUT, DEFINITION);
            definitionStack.pop();
        }
    }

    @Override
    public void visit(ForEachStmt n, Action action) {
        n.getIterable().accept(this, USE);
        for (VariableDeclarator variable : n.getVariable().getVariables()) {
            acceptAction(variable.getNameAsString(), DECLARATION);
            // ForEach initializes to each value of the iterable, but that expression is not available.
            acceptActionNullDefinition(variable.getNameAsString());
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
        if (n.getValue().calculateResolvedType().isPrimitive()) {
            // Target will be used if operator is not '='
            if (n.getOperator() != AssignExpr.Operator.ASSIGN)
                n.getTarget().accept(this, action);
            visitAsDefinition(n.getTarget(), n.getValue(), action);
        } else {
            List<String> realNameWithoutRootList = new LinkedList<>();
            n.getTarget().accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(NameExpr nameExpr, Void arg) {
                    String realName = getRealName(nameExpr);
                    definitionStack.push(n.getValue());
                    if (!realName.contains(".")) {
                        acceptAction(nameExpr, realName, DEFINITION);
                        realNameWithoutRootList.add("");
                    } else {
                        String root = ObjectTree.removeFields(realName);
                        acceptAction(root, DEFINITION);
                        List<VariableAction> list = graphNode.getVariableActions();
                        VariableAction def = list.get(graphNode.getVariableActions().size() - 1);
                        def.getObjectTree().addField(realName);
                        realNameWithoutRootList.add(realName);
                    }
                    definitionStack.pop();
                }

                @Override
                public void visit(FieldAccessExpr fieldAccessExpr, Void arg) {
                    Expression scope = fieldAccessExpr.getScope();
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
                    if (!scope.isNameExpr() && !scope.isThisExpr())
                        throw new IllegalStateException("only valid assignments are this[.<field>]+ =, and <var>[.<field>]+");
                    String realName = getRealName(fieldAccessExpr);
                    String root = ObjectTree.removeFields(realName);
                    definitionStack.push(n.getValue());
                    acceptAction(root, DEFINITION);
                    definitionStack.pop();
                    List<VariableAction> list = graphNode.getVariableActions();
                    VariableAction def = list.get(graphNode.getVariableActions().size() - 1);
                    def.getObjectTree().addField(realName);
                    realNameWithoutRootList.add(ObjectTree.removeRoot(realName));
                }

                @Override
                public void visit(ArrayAccessExpr n, Void arg) {
                    throw new UnsupportedOperationException("Arrays are not yet supported as target of assignment.");
                }
            }, null);
            assert realNameWithoutRootList.size() == 1;
            List<VariableAction> list = graphNode.getVariableActions();
            ExpressionObjectTreeFinder finder = new ExpressionObjectTreeFinder(graphNode);
            finder.handleAssignExpr(n, list.get(graphNode.getVariableActions().size() - 1), realNameWithoutRootList.get(0));
        }
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
                visitAsDefinition(n.getExpression(), null, action);
                break;
        }
        n.getExpression().accept(this, action);
        switch (n.getOperator()) {
            case POSTFIX_INCREMENT:
            case POSTFIX_DECREMENT:
                visitAsDefinition(n.getExpression(), null, action);
                break;
        }
    }

    @Override
    public void visit(VariableDeclarationExpr n, Action action) {
        for (VariableDeclarator v : n.getVariables()) {
            acceptAction(v.getNameAsString(), DECLARATION);
            v.getInitializer().ifPresent(init -> {
                init.accept(this, action);
                acceptActionNullDefinition(v.getNameAsString());
            });
            v.accept(this, action);
        }
    }

    @Override
    public void visit(FieldDeclaration n, Action action) {
        for (VariableDeclarator v : n.getVariables()) {
            String realName = getRealNameForFieldDeclaration(v);
            acceptAction(realName, DECLARATION);
            Expression init = v.getInitializer().orElseGet(() -> ASTUtils.initializerForField(n));
            init.accept(this, action);
            definitionStack.push(init);
            acceptAction(realName, DEFINITION);
            definitionStack.pop();
            v.accept(this, action);
        }
    }

    @Override
    public void visit(VariableDeclarator n, Action arg) {
        if (n.getType().isClassOrInterfaceType() && n.getInitializer().isPresent())
            new ExpressionObjectTreeFinder(graphNode).handleVariableDeclarator(n);
    }

    @Override
    public void visit(CatchClause n, Action arg) {
        n.getParameter().accept(this, arg.or(DECLARATION));
        n.getBody().accept(this, arg);
    }

    @Override
    public void visit(Parameter n, Action arg) {
        acceptAction(n.getNameAsString(), DECLARATION);
        acceptActionNullDefinition(n.getNameAsString());
    }

    // =======================================================================
    // ================================ CALLS ================================
    // =======================================================================

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
            acceptAction(getFQClassName(n) + ".this", DECLARATION);
            super.visit(n, arg);
        }
        // Regardless of whether it resolves or not, 'this' is defined
        acceptActionNullDefinition(realName4this);
    }

    @Override
    public void visit(MethodCallExpr n, Action arg) {
        if (visitCall(n, arg))
            super.visit(n, arg);
    }

    /** Tries to resolve and add the corresponding call markers. */
    protected boolean visitCall(Resolvable<? extends ResolvedMethodLikeDeclaration> call, Action action) {
        // If we don't have the AST for the call, we should visit the rest of the call.
        if (ASTUtils.shouldVisitArgumentsForMethodCalls(call, graphNode))
            return true;
        CallableDeclaration<?> decl = ASTUtils.getResolvedAST(call.resolve()).orElseThrow();
        // Start
        graphNode.addCallMarker(call, true);
        // Scope
        if (call instanceof ExplicitConstructorInvocationStmt)
            acceptAction(getFQClassName((ExplicitConstructorInvocationStmt) call) + ".this", DECLARATION);
        if (call instanceof MethodCallExpr && !((JavaParserMethodDeclaration) call.resolve()).isStatic()) {
            ActualIONode scopeIn = ActualIONode.createActualIn(call, getFQClassName(decl) + ".this", ((MethodCallExpr) call).getScope().orElse(null));
            graphNode.addSyntheticNode(scopeIn);
            realNodeStack.push(scopeIn);
            ASTUtils.getResolvableScope(call).ifPresentOrElse(
                    scope -> scope.accept(this, action),
                    () -> acceptAction(getFQClassName(decl) + ".this", USE));
            realNodeStack.pop();
        }
        // Args
        NodeWithArguments<?> callWithArgs = (NodeWithArguments<?>) call;
        for (int i = 0; i < callWithArgs.getArguments().size(); i++) {
            Expression argument = callWithArgs.getArguments().get(i);
            ActualIONode actualIn = ActualIONode.createActualIn(call, decl.getParameter(i).getNameAsString(), argument);
            graphNode.addSyntheticNode(actualIn);
            realNodeStack.push(actualIn);
            argument.accept(this, action);
            realNodeStack.pop();
        }
        // Return
        insertOutputDefAndUse(call);
        // End
        graphNode.addCallMarker(call, false);
        return false; // We have manually visited each element, don't call super.visit(...)
    }

    protected void insertOutputDefAndUse(Resolvable<? extends ResolvedMethodLikeDeclaration> call) {
        if (ASTUtils.resolvableIsVoid(call))
            return;
        Expression callExpr = call instanceof Expression ? (Expression) call : null;
        // A node defines -output-
        var fields = getFieldsForReturn(call);
        VariableAction.Definition def;
        if (fields.isPresent())
            def = new VariableAction.Definition(callExpr, VARIABLE_NAME_OUTPUT, graphNode, (ObjectTree) fields.get().clone());
        else
            def = new VariableAction.Definition(callExpr, VARIABLE_NAME_OUTPUT, graphNode);
        var defMov = new VariableAction.Movable(def, CallNode.Return.create(call));
        graphNode.addVariableAction(defMov);
        // The container of the call uses -output-, unless the call is wrapped in an ExpressionStmt
        Optional<Node> parentNode = ((Node) call).getParentNode();
        if (parentNode.isEmpty() || !(parentNode.get() instanceof ExpressionStmt)) {
            VariableAction.Usage use;
            if (fields.isPresent())
                use = new VariableAction.Usage(callExpr, VARIABLE_NAME_OUTPUT, graphNode, (ObjectTree) fields.get().clone());
            else
                use = new VariableAction.Usage(callExpr, VARIABLE_NAME_OUTPUT, graphNode);
            graphNode.addVariableAction(use);
        }
    }

    protected Optional<ObjectTree> getFieldsForReturn(Resolvable<? extends ResolvedMethodLikeDeclaration> call) {
        ResolvedMethodLikeDeclaration resolved = call.resolve();
        if (resolved instanceof AssociableToAST) {
            Optional<? extends Node> n = ((AssociableToAST<? extends Node>) resolved).toAst();
            if (n.isPresent() && n.get() instanceof CallableDeclaration)
                return ClassGraph.getInstance().generateObjectTreeForReturnOf((CallableDeclaration<?>) n.get());
        }
        return Optional.empty();
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
        } else if (n.isSuperExpr()) {
            if (n.asSuperExpr().getTypeName().isPresent())
                return n.asSuperExpr().calculateResolvedType().asReferenceType().getQualifiedName() + ".this";
            return getFQClassName(n) + ".this";
        } else if (n.isThisExpr()) {
            var hasTypeName = n.asThisExpr().getTypeName().isPresent();
            return (hasTypeName ? n.asThisExpr().resolve().getQualifiedName() : getFQClassName(n)) + ".this";
        } else if (n.isFieldAccessExpr()) { // this.a.b
            Expression scope = n.asFieldAccessExpr().getScope();
            StringBuilder builder = new StringBuilder(n.asFieldAccessExpr().getNameAsString());
            while (scope instanceof FieldAccessExpr) {
                builder.insert(0, '.');
                builder.insert(0, scope.asFieldAccessExpr().getNameAsString());
                scope = scope.asFieldAccessExpr().getScope();
            }
            builder.insert(0, '.');
            builder.insert(0, getRealName(scope));
            return builder.toString();
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
                // It can be representing a fieldAccessExpr or a fieldDeclaration
                // in the first, we can use the expression to obtain the 'type.this' or 'object_name'
                // in the second, the expression is null but we can extract 'type.this' from realName
            } else if (!VariableAction.rootMatches(action, lastRootAction)
                    || !VariableAction.typeMatches(action, lastRootAction)) {
                // No match: add the root before the current element and update counter
                graphNode.variableActions.add(i, lastRootAction);
                i++;
                // generate our own root action
                lastRootAction = action.getRootAction();
            }
            lastRootAction.addObjectField(action.getVariable());
        }
        // Append the last root action if there is any!
        if (lastRootAction != null)
            graphNode.variableActions.add(lastRootAction);
    }
}
