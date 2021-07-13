package es.upv.mist.slicing.graphs.jsysdg;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.graphs.ClassGraph;
import es.upv.mist.slicing.graphs.ExpressionObjectTreeFinder;
import es.upv.mist.slicing.graphs.cfg.CFGBuilder;
import es.upv.mist.slicing.graphs.exceptionsensitive.ESCFG;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.VariableAction;
import es.upv.mist.slicing.nodes.io.MethodExitNode;
import es.upv.mist.slicing.utils.ASTUtils;
import es.upv.mist.slicing.utils.NodeHashSet;
import es.upv.mist.slicing.utils.NodeNotFoundException;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An SDG that is tailored for Java, including a class graph, inheritance,
 * polymorphism and other features.
 */
public class JSysCFG extends ESCFG {
    @Override
    public void build(CallableDeclaration<?> declaration) {
        throw new UnsupportedOperationException("Use build(CallableDeclaration, ClassGraph, Set<ConstructorDeclaration>)");
    }

    public void build(CallableDeclaration<?> declaration, Set<ConstructorDeclaration> implicitConstructors, ClassGraph classGraph) {
        Builder builder = (Builder) newCFGBuilder();
        builder.implicitDeclaration = implicitConstructors.contains(declaration);
        builder.classGraph = classGraph;
        declaration.accept(builder, null);
        vertexSet().stream()
                .filter(Predicate.not(GraphNode::isImplicitInstruction))
                .filter(gn -> builder.methodInsertedInstructions.contains(gn.getAstNode()))
                .forEach(GraphNode::markAsImplicit);
        // Verify that it has been built
        exitNode = vertexSet().stream().filter(MethodExitNode.class::isInstance).findFirst()
                .orElseThrow(() -> new IllegalStateException("Built graph has no exit node!"));
        built = true;
    }

    @Override
    protected CFGBuilder newCFGBuilder() {
        return new Builder(this);
    }

    /** Given a usage of an object member, find the last definitions of that member.
     *  This method returns a list of variable actions, where the caller can find the member. */
    public List<VariableAction> findLastDefinitionOfObjectMember(VariableAction usage, String member) {
        return findLastVarActionsFrom(usage, def -> def.isDefinition() && def.hasTreeMember(member));
    }

    /** Given a usage of a primitive variable, find the last def actions that affect it. */
    public List<VariableAction> findLastDefinitionOfPrimitive(VariableAction usage) {
        return findLastVarActionsFrom(usage, VariableAction::isDefinition);
    }

    /** Given the usage of a root object variable, find the last root definitions that affect it. */
    public List<VariableAction> findLastDefinitionOfObjectRoot(VariableAction usage) {
        return findLastVarActionsFrom(usage, VariableAction::isDefinition);
    }

    /** Given a field declaration, locate all definitions that affect the given member. */
    public List<VariableAction> findAllFutureObjectDefinitionsFor(VariableAction action) {
        List<VariableAction> list = new LinkedList<>();
        Predicate<VariableAction> filter = a -> a.isDefinition() && a.getName().equals("this") && a.hasPolyTreeMember(action.getName());
        findAllFutureVarActionsFor(new HashSet<>(), list, action.getGraphNode(), action, filter);
        return list;
    }

    /** Locate variable actions that match the given filter, starting in {@code currentNode}, at variable action
     *  {@code var} and searching forwards through the control-flow graph. The resulting variable actions are
     *  placed in the given argument. This search does not stop when an action in that control-flow branch is found. */
    protected void findAllFutureVarActionsFor(Set<GraphNode<?>> visited, List<VariableAction> result,
                                            GraphNode<?> currentNode, VariableAction var,
                                            Predicate<VariableAction> filter) {
        // Base case
        if (visited.contains(currentNode))
            return;
        visited.add(currentNode);

        Stream<VariableAction> stream = currentNode.getVariableActions().stream();
        if (var.getGraphNode().equals(currentNode))
            stream = stream.dropWhile(va -> va != var);
        stream.filter(filter).forEach(result::add);

        // always traverse forwards!
        for (Arc arc : outgoingEdgesOf(currentNode))
            if (arc.isExecutableControlFlowArc())
                findAllFutureVarActionsFor(visited, result, getEdgeTarget(arc), var, filter);
    }

