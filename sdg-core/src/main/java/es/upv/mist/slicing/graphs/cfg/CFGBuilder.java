package es.upv.mist.slicing.graphs.cfg;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.VariableAction;
import es.upv.mist.slicing.nodes.io.MethodExitNode;
import es.upv.mist.slicing.nodes.io.OutputNode;
import es.upv.mist.slicing.utils.ASTUtils;

import java.util.*;

/**
 * Populates a {@link CFG}, given one and an AST root node.
 * It accepts both {@link MethodDeclaration} and {@link ConstructorDeclaration}
 * as roots, as it disallows multiple methods. <br/>
 * <strong>Usage:</strong>
 * <ol>
 *     <li>Create a new {@link CFG}.</li>
 *     <li>Create a new {@link CFGBuilder}, passing the graph as argument.</li>
 *     <li>Accept the builder as a visitor of the declaration you
 *     want to analyse using {@link Node#accept(VoidVisitor, Object)}: {@code methodDecl.accept(builder, null)}</li>
 *     <li>Once the previous step is finished, the complete CFG is saved in
 *     the object created in the first step. <strong>The builder should be discarded
 *     and not reused.</strong></li>
 * </ol>
 */
public class CFGBuilder extends VoidVisitorAdapter<Void> {
    /** The name for the return value. This name should always be illegal as a Java variable. */
    public static final String VARIABLE_NAME_OUTPUT = "-output-";

    /** Stores the CFG representing the method analyzed. */
    protected final CFG graph;
    /** Nodes that haven't yet been connected to another one.
     * The next node will be the destination, they are the source. */
    protected final List<GraphNode<?>> hangingNodes = new LinkedList<>();
    /** Stack of hanging nodes, for temporary storage. */
    protected final Deque<List<GraphNode<?>>> hangingNodesStack = new LinkedList<>();
    /** Stack of break statements collected in various (nestable) breakable blocks. */
    protected final Deque<List<GraphNode<BreakStmt>>> breakStack = new LinkedList<>();
    /** Stack of continue statements collected in various (nestable) continuable blocks. */
    protected final Deque<List<GraphNode<ContinueStmt>>> continueStack = new LinkedList<>();
    /** Lists of labelled break statements, mapped according to their label. */
    protected final Map<SimpleName, List<GraphNode<BreakStmt>>> breakMap = new HashMap<>();
    /** Lists of labelled continue statements, mapped according to their label. */
    protected final Map<SimpleName, List<GraphNode<ContinueStmt>>> continueMap = new HashMap<>();
    /** Return statements that should be connected to the final node, if it is created at the end of the */
    protected final List<GraphNode<ReturnStmt>> returnList = new LinkedList<>();
    /** Stack of lists of hanging cases on switch statements */
    protected final Deque<List<GraphNode<SwitchEntry>>> switchEntriesStack = new LinkedList<>();

    protected CFGBuilder(CFG graph) {
        this.graph = graph;
    }

    /**
     * Creates and connects a GraphNode from the AST node, using {@link Node#toString()}
     * to create the graphNode's label.
     * @see #connectTo(Node, String)
     */
    protected <T extends Node> GraphNode<T> connectTo(T n) {
        return connectTo(n, n.toString());
    }

    /**
     * Create a new {@link GraphNode}, add it to the CFG and connect it
     * to the current chain (using {@link #hangingNodes}).
     * @param n The AST node that is represented by the result.
     * @param text The resulting node's label.
     * @param <T> The AST node's type
     * @return A new GraphNode that represents {@code n}.
     * @see #connectTo(GraphNode)
     */
    protected <T extends Node> GraphNode<T> connectTo(T n, String text) {
        GraphNode<T> dest = graph.addVertex(text, n);
        connectTo(dest);
        return dest;
    }

    /**
     * Connect the argument to the current chain (using {@link #hangingNodes}).
     * From each hanging node to the argument, a control flow arc will be placed.
     * The only hanging node after this call will be the argument.
     */
    protected void connectTo(GraphNode<?> node) {
        for (GraphNode<?> src : hangingNodes)
            graph.addControlFlowArc(src, node);
        clearHanging();
        hangingNodes.add(node);
    }

    /** Clears all lists with hanging nodes that are connected in {@link #connectTo(GraphNode)}.
     * Subclasses that add additional lists should override this method. */
    protected void clearHanging() {
        hangingNodes.clear();
    }

    /** Copies the contents of all hanging node lists to a stack for later usage. Each time this is called, a matching
     *  call to {@link #restoreHanging()} or {@link #dropHanging()} must occur, so that the stack is balanced */
    protected void saveHanging() {
        hangingNodesStack.push(List.copyOf(hangingNodes));
    }

    /** Restores the last stored hanging node list, without clearing them first.
     * This method shall only be called as a complement to {@link #saveHanging()}. */
    protected void restoreHanging() {
        hangingNodes.addAll(hangingNodesStack.pop());
    }

    /** Deletes the last stored hanging node list. This method should be called as a complement to
     * {@link #saveHanging()}, when there is no further use for the stored list. */
    protected void dropHanging() {
        hangingNodesStack.pop();
    }

