package tfm.graphs.cfg;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.nodes.FormalIONode;
import tfm.nodes.GraphNode;
import tfm.nodes.TypeNodeFactory;
import tfm.nodes.type.NodeType;
import tfm.utils.ASTUtils;

import java.util.*;

/**
 * Populates a {@link CFG}, given one and an AST root node.
 * For now it only accepts {@link MethodDeclaration} as roots, as it disallows
 * multiple methods.
 * <br/>
 * <b>Usage:</b>
 * <ol>
 *     <li>Create a new {@link CFG}.</li>
 *     <li>Create a new {@link CFGBuilder}, passing the graph as argument.</li>
 *     <li>Accept the builder as a visitor of the {@link MethodDeclaration} you
 *     want to analyse using {@link Node#accept(VoidVisitor, Object)}: {@code methodDecl.accept(builder, null)}</li>
 *     <li>Once the previous step is finished, the complete CFG is saved in
 *     the object created in the first step. <emph>The builder should be discarded
 *     and not reused.</emph></li>
 * </ol>
 */
public class CFGBuilder extends VoidVisitorAdapter<Void> {
    public static final String VARIABLE_NAME_OUTPUT = "-output-";

    /**
     * Stores the CFG representing the method analyzed.
     */
    protected final CFG graph;
    /**
     * Nodes that haven't yet been connected to another one.
     * The next node will be the destination, they are the source.
     */
    protected final List<GraphNode<?>> hangingNodes = new LinkedList<>();
    /**
     * Stack of break statements collected in various (nestable) breakable blocks.
     */
    protected final Deque<List<GraphNode<BreakStmt>>> breakStack = new LinkedList<>();
    /**
     * Stack of continue statements collected in various (nestable) continuable blocks.
     */
    protected final Deque<List<GraphNode<ContinueStmt>>> continueStack = new LinkedList<>();
    /**
     * Lists of labelled break statements, mapped according to their label.
     */
    protected final Map<SimpleName, List<GraphNode<BreakStmt>>> breakMap = new HashMap<>();
    /**
     * Lists of labelled continue statements, mapped according to their label.
     */
    protected final Map<SimpleName, List<GraphNode<ContinueStmt>>> continueMap = new HashMap<>();
    /**
     * Return statements that should be connected to the final node, if it is created at the end of the
     */
    protected final List<GraphNode<ReturnStmt>> returnList = new LinkedList<>();
    /**
     * Stack of lists of hanging cases on switch statements
     */
    protected final Deque<List<GraphNode<SwitchEntryStmt>>> switchEntriesStack = new LinkedList<>();

    protected CFGBuilder(CFG graph) {
        this.graph = graph;
    }

    protected <T extends Node> GraphNode<T> connectTo(T n) {
        return connectTo(n, n.toString());
    }

    protected <T extends Node> GraphNode<T> connectTo(T n, String text) {
        GraphNode<T> dest = graph.addNode(text, n);
        connectTo(dest);
        return dest;
    }

    protected void connectTo(GraphNode<?> node) {
        for (GraphNode<?> src : hangingNodes)
            graph.addControlFlowEdge(src, node);
        clearHanging();
        hangingNodes.add(node);
    }

    protected void clearHanging() {
        hangingNodes.clear();
    }

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
        List<GraphNode<?>> hangingThenNodes = new LinkedList<>(hangingNodes);