    /** Given an action that defines a member, locates the previous total definition that gave
     *  it value. */
    public List<VariableAction> findLastTotalDefinitionOf(VariableAction action, String member) {
        return findLastVarActionsFrom(action, def ->
                (def.isDeclaration() && def.hasTreeMember(member))
                || (def.isDefinition() && def.asDefinition().isTotallyDefinedMember(member)));
    }

    /** Given a definition of a given member, locate all definitions of the same object until a definition
     *  containing the given member is found (not including that last one). If the member is found in the
     *  given definition, it will return a list with only the given definition. */
    public List<VariableAction> findNextObjectDefinitionsFor(VariableAction definition, String member) {
        if (!this.containsVertex(definition.getGraphNode()))
            throw new NodeNotFoundException(definition.getGraphNode(), this);
        if (definition.hasTreeMember(member))
            return List.of(definition);
        List<VariableAction> list = new LinkedList<>();
        findNextVarActionsFor(new HashSet<>(), list, definition.getGraphNode(), definition, VariableAction::isDefinition, member);
        return list;
    }

    /** Locate variable actions that match the given filter and variable name, starting in {@code currentNode},
     *  at variable action {@code var} and searching backwards. The resulting variable actions are placed in
     *  the given argument. This search stops after finding a matching action in each branch. */
    protected boolean findNextVarActionsFor(Set<GraphNode<?>> visited, List<VariableAction> result,
                                            GraphNode<?> currentNode, VariableAction var,
                                            Predicate<VariableAction> filter, String memberName) {
        // Base case
        if (visited.contains(currentNode))
            return true;
        visited.add(currentNode);

        Stream<VariableAction> stream = currentNode.getVariableActions().stream();
        if (var.getGraphNode().equals(currentNode))
            stream = stream.dropWhile(va -> va != var);
        List<VariableAction> list = stream.filter(var::matches).filter(filter).collect(Collectors.toList());
        if (!list.isEmpty()) {
            boolean found = false;
            for (VariableAction variableAction : list) {
                if (!variableAction.isOptional() && variableAction.hasTreeMember(memberName)) {
                    found = true;
                    break;
                }
                result.add(variableAction);
            }
            if (found)
                return true;
        }

        // Not found: traverse forwards!
        boolean allBranches = !outgoingEdgesOf(currentNode).isEmpty();
        for (Arc arc : outgoingEdgesOf(currentNode))
            if (arc.isExecutableControlFlowArc())
                allBranches &= findNextVarActionsFor(visited, result, getEdgeTarget(arc), var, filter, memberName);
        return allBranches;
    }

    public class Builder extends ESCFG.Builder {
        protected ClassGraph classGraph;
        /** List of implicit instructions inserted explicitly in this CFG.
         *  They should be included in the graph as ImplicitNodes. */
        protected NodeHashSet<Node> methodInsertedInstructions = new NodeHashSet<>();
        /** Whether we are building a CFG for an implicit method or not. */
        protected boolean implicitDeclaration = false;

        protected Builder(JSysCFG jSysCFG) {
            super(JSysCFG.this);
            assert jSysCFG == JSysCFG.this;
        }

        @Override
        protected <T extends Node> GraphNode<T> connectTo(T n, String text) {
            GraphNode<T> dest;
            dest = new GraphNode<>(text, n);
            if (methodInsertedInstructions.contains(n) ||
                    (implicitDeclaration && !(n instanceof FieldDeclaration)))
                dest.markAsImplicit();
            addVertex(dest);
            connectTo(dest);
            return dest;
        }

        @Override
        public void visit(ExplicitConstructorInvocationStmt n, Void arg) {
            stmtStack.push(n);
            // 1. Connect to the following statements
            connectTo(n);
            // 2. Insert dynamic class code (only for super())
            if (!n.isThis())
                ASTUtils.getTypeInit(n.findAncestor(TypeDeclaration.class).orElseThrow(), false)
                        .forEach(node -> node.accept(this, arg));
            // 3. Handle exceptions
            super.visitCallForExceptions(n);
            stmtStack.pop();
        }

        @Override
        public void visit(FieldDeclaration n, Void arg){
            connectTo(n);
            super.visit(n,arg);
        }

