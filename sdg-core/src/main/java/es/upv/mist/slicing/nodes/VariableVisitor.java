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
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
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
        DECLARATION(true),
        DEFINITION(true),
        USE(true),
        OPT_DEFINITION(false),
        OPT_USE(false);

        /** Whether the action is performed in all executions of the node. */
        private final boolean always;

        Action(boolean always) {
            this.always = always;
        }

        /** Join multiple actions into one. */
        public Action or(Action action) {
            if (action == DECLARATION || this == DECLARATION)
                return DECLARATION;
            if (action == DEFINITION || this == DEFINITION || this == OPT_DEFINITION)
                return always ? DEFINITION : OPT_DEFINITION;
            return this;
        }

        public Action andUse() {
            return always ? USE : OPT_USE;
        }

        public boolean isUse() {
            return this == USE || this == OPT_USE;
        }

        public boolean isDefinition() {
            return this == DEFINITION || this == OPT_DEFINITION;
        }

        public boolean isDeclaration() {
            return this == DECLARATION;
        }

        public boolean isOptional() {
            return !always;
        }

        public Action optional() {
            switch (this) {
                case OPT_DEFINITION:
                case OPT_USE:
                    return this;
                case USE:
                    return OPT_USE;
                case DEFINITION:
                    return OPT_DEFINITION;
                case DECLARATION:
                default:
                    throw new UnsupportedOperationException("Action unsupported");
            }
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
            VariableAction va = acceptAction(DeclarationType.valueOf(n), realName, action);
            va.addExpression(n);
            va.setStaticType(ASTUtils.resolvedTypeOfCurrentClass(n));
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
            VariableAction va;
            if (realName.equals(n.toString())) {
                va = acceptAction(n, action);
                va.setStaticType(scope.calculateResolvedType());
            } else {
                va = acceptAction(DeclarationType.valueOf(n), realName, action);
                va.setStaticType(ASTUtils.resolvedTypeOfCurrentClass(n));
            }
            // Register both expressions, as ExpressionObjectTreeFinder will search based on scope.
            va.addExpression(scope);
            va.addExpression(n);
        } else {
            n.getScope().accept(this, action); // this.call().c.d
        }
    }

    protected VariableAction acceptAction(Expression n, Action action) {
        VariableAction va = acceptAction(DeclarationType.valueOf(n), getRealName(n), action, false);
        va.setStaticType(n.calculateResolvedType());
        va.addExpression(n);
        return va;
    }

    protected VariableAction acceptAction(DeclarationType declarationType, String realName, Action action) {
        return acceptAction(declarationType, realName, action, false);
    }

    protected VariableAction acceptAction(Expression n, String realName, Action action) {
        VariableAction va = acceptAction(DeclarationType.valueOf(n), realName, action, false);
        va.setStaticType(n.calculateResolvedType());
        va.addExpression(n);
        return va;
    }

    protected VariableAction acceptActionNullDefinition(DeclarationType declarationType, String realName) {
        return acceptAction(declarationType, realName, DEFINITION, true);
    }

    protected VariableAction acceptAction(DeclarationType declarationType, String realName, Action action, boolean canDefBeNull) {
        VariableAction va;
        if (action.isDeclaration()) {
            va = new VariableAction.Declaration(declarationType, realName, graphNode);
        } else if (action.isDefinition()) {
            assert !definitionStack.isEmpty() || canDefBeNull;
            va = new VariableAction.Definition(declarationType, realName, graphNode, definitionStack.peek());
        } else if (action.isUse()) {
            va = new VariableAction.Usage(declarationType, realName, graphNode);
        } else {
            throw new UnsupportedOperationException();
        }
        va.setOptional(action.isOptional());
        if (!realNodeStack.isEmpty()) {
            va = new VariableAction.Movable(va, realNodeStack.peek());
        }
        graphNode.addVariableAction(va);
        return va;
    }

    // Partially traversed (only expressions that may contain variables are traversed)
    @Override
    public void visit(CastExpr n, Action action) {
        n.getExpression().accept(this, action);
    }

    // Modified traversal (there may be variable definitions or declarations)
    @Override
    public void visit(ArrayAccessExpr n, Action arg) {
        if (arg.isDefinition()) {
            n.getName().accept(this, arg);
            n.getIndex().accept(this, arg.andUse());
        } else if (arg.isUse()) {
            super.visit(n, arg);
        } else {
            throw new IllegalStateException("Array accesses cannot be declared");
        }
    }

    @Override
    public void visit(ReturnStmt n, Action arg) {
        super.visit(n, arg);
        if (n.getExpression().isPresent()) {
            definitionStack.push(n.getExpression().get());
            VariableAction va = acceptAction(SYNTHETIC, VARIABLE_NAME_OUTPUT, DEFINITION);
            va.setStaticType(n.getExpression().get().calculateResolvedType());
            definitionStack.pop();
            va.asDefinition().setTotallyDefinedMember(ROOT_NAME);
        }
    }

    @Override
    public void visit(ThrowStmt n, Action arg) {
        super.visit(n, arg);
        definitionStack.push(n.getExpression());
        VariableAction va = acceptAction(SYNTHETIC, ACTIVE_EXCEPTION_VARIABLE, DEFINITION);
        ResolvedReferenceType type = n.getExpression().calculateResolvedType().asReferenceType();
        va.setStaticType(type);
        definitionStack.pop();
        va.getObjectTree().addAll(ClassGraph.getInstance().generateObjectTreeFor(type));
        new ExpressionObjectTreeFinder(graphNode).locateAndMarkTransferenceToRoot(n.getExpression(), -1);
    }

    @Override
    public void visit(ForEachStmt n, Action action) {
        n.getIterable().accept(this, USE);
        for (VariableDeclarator variable : n.getVariable().getVariables()) {
            VariableAction vaDec = acceptAction(LOCAL_VARIABLE, variable.getNameAsString(), DECLARATION);
            vaDec.setStaticType(variable.getType().resolve());
            // ForEach initializes to each value of the iterable, but that expression is not available.
            VariableAction vaDef = acceptActionNullDefinition(LOCAL_VARIABLE, variable.getNameAsString());
            vaDef.setStaticType(variable.getType().resolve());
        }
    }

    @Override
    public void visit(ConditionalExpr n, Action arg) {
        n.getCondition().accept(this, arg);
        n.getThenExpr().accept(this, arg.optional());
        n.getElseExpr().accept(this, arg.optional());
    }

    @Override
    public void visit(BinaryExpr n, Action arg) {
        n.getLeft().accept(this, arg);
        switch (n.getOperator()) {
            case OR:
            case AND:
                n.getRight().accept(this, arg.optional());
                break;
            default:
                n.getRight().accept(this, arg);
                break;
        }
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
                    VariableAction va = acceptAction(nameExpr, realName, action.or(DEFINITION));
                    va.asDefinition().setTotallyDefinedMember(realName);
                    realNameWithoutRootList.add("");
                } else {
                    String root = ObjectTree.removeFields(realName);
                    VariableAction va = acceptAction(DeclarationType.valueOf(nameExpr), root, action.or(DEFINITION));
                    va.setStaticType(ASTUtils.resolvedTypeOfCurrentClass(nameExpr));
                    va.getObjectTree().addField(realName);
                    va.asDefinition().setTotallyDefinedMember(realName);
                    va.addExpression(nameExpr);
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
                VariableAction va;
                if (root.equals(scope.toString()))
                    va = acceptAction(scope, root, action.or(DEFINITION));
                else {
                    va = acceptAction(FIELD, root, action.or(DEFINITION));
                    va.setStaticType(ASTUtils.resolvedTypeOfCurrentClass(fieldAccessExpr));
                }
                // Register both expressions, as ExpressionObjectTreeFinder will search based on scope.
                va.addExpression(fieldAccessExpr);
                va.addExpression(scope);
                definitionStack.pop();
                va.asDefinition().setTotallyDefinedMember(realName);
                va.getObjectTree().addField(realName);
                realNameWithoutRootList.add(ObjectTree.removeRoot(realName));
            }

            @Override
            public void visit(ArrayAccessExpr n, Void arg) {
                n.getName().accept(this, arg);
                n.getIndex().accept(VariableVisitor.this, action.andUse());
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
            VariableAction vaDec = acceptAction(LOCAL_VARIABLE, v.getNameAsString(), DECLARATION);
            vaDec.setStaticType(v.getType().resolve());
            vaDec.addExpression(n);
            v.getInitializer().ifPresent(init -> {
                init.accept(this, action);
                definitionStack.push(init);
                VariableAction vaDef = acceptAction(LOCAL_VARIABLE, v.getNameAsString(), DEFINITION);
                vaDef.addExpression(n);
                vaDef.setStaticType(v.getType().resolve());
                definitionStack.pop();
                if (v.getType().isClassOrInterfaceType())
                    vaDef.asDefinition().setTotallyDefinedMember(v.getNameAsString());
                v.accept(this, action);
            });
        }
    }

    @Override
    public void visit(FieldDeclaration n, Action action) {
        ResolvedType staticType = ASTUtils.resolvedTypeOfCurrentClass(n);
        for (VariableDeclarator v : n.getVariables()) {
            String realName = getRealNameForFieldDeclaration(v);
            VariableAction vaDec = acceptAction(FIELD, realName, DECLARATION);
            vaDec.setStaticType(staticType);
            Expression init = v.getInitializer().orElseGet(() -> ASTUtils.initializerForField(n));
            init.accept(this, action);
            definitionStack.push(init);
            VariableAction vaDef = acceptAction(FIELD, realName, DEFINITION);
            vaDef.setStaticType(staticType);
            definitionStack.pop();
            if (v.getType().isClassOrInterfaceType())
                vaDef.asDefinition().setTotallyDefinedMember(realName);
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
        VariableAction vaDec = acceptAction(PARAMETER, n.getNameAsString(), DECLARATION);
        vaDec.setStaticType(n.getType().resolve());
        VariableAction vaDef = acceptActionNullDefinition(PARAMETER, n.getNameAsString());
        vaDef.setStaticType(n.getType().resolve());
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
        boolean visitCall = visitCall(n, arg);
        if (visitCall) {
            VariableAction va = acceptAction(FIELD, "this", DECLARATION);
            va.setStaticType(ASTUtils.resolvedTypeOfCurrentClass(n));
            super.visit(n, arg);
        }
        // Regardless of whether it resolves or not, 'this' is defined
        VariableAction defThis = acceptActionNullDefinition(FIELD, "this");
        defThis.setStaticType(ASTUtils.resolvedTypeOfCurrentClass(n));
        // setup a connection between USE(-output-) and DEF(this)
        List<VariableAction> vaList = graphNode.getVariableActions();
        if (!visitCall) { // call-super, DEC(this), USE(-output-), ret-super, DEF(this)
            assert vaList.size() >= 5;
            VariableAction useOutput = vaList.get(vaList.size() - 3);
            assert useOutput.isUsage() && useOutput.getName().equals(VARIABLE_NAME_OUTPUT);
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
            VariableAction va = acceptAction(FIELD, "this", DECLARATION);
            va.setStaticType(ASTUtils.resolvedTypeOfCurrentClass((ExplicitConstructorInvocationStmt) call));
        }
        if (call instanceof MethodCallExpr && !((JavaParserMethodDeclaration) call.resolve()).isStatic()) {
            ActualIONode scopeIn = ActualIONode.createActualIn(call, "this", ((MethodCallExpr) call).getScope().orElse(null));
            graphNode.addSyntheticNode(scopeIn);
            realNodeStack.push(scopeIn);
            ASTUtils.getResolvableScope(call).ifPresentOrElse(
                    scope -> scope.accept(this, action),
                    () -> {
                        VariableAction va = acceptAction(FIELD, "this", action);
                        va.setStaticType(ASTUtils.resolvedTypeOfCurrentClass((MethodCallExpr) call));
                    });
            // Generate -scope-in- action, so that InterproceduralUsageFinder does not need to do so.
            VariableAction.Definition def = new VariableAction.Definition(VariableAction.DeclarationType.SYNTHETIC, "-scope-in-", graphNode);
            VariableAction.Movable movDef = new VariableAction.Movable(def, scopeIn);
            graphNode.addVariableAction(movDef);
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
            // Generate -arg-in- action, so that InterproceduralUsageFinder does not need to do so.
            VariableAction.Definition def = new VariableAction.Definition(VariableAction.DeclarationType.SYNTHETIC, "-arg-in-", graphNode);
            VariableAction.Movable movDef = new VariableAction.Movable(def, actualIn);
            graphNode.addVariableAction(movDef);
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
            VariableAction use = new VariableAction.Usage(SYNTHETIC, VARIABLE_NAME_OUTPUT, graphNode,
                    fields.map(tree -> (ObjectTree) tree.clone()).orElse(null));
            graphNode.addVariableAction(use);
            use.setStaticType(ASTUtils.getCallResolvedType(call));
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
                return getNamePrefix(n.asNameExpr()) + n;
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
            lastRootAction.copyExpressions(action);
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
    protected void polyUnit(ObjectTree oldOT, ObjectTree newOT, Set<ClassGraph.ClassVertex<?>> types, ClassGraph classGraph) {
        boolean skipPolyNodes = types.stream().noneMatch(classGraph::containsVertex) || !oldOT.hasChildren() || oldOT.hasPoly();
        if (skipPolyNodes) {
            // Copy as-is
            newOT.addAll(oldOT);
        } else {
            // Copy with typing information
            for (ClassGraph.ClassVertex<?> rt : types) {
                boolean rtInGraph = classGraph.containsVertex(rt);
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
    protected Set<ClassGraph.ClassVertex<?>> dynamicTypesOf(ClassGraph.ClassVertex<?> rt, String fieldName, ClassGraph classGraph) {
        Optional<FieldDeclaration> field = classGraph.findClassField(rt, fieldName);
        if (field.isEmpty())
            return Collections.emptySet();
        ResolvedType fieldType = field.get().getVariable(0).getType().resolve();
        if (!fieldType.isReferenceType() || !classGraph.containsType(fieldType))
            return Set.of(classGraph.vertexOf(fieldType));
        return classGraph.subclassesOf(fieldType.asReferenceType())
                .collect(Collectors.toSet());
    }
}