        if (ifStmt.getElseStmt().isPresent()) {
            // if -> {then *else*} -> after
            clearHanging();
            hangingNodes.add(cond);
            ifStmt.getElseStmt().get().accept(this, arg);
            hangingNodes.addAll(hangingThenNodes);
        } else {
            // if -> {then **} -> after
            hangingNodes.add(cond);
        }
        // if -> {then else} -> *after*
    }

    @Override
    public void visit(LabeledStmt n, Void arg) {
        breakMap.put(n.getLabel(), new LinkedList<>());
        continueMap.put(n.getLabel(), new LinkedList<>());
        super.visit(n, arg);
        hangingNodes.addAll(breakMap.remove(n.getLabel()));
        // Remove the label from the continue map; the list should have been emptied
        // in the corresponding loop.
        if (!continueMap.remove(n.getLabel()).isEmpty())
            throw new IllegalStateException("Labeled loop has not cleared its list of continue statements!");
    }

    protected void hangLabelledContinueStmts(Node loopParent) {
        if (loopParent instanceof LabeledStmt) {
            SimpleName label = ((LabeledStmt) loopParent).getLabel();
            if (continueMap.containsKey(label)) {
                List<GraphNode<ContinueStmt>> list = continueMap.get(label);
                hangingNodes.addAll(list);
                list.clear();
            }
        }
    }

    @Override
    public void visit(WhileStmt whileStmt, Void arg) {
        GraphNode<?> cond = connectTo(whileStmt, String.format("while (%s)", whileStmt.getCondition()));
        whileStmt.getCondition().accept(this, arg);
        breakStack.push(new LinkedList<>());
        continueStack.push(new LinkedList<>());

        whileStmt.getBody().accept(this, arg);

        hangingNodes.addAll(continueStack.pop());
        hangLabelledContinueStmts(whileStmt.getParentNode().orElse(null));
        // Loop contains anything
        if (hangingNodes.size() != 1 || hangingNodes.get(0) != cond)
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
        hangLabelledContinueStmts(doStmt.getParentNode().orElse(null));
        // Loop contains anything
        if (hangingNodes.size() != 1 || hangingNodes.get(0) != cond)
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
        hangLabelledContinueStmts(forStmt.getParentNode().orElse(null));
        if ((hangingNodes.size()) != 1 || hangingNodes.get(0) != cond)
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
        hangLabelledContinueStmts(forEachStmt.getParentNode().orElse(null));
        if (hangingNodes.size() != 1 || hangingNodes.get(0) != cond)
            connectTo(cond);
        hangingNodes.addAll(breakStack.pop());
    }

    /**
     * Switch entry, considered part of the condition of the switch.
     */
    @Override
    public void visit(SwitchEntryStmt entryStmt, Void arg) {
        // Case header (prev -> case EXPR)
        GraphNode<SwitchEntryStmt> node = connectTo(entryStmt, entryStmt.getLabel().isPresent() ?
                "case " + entryStmt.getLabel().get() : "default");
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
        for (SwitchEntryStmt entry : switchStmt.getEntries()) {
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
        List<GraphNode<SwitchEntryStmt>> entries = switchEntriesStack.pop();
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
    public void visit(MethodDeclaration methodDeclaration, Void arg) {
        // Sanity checks
        if (graph.getRootNode().isPresent())
            throw new IllegalStateException("CFG is only allowed for one method, not multiple!");
        if (methodDeclaration.getBody().isEmpty())
            throw new IllegalStateException("The method must have a body! Abstract methods have no CFG");

        // Create the root node
        graph.buildRootNode(
                "ENTER " + methodDeclaration.getNameAsString(),
                methodDeclaration,
                TypeNodeFactory.fromType(NodeType.METHOD_ENTER));
        hangingNodes.add(graph.getRootNode().get());
        // Create and connect formal-in nodes sequentially
        for (Parameter param : methodDeclaration.getParameters())
            connectTo(addFormalInGraphNode(methodDeclaration, param));
        // Visit the body of the method
        methodDeclaration.getBody().get().accept(this, arg);
        // Append all return statements (without repetition)
        returnList.stream().filter(node -> !hangingNodes.contains(node)).forEach(hangingNodes::add);

        createAndConnectFormalOutNodes(methodDeclaration);
        // Create and connect the exit node
        connectTo(graph.addNode("Exit", new EmptyStmt(), TypeNodeFactory.fromType(NodeType.METHOD_EXIT)));
    }

    protected void createAndConnectFormalOutNodes(MethodDeclaration methodDeclaration) {
        createAndConnectFormalOutNodes(methodDeclaration, true);
    }

    /** If the method declaration has a return type, create an "OUTPUT" node and connect it. */
    protected void createAndConnectFormalOutNodes(MethodDeclaration methodDeclaration, boolean createOutput) {
        for (Parameter param : methodDeclaration.getParameters())
            connectTo(addFormalOutGraphNode(methodDeclaration, param));
        if (createOutput && !methodDeclaration.getType().equals(new VoidType())) {
            GraphNode<?> outputNode = graph.addNode("output", new EmptyStmt(), TypeNodeFactory.fromType(NodeType.METHOD_OUTPUT));
            outputNode.addUsedVariable(new NameExpr(VARIABLE_NAME_OUTPUT));
            connectTo(outputNode);
        }
    }

    protected FormalIONode addFormalInGraphNode(MethodDeclaration methodDeclaration, Parameter param) {
        FormalIONode node = FormalIONode.createFormalIn(methodDeclaration, param);
        graph.addNode(node);
        return node;
    }

    protected FormalIONode addFormalOutGraphNode(MethodDeclaration methodDeclaration, Parameter param) {
        FormalIONode node = FormalIONode.createFormalOut(methodDeclaration, param);
        graph.addNode(node);
        return node;
    }
}