        @Override
        public void visit(ConstructorDeclaration n, Void arg) {
            // Insert call to super() if it is implicit.
            if (ASTUtils.shouldInsertExplicitConstructorInvocation(n)) {
                var superCall = new ExplicitConstructorInvocationStmt(null, null, false, null, new NodeList<>());
                methodInsertedInstructions.add(superCall);
                n.getBody().addStatement(0, superCall);
            }
            // insert return this; at the end of the constructor
            var returnThis = new ReturnStmt();
            methodInsertedInstructions.add(returnThis);
            n.getBody().addStatement(returnThis);
            // modify every return statement so that it returns 'this'
            modifyAllReturnExpr(n, ThisExpr::new);
            // Perform the same task as previous graphs.
            super.visit(n, arg);
            // restore return statements
            modifyAllReturnExpr(n, () -> null);
            // Convert enter/exit nodes to implicit if appropriate
            if (implicitDeclaration) {
                getRootNode().markAsImplicit();
                vertexSet().stream()
                        .filter(MethodExitNode.class::isInstance)
                        .forEach(GraphNode::markAsImplicit);
            }
        }

        @Override
        protected void buildEnter(CallableDeclaration<?> callableDeclaration) {
            super.buildEnter(callableDeclaration);
            // enums have no super(), so the implicitly inserted instructions
            // must be placed after the root node
            if (callableDeclaration.isConstructorDeclaration()) {
                ConstructorDeclaration cd = callableDeclaration.asConstructorDeclaration();
                TypeDeclaration<?> type = cd.findAncestor(TypeDeclaration.class).orElseThrow();
                if (!ASTUtils.shouldInsertExplicitConstructorInvocation(cd) &&
                        type.isEnumDeclaration() &&
                        ASTUtils.shouldInsertDynamicInitInEnum(cd)) {
                    ASTUtils.getTypeInit(type, false).forEach(n -> n.accept(this, null));
                }
            }
        }

        /**
         * Sets the expression for all return statements contained in its argument.
         * @param node The AST to search for return statements.
         * @param expressionSupplier The expression to be set.
         */
        protected void modifyAllReturnExpr(Node node, Supplier<Expression> expressionSupplier) {
            node.accept(new ModifierVisitor<Void>() {
                @Override
                public Visitable visit(ReturnStmt n, Void arg) {
                    n.setExpression(expressionSupplier.get());
                    return n;
                }
            }, null);
        }

        @Override
        protected void addMethodOutput(CallableDeclaration<?> callableDeclaration, GraphNode<?> exit) {
            super.addMethodOutput(callableDeclaration, exit);
            for (VariableAction action : exit.getVariableActions()) {
                if (action.getName().equals(VARIABLE_NAME_OUTPUT)) {
                    expandOutputVariable(callableDeclaration, action);
                    break;
                }
            }
        }

        /**
         * Generates the object tree for the output of a declaration, and copies that same tree
         * to each of its return statements. It also sets the connection between them, to be applied
         * later.
         * @param callableDeclaration The root of the declarations' AST.
         * @param useOutput The variable at the method's exit that uses -output-.
         */
        protected void expandOutputVariable(CallableDeclaration<?> callableDeclaration, VariableAction useOutput) {
            // Generate the full tree for the method's returned type (static)
            var fields = classGraph.generateObjectTreeForReturnOf(callableDeclaration);
            if (fields.isPresent()) {
                // Insert tree into the OutputNode
                useOutput.getObjectTree().addAll(fields.get());
                // Insert tree into GraphNode<ReturnStmt> nodes, the last action is always DEF(-output-)
                vertexSet().stream()
                        .filter(gn -> gn.getAstNode() instanceof ReturnStmt)
                        .map(GraphNode::getLastVariableAction)
                        .map(VariableAction::getObjectTree)
                        .forEach(tree -> tree.addAll(fields.get()));
            }
            // Generate the assignment trees and prepare for linking
            vertexSet().stream()
                    .filter(gn -> gn.getAstNode() instanceof ReturnStmt)
                    .forEach(gn -> {
                        Expression expr = ((ReturnStmt) gn.getAstNode()).getExpression().orElseThrow();
                        new ExpressionObjectTreeFinder(gn).locateAndMarkTransferenceToRoot(expr, -1);
                    });
        }
    }
}