    // ======================================================================
    // ========================== Normal AST nodes ==========================
    // ======================================================================

    @Override
    public void visit(ExpressionStmt expressionStmt, Void arg) {
        connectTo(expressionStmt);
        expressionStmt.getExpression().accept(this, arg);
    }

    @Override
    public void visit(IfStmt ifStmt, Void arg) {
        // *if* -> {then else} -> after
        GraphNode<?> cond = connectTo(ifStmt, String.format("if (%s)", ifStmt.getCondition()));
        ifStmt.getCondition().accept(this, arg);

        // if -> {*then* else} -> after
        ifStmt.getThenStmt().accept(this, arg);
        saveHanging();

        if (ifStmt.getElseStmt().isPresent()) {
            // if -> {then *else*} -> after
            clearHanging();
            hangingNodes.add(cond);
            ifStmt.getElseStmt().get().accept(this, arg);
            restoreHanging();
        } else {
            // if -> {then **} -> after
            hangingNodes.add(cond);
            dropHanging();
        }
        // if -> {then else} -> *after*
    }

    @Override
    public void visit(LabeledStmt n, Void arg) {
        breakMap.put(n.getLabel(), new LinkedList<>());
        continueMap.put(n.getLabel(), new LinkedList<>());
        super.visit(n, arg);
        hangingNodes.addAll(breakMap.remove(n.getLabel()));
        // Remove the label from the continue map; the list should have been emptied in the corresponding loop.
        if (!continueMap.remove(n.getLabel()).isEmpty())
            throw new IllegalStateException("Labeled loop has not cleared its list of continue statements!");
    }

    /** Add to {@link #hangingNodes} the labelled continue statements that correspond to this loop, if any.
     * As a side-effect, it clears the list of labelled statements, so that they can't accidentally be linked twice. */
    protected void hangLabelledContinue(Node loop) {
        Optional<Node> loopParent = loop.getParentNode();
        if (loopParent.isPresent() && loopParent.get() instanceof LabeledStmt) {
            SimpleName label = ((LabeledStmt) loopParent.get()).getLabel();
            if (continueMap.containsKey(label)) {
                List<GraphNode<ContinueStmt>> list = continueMap.get(label);
                hangingNodes.addAll(list);
                list.clear();
            }
        }
    }

    /** Checks whether or not a loop was empty, after its traversal, given its condition node. */
    protected boolean isEmptyLoop(GraphNode<?> condition) {
        return hangingNodes.size() == 1 && hangingNodes.get(0) == condition;
    }

    @Override
    public void visit(WhileStmt whileStmt, Void arg) {
        GraphNode<?> cond = connectTo(whileStmt, String.format("while (%s)", whileStmt.getCondition()));
        whileStmt.getCondition().accept(this, arg);
        breakStack.push(new LinkedList<>());
        continueStack.push(new LinkedList<>());

        whileStmt.getBody().accept(this, arg);

        hangingNodes.addAll(continueStack.pop());
        hangLabelledContinue(whileStmt);
        if (!isEmptyLoop(cond))
            connectTo(cond);
        hangingNodes.addAll(breakStack.pop());
    }

    @Override
    public void visit(DoStmt doStmt, Void arg) {
        breakStack.push(new LinkedList<>());
        continueStack.push(new LinkedList<>());

        GraphNode<?> cond = connectTo(doStmt, String.format("while (%s)", doStmt.getCondition()));
        doStmt.getCondition().accept(this, arg);

        doStmt.getBody().accept(this, arg);

        hangingNodes.addAll(continueStack.pop());
        hangLabelledContinue(doStmt);
        if (!isEmptyLoop(cond))
            connectTo(cond);
        hangingNodes.addAll(breakStack.pop());
    }

    @Override
    public void visit(ForStmt forStmt, Void arg) {
        breakStack.push(new LinkedList<>());
        continueStack.push(new LinkedList<>());

        // Initialization
        forStmt.getInitialization().forEach(n -> {
            connectTo(n);
            n.accept(this, arg);
        });

        // Condition
        Expression condition = forStmt.getCompare().orElse(new BooleanLiteralExpr(true));
        GraphNode<?> cond = connectTo(forStmt, String.format("for (;%s;)", condition));
        condition.accept(this, arg);

        // Body and update expressions
        forStmt.getBody().accept(this, arg);
        forStmt.getUpdate().forEach(n -> {
            connectTo(n);
            n.accept(this, arg);
        });

        // Condition if body contained anything
        hangingNodes.addAll(continueStack.pop());
        hangLabelledContinue(forStmt);
        if (!isEmptyLoop(cond))
            connectTo(cond);
        hangingNodes.addAll(breakStack.pop());
    }

    @Override
    public void visit(ForEachStmt forEachStmt, Void arg) {
        breakStack.push(new LinkedList<>());
        continueStack.push(new LinkedList<>());

        GraphNode<?> cond = connectTo(forEachStmt,
                String.format("for (%s : %s)", forEachStmt.getVariable(), forEachStmt.getIterable()));
        forEachStmt.getIterable().accept(this, arg);

        forEachStmt.getBody().accept(this, arg);

        hangingNodes.addAll(continueStack.pop());
        hangLabelledContinue(forEachStmt);
        if (!isEmptyLoop(cond))
            connectTo(cond);
        hangingNodes.addAll(breakStack.pop());
    }

