package es.upv.mist.slicing.nodes;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithArguments;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.AssociableToAST;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;
import es.upv.mist.slicing.graphs.ClassGraph;
import es.upv.mist.slicing.graphs.ExpressionObjectTreeFinder;
import es.upv.mist.slicing.graphs.GraphNodeContentVisitor;
import es.upv.mist.slicing.nodes.VariableAction.DeclarationType;
import es.upv.mist.slicing.nodes.io.ActualIONode;
import es.upv.mist.slicing.nodes.io.CallNode;
import es.upv.mist.slicing.utils.ASTUtils;
import es.upv.mist.slicing.utils.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static es.upv.mist.slicing.graphs.cfg.CFGBuilder.VARIABLE_NAME_OUTPUT;
import static es.upv.mist.slicing.graphs.exceptionsensitive.ESCFG.ACTIVE_EXCEPTION_VARIABLE;
import static es.upv.mist.slicing.nodes.ObjectTree.ROOT_NAME;
import static es.upv.mist.slicing.nodes.VariableAction.DeclarationType.*;
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
        generatePolyTrees(node);
    }

    @Override
    public void visit(NameExpr n, Action action) {
        String realName = getRealName(n);
        if (realName.equals(n.toString())) {
            acceptAction(n, action);
        } else {
            acceptAction(DeclarationType.valueOf(n), realName, action);
            graphNode.getLastVariableAction().setStaticType(ASTUtils.resolvedTypeDeclarationToResolvedType(n.findAncestor(ClassOrInterfaceDeclaration.class).orElseThrow().resolve()));
        }
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
        if (scope.isNameExpr() || scope.isThisExpr()) {
            String realName = getRealName(n);
            if (realName.equals(n.toString())) {
                acceptAction(n, action); // a.b.c.d
                vaSetStaticType(scope);
            } else {
                acceptAction(DeclarationType.valueOf(n), realName, action);
                graphNode.getLastVariableAction().setStaticType(ASTUtils.resolvedTypeDeclarationToResolvedType(n.findAncestor(ClassOrInterfaceDeclaration.class).orElseThrow().resolve()));
            }
        } else
            n.getScope().accept(this, action); // this.call().c.d
    }

    protected void acceptAction(Expression n, Action action) {
        acceptAction(DeclarationType.valueOf(n), getRealName(n), action, false);
        vaSetStaticType(n);
    }

    protected void acceptAction(DeclarationType declarationType, String realName, Action action) {
        acceptAction(declarationType, realName, action, false);
    }

    protected void acceptAction(Expression n, String realName, Action action) {
        acceptAction(DeclarationType.valueOf(n), realName, action, false);
        vaSetStaticType(n);
    }

    protected void acceptActionNullDefinition(DeclarationType declarationType, String realName) {
        acceptAction(declarationType, realName, DEFINITION, true);
    }

    protected void acceptAction(DeclarationType declarationType, String realName, Action action, boolean canDefBeNull) {
        VariableAction va;
        switch (action) {
            case DECLARATION:
                va = new VariableAction.Declaration(declarationType, realName, graphNode);
                break;
            case DEFINITION:
                assert !definitionStack.isEmpty() || canDefBeNull;
                va = new VariableAction.Definition(declarationType, realName, graphNode, definitionStack.peek());
                break;
            case USE:
                va = new VariableAction.Usage(declarationType, realName, graphNode);
                break;
            default:
                throw new UnsupportedOperationException();
        }
        if (!realNodeStack.isEmpty()) {
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
    public void visit(ArrayAccessExpr n, Action arg) {
        if (arg == DEFINITION) {
            n.getName().accept(this, arg);
            n.getIndex().accept(this, USE);
        } else if (arg == USE) {
            super.visit(n, arg);
        } else {
            throw new IllegalStateException("Array accesses cannot be defined");
        }
    }

    @Override
    public void visit(ReturnStmt n, Action arg) {
        super.visit(n, arg);
        if (n.getExpression().isPresent()) {
            definitionStack.push(n.getExpression().get());
            acceptAction(SYNTHETIC, VARIABLE_NAME_OUTPUT, DEFINITION);
            vaSetStaticType(n.getExpression().get());
            definitionStack.pop();
            graphNode.getLastVariableAction().asDefinition()
                    .setTotallyDefinedMember(ROOT_NAME);
        }
    }

    @Override
    public void visit(ThrowStmt n, Action arg) {
        super.visit(n, arg);
        definitionStack.push(n.getExpression());
        acceptAction(SYNTHETIC, ACTIVE_EXCEPTION_VARIABLE, DEFINITION);
        vaSetStaticType(n.getExpression());
        definitionStack.pop();
        var fields = ClassGraph.getInstance().generateObjectTreeFor(n.getExpression().calculateResolvedType().asReferenceType());
        graphNode.getLastVariableAction().getObjectTree().addAll(fields);
        new ExpressionObjectTreeFinder(graphNode).locateAndMarkTransferenceToRoot(n.getExpression(), -1);
    }

    @Override
    public void visit(ForEachStmt n, Action action) {
        n.getIterable().accept(this, USE);
        for (VariableDeclarator variable : n.getVariable().getVariables()) {
            acceptAction(LOCAL_VARIABLE, variable.getNameAsString(), DECLARATION);
            graphNode.getLastVariableAction().setStaticType(variable.getType().resolve());
            // ForEach initializes to each value of the iterable, but that expression is not available.
            acceptActionNullDefinition(LOCAL_VARIABLE, variable.getNameAsString());
            graphNode.getLastVariableAction().setStaticType(variable.getType().resolve());
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
                break;
            default:
                n.getRight().accept(this, arg);
                break;
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
        List<String> realNameWithoutRootList = new LinkedList<>();
        List<Boolean> foundArray = new LinkedList<>();
        n.getTarget().accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(NameExpr nameExpr, Void arg) {
                String realName = getRealName(nameExpr);
                definitionStack.push(n.getValue());
                if (!realName.contains(".")) {
                    acceptAction(nameExpr, realName, DEFINITION);
                    getLastDefinition().setTotallyDefinedMember(realName);
                    realNameWithoutRootList.add("");
                } else {
                    String root = ObjectTree.removeFields(realName);
                    acceptAction(DeclarationType.valueOf(nameExpr), root, DEFINITION);
                    VariableAction.Definition def = getLastDefinition();
                    def.setStaticType(ASTUtils.resolvedTypeDeclarationToResolvedType(nameExpr.findAncestor(ClassOrInterfaceDeclaration.class).orElseThrow().resolve()));
                    def.getObjectTree().addField(realName);
                    def.setTotallyDefinedMember(realName);
                    realNameWithoutRootList.add(ObjectTree.removeRoot(realName));
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
                if (root.equals(scope.toString()))
                    acceptAction(scope, root, DEFINITION);
                else {
                    acceptAction(FIELD, root, DEFINITION);
                    graphNode.getLastVariableAction().setStaticType(ASTUtils.resolvedTypeDeclarationToResolvedType(fieldAccessExpr.findAncestor(ClassOrInterfaceDeclaration.class).orElseThrow().resolve()));
                }
                definitionStack.pop();
                VariableAction.Definition def = getLastDefinition();
                def.setTotallyDefinedMember(realName);
                def.getObjectTree().addField(realName);
                realNameWithoutRootList.add(ObjectTree.removeRoot(realName));
            }

            @Override
            public void visit(ArrayAccessExpr n, Void arg) {
                n.getName().accept(this, arg);
                n.getIndex().accept(VariableVisitor.this, USE);
                foundArray.add(true);
            }
        }, null);
        assert realNameWithoutRootList.size() == 1 || !foundArray.isEmpty();
        groupActionsByRoot(graphNode);
        ExpressionObjectTreeFinder finder = new ExpressionObjectTreeFinder(graphNode);
        if (foundArray.isEmpty()) // Handle a field access or normal variable
            finder.handleAssignExpr(n, graphNode.getLastVariableAction(), realNameWithoutRootList.get(0));
        else // Handle an array access
            finder.handleArrayAssignExpr(n);
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
            acceptAction(LOCAL_VARIABLE, v.getNameAsString(), DECLARATION);
            graphNode.getLastVariableAction().setStaticType(v.getType().resolve());
            v.getInitializer().ifPresent(init -> {
                init.accept(this, action);
                definitionStack.push(init);
                acceptAction(LOCAL_VARIABLE, v.getNameAsString(), DEFINITION);
                graphNode.getLastVariableAction().setStaticType(v.getType().resolve());
                definitionStack.pop();
                if (v.getType().isClassOrInterfaceType())
                    getLastDefinition().setTotallyDefinedMember(v.getNameAsString());
                v.accept(this, action);
            });
        }
    }

    @Override
    public void visit(FieldDeclaration n, Action action) {
        ResolvedType staticType = ASTUtils.resolvedTypeDeclarationToResolvedType(n.findAncestor(ClassOrInterfaceDeclaration.class).orElseThrow().resolve());
        for (VariableDeclarator v : n.getVariables()) {
            String realName = getRealNameForFieldDeclaration(v);
            acceptAction(FIELD, realName, DECLARATION);
            graphNode.getLastVariableAction().setStaticType(staticType);
            Expression init = v.getInitializer().orElseGet(() -> ASTUtils.initializerForField(n));
            init.accept(this, action);
            definitionStack.push(init);
            acceptAction(FIELD, realName, DEFINITION);
            graphNode.getLastVariableAction().setStaticType(staticType);
            definitionStack.pop();
            if (v.getType().isClassOrInterfaceType())
                getLastDefinition().setTotallyDefinedMember(realName);
            v.accept(this, action);
        }
    }

    @Override
    public void visit(VariableDeclarator n, Action arg) {
        if (n.getInitializer().isPresent()) {
            groupActionsByRoot(graphNode);
            String realName = n.getNameAsString();
            if (n.resolve().isField() && !n.resolve().asField().isStatic())
                realName = "this." + realName;
            new ExpressionObjectTreeFinder(graphNode).handleVariableDeclarator(n, realName);
        }
    }

    @Override
    public void visit(CatchClause n, Action arg) {
        n.getParameter().accept(this, arg.or(DECLARATION));
        n.getBody().accept(this, arg);
    }

    @Override
    public void visit(Parameter n, Action arg) {
        acceptAction(PARAMETER, n.getNameAsString(), DECLARATION);
        graphNode.getLastVariableAction().setStaticType(n.getType().resolve());
        acceptActionNullDefinition(PARAMETER, n.getNameAsString());
        graphNode.getLastVariableAction().setStaticType(n.getType().resolve());
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
        if (visitCall(n, arg)) {
            acceptAction(FIELD, "this", DECLARATION);
            graphNode.getLastVariableAction().setStaticType(ASTUtils.resolvedTypeDeclarationToResolvedType(ASTUtils.getClassNode(n).resolve()));
            super.visit(n, arg);
        }
        // Regardless of whether it resolves or not, 'this' is defined
        acceptActionNullDefinition(FIELD, "this");
        graphNode.getLastVariableAction().setStaticType(ASTUtils.resolvedTypeDeclarationToResolvedType(ASTUtils.getClassNode(n).resolve()));
        // setup a connection between USE(-output-) and DEF(this)
        List<VariableAction> vaList = graphNode.getVariableActions();
        if (vaList.size() >= 5) { // call-super, DEC(this), USE(-output-), ret-super, DEF(this)
            VariableAction useOutput = vaList.get(vaList.size() - 3);
            VariableAction defThis = graphNode.getLastVariableAction();
            assert useOutput.isUsage() && useOutput.getName().equals(VARIABLE_NAME_OUTPUT);
            assert defThis.isDefinition() && defThis.getName().equals("this");
            defThis.asDefinition().setTotallyDefinedMember("this");
            ObjectTree.copyTargetTreeToSource(defThis.getObjectTree(), useOutput.getObjectTree(), "", "");
            useOutput.setPDGTreeConnectionTo(defThis, "", "");
        }
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
        if (call instanceof ExplicitConstructorInvocationStmt) {
            acceptAction(FIELD, "this", DECLARATION);
            graphNode.getLastVariableAction().setStaticType(ASTUtils.resolvedTypeDeclarationToResolvedType(((ExplicitConstructorInvocationStmt) call).findAncestor(ClassOrInterfaceDeclaration.class).orElseThrow().resolve()));
        }
        if (call instanceof MethodCallExpr && !((JavaParserMethodDeclaration) call.resolve()).isStatic()) {
            ActualIONode scopeIn = ActualIONode.createActualIn(call, "this", ((MethodCallExpr) call).getScope().orElse(null));
            graphNode.addSyntheticNode(scopeIn);
            realNodeStack.push(scopeIn);
            ASTUtils.getResolvableScope(call).ifPresentOrElse(
                    scope -> scope.accept(this, action),
                    () -> {
                        acceptAction(FIELD, "this", USE);
                        graphNode.getLastVariableAction().setStaticType(ASTUtils.resolvedTypeDeclarationToResolvedType(((MethodCallExpr) call).findAncestor(ClassOrInterfaceDeclaration.class).orElseThrow().resolve()));
                    });
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
        // A node defines -output-
        var fields = getFieldsForReturn(call);
        var def = new VariableAction.Definition(SYNTHETIC, VARIABLE_NAME_OUTPUT, graphNode,
                fields.map(tree -> (ObjectTree) tree.clone()).orElse(null));
        def.setTotallyDefinedMember(ROOT_NAME);
        var defMov = new VariableAction.Movable(def, CallNode.Return.create(call));
        defMov.setStaticType(ASTUtils.getCallResolvedType(call));
        graphNode.addVariableAction(defMov);
        // The container of the call uses -output-, unless the call is wrapped in an ExpressionStmt
        Optional<Node> parentNode = ((Node) call).getParentNode();
        if (parentNode.isEmpty() || !(parentNode.get() instanceof ExpressionStmt)) {
            graphNode.addVariableAction(new VariableAction.Usage(SYNTHETIC, VARIABLE_NAME_OUTPUT, graphNode,
                    fields.map(tree -> (ObjectTree) tree.clone()).orElse(null)));
            graphNode.getLastVariableAction().setStaticType(ASTUtils.getCallResolvedType(call));
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

    /** Prepends [declaring class name].this. to the name of the given variable declarator. */
    protected String getRealNameForFieldDeclaration(VariableDeclarator decl) {
        return "this." + decl.getNameAsString();
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
        } else if (n.isSuperExpr() || n.isThisExpr()) {
            return "this";
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
        return "this.";
    }

    /** Extracts the parent elements affected by each variable action (e.g. an action on a.x affects a).
     *  When multiple compatible actions (same root and action) are found, only one parent element is generated. */
    protected void groupActionsByRoot(GraphNode<?> graphNode) {
        VariableAction lastRootAction = null;
        for (int i = 0; i < graphNode.variableActions.size(); i++) {
            VariableAction action = graphNode.variableActions.get(i);
            if (action instanceof VariableAction.CallMarker ||
                    action.isDeclaration() || action.isRootAction()) {
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
            } else if (!action.rootMatches(lastRootAction)
                    || !actionTypeMatches(action, lastRootAction)) {
                // No match: add the root before the current element and update counter
                graphNode.variableActions.add(i, lastRootAction);
                i++;
                // generate our own root action
                lastRootAction = action.getRootAction();
            }
            lastRootAction.getObjectTree().addField(action.getName());
            graphNode.variableActions.remove(action);
            i--;
        }
        // Append the last root action if there is any!
        if (lastRootAction != null)
            graphNode.variableActions.add(lastRootAction);
    }

    /** Whether two variable actions perform the same action. */
    private static boolean actionTypeMatches(VariableAction a, VariableAction b) {
        return (a.isDeclaration() && b.isDeclaration()) ||
                (a.isDefinition() && b.isDefinition()) ||
                (a.isUsage() && b.isUsage());
    }

    /** Obtain the last variable action in the current graph node, cast to definition. */
    protected VariableAction.Definition getLastDefinition() {
        return graphNode.getLastVariableAction().asDefinition();
    }

    /** Set the static type of the last variable action in the current graph node to
     *  the static type of the given expression. */
    protected void vaSetStaticType(Expression e) {
        graphNode.getLastVariableAction().setStaticType(e.calculateResolvedType());
    }

    /** Given a graph node, modify the existing object trees in each variable action
     *  to include polymorphic nodes, according to the dynamic types that are included.
     *  <br>
     *  <b>Static variable actions are excluded from this process.</b>
     *  <br>
     *  Polymorphic nodes are only generated if (a) at least one of the dynamic types is
     *  contained in the class graph, (b) the polymorphic variable has fields and (c) there
     *  aren't any polymorphic nodes in the object tree already. */
    protected void generatePolyTrees(GraphNode<?> graphNode) {
        ClassGraph classGraph = ClassGraph.getInstance();
        for (VariableAction va : graphNode.getVariableActions()) {
            if (va.isStatic() || !va.hasObjectTree())
                continue;
            ObjectTree newTree = new ObjectTree(va.getName());
            polyUnit(va.getObjectTree(), newTree, va.getDynamicTypes(), classGraph);
            va.setObjectTree(newTree);
        }
    }

    /** Handle a single level of conversion between object tree without polymorphic nodes
     *  to new object tree with polymorphic nodes.
     *  @see #generatePolyTrees(GraphNode) */
    protected void polyUnit(ObjectTree oldOT, ObjectTree newOT, Set<ResolvedType> types, ClassGraph classGraph) {
        boolean skipPolyNodes = types.stream().noneMatch(classGraph::containsType) || oldOT.hasChildren() || oldOT.hasPoly();
        if (skipPolyNodes) {
            // Copy as-is
            newOT.addAll(oldOT);
        } else {
            // Copy with typing information
            for (ResolvedType rt : types) {
                boolean rtInGraph = classGraph.containsType(rt);
                ObjectTree typeRoot = newOT.addType(rt);
                // Insert type node and copy members over
                for (Map.Entry<String,ObjectTree> entry : oldOT.entrySet())
                    polyUnit(entry.getValue(),
                            typeRoot.addImmediateField(entry.getKey()),
                            rtInGraph ? dynamicTypesOf(rt, entry.getKey(), classGraph) : Collections.emptySet(),
                            classGraph);
            }
        }
    }

    /** Obtain the set of possible dynamic types of the given field within a given type.
     *  Doesn't take into account the CFG, only the class graph. */
    protected Set<ResolvedType> dynamicTypesOf(ResolvedType rt, String fieldName, ClassGraph classGraph) {
        Optional<FieldDeclaration> field = classGraph.findClassField(rt, fieldName);
        if (field.isEmpty())
            return Collections.emptySet();
        ResolvedType fieldType = field.get().getVariable(0).getType().resolve();
        if (!fieldType.isReferenceType() || !classGraph.containsType(fieldType))
            return Set.of(fieldType);
        return classGraph.subclassesOf(fieldType.asReferenceType()).stream()
                .map(ClassOrInterfaceDeclaration::resolve)
                .map(ASTUtils::resolvedTypeDeclarationToResolvedType)
                .collect(Collectors.toSet());
    }
}