    @Override
    public void visit(SwitchEntry entryStmt, Void arg) {
        // Case header (prev -> case EXPR)
        GraphNode<SwitchEntry> node = connectTo(entryStmt, entryStmt.getLabels().isNonEmpty() ?
                "case " + entryStmt.getLabels().stream()
                        .map(Node::toString)
                        .reduce((a, b) -> a + ", " + b)
                : "default");
        switchEntriesStack.peek().add(node);
        // Case body (case EXPR --> body)
        entryStmt.getStatements().accept(this, arg);
        // body --> next
    }

    @Override
    public void visit(SwitchStmt switchStmt, Void arg) {
        // Link previous statement to the switch's selector
        switchEntriesStack.push(new LinkedList<>());
        breakStack.push(new LinkedList<>());
        GraphNode<?> cond = connectTo(switchStmt, String.format("switch (%s)", switchStmt.getSelector()));
        switchStmt.getSelector().accept(this, arg);
        // expr --> each case (fallthrough by default, so case --> case too)
        for (SwitchEntry entry : switchStmt.getEntries()) {
            entry.accept(this, arg); // expr && prev case --> case --> next case
            hangingNodes.add(cond); // expr --> next case
        }
        // The next statement will be linked to:
        //		1. All break statements that broke from the switch (done with break section)
        // 		2. If the switch doesn't have a default statement, the switch's selector (already present)
        // 		3. If the last entry doesn't break, to the last statement (present already)
        // If the last case is a default case, remove the selector node from the list of nodes (see 2)
        if (ASTUtils.switchHasDefaultCase(switchStmt))
            hangingNodes.remove(cond);
        List<GraphNode<SwitchEntry>> entries = switchEntriesStack.pop();
        // End block and break section
        hangingNodes.addAll(breakStack.pop());
    }

    @Override
    public void visit(BreakStmt breakStmt, Void arg) {
        GraphNode<BreakStmt> node = connectTo(breakStmt);
        if (breakStmt.getLabel().isPresent())
            breakMap.get(breakStmt.getLabel().get()).add(node);
        else
            breakStack.peek().add(node);
        clearHanging();
    }

    @Override
    public void visit(ContinueStmt continueStmt, Void arg) {
        GraphNode<ContinueStmt> node = connectTo(continueStmt);
        if (continueStmt.getLabel().isPresent())
            continueMap.get(continueStmt.getLabel().get()).add(node);
        else
            continueStack.peek().add(node);
        clearHanging();
    }

    @Override
    public void visit(ReturnStmt returnStmt, Void arg) {
        GraphNode<ReturnStmt> node = connectTo(returnStmt);
        returnStmt.getExpression().ifPresent(n -> {
            n.accept(this, arg);
            node.addDefinedVariable(new NameExpr(VARIABLE_NAME_OUTPUT));
        });
        returnList.add(node);
        clearHanging();
    }

    @Override
    public void visit(ExplicitConstructorInvocationStmt n, Void arg) {
        connectTo(n);
    }

    // ======================================================================
    // ============================ Declarations ============================
    // ======================================================================

    @Override
    public void visit(MethodDeclaration n, Void arg) {
        visitCallableDeclaration(n, arg);
    }

    @Override
    public void visit(ConstructorDeclaration n, Void arg) {
        visitCallableDeclaration(n, arg);
    }

    /**
     * <strong>This method should be visited once per instance.</strong>
     * Creates the CFG of a {@link CallableDeclaration}. The root node contains the declaration,
     * and from there the body is visited. Finally, all return and hanging statements are connected
     * to the exit node.
     */
    protected void visitCallableDeclaration(CallableDeclaration<?> callableDeclaration, Void arg) {
        graph.buildRootNode(callableDeclaration);
        hangingNodes.add(graph.getRootNode());

        ASTUtils.getCallableBody(callableDeclaration).accept(this, arg);
        returnList.stream().filter(node -> !hangingNodes.contains(node)).forEach(hangingNodes::add);

        MethodExitNode exit = new MethodExitNode(callableDeclaration);
        graph.addVertex(exit);
        addMethodOutput(callableDeclaration, exit);
        connectTo(exit);
    }

    /** Adds the output variable to the exit node, if appropriate to the declaration.
     * @see #VARIABLE_NAME_OUTPUT */
    protected void addMethodOutput(CallableDeclaration<?> callableDeclaration, GraphNode<?> exit) {
        if (!(callableDeclaration instanceof MethodDeclaration) || !((MethodDeclaration) callableDeclaration).getType().isVoidType()) {
            VariableAction usage = new VariableAction.Usage(new NameExpr(VARIABLE_NAME_OUTPUT), exit);
            exit.addMovableVariable(new VariableAction.Movable(usage, OutputNode.create(callableDeclaration)));
        }
    }
}
