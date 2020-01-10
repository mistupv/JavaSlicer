package tfm.visitors.cfg;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.graphs.CFGGraph;
import tfm.nodes.GraphNode;
import tfm.utils.ASTUtils;

import java.util.*;

public class CFGBuilder extends VoidVisitorAdapter<Void> {
    /** Stores whether a method visit has started, to avoid mapping multiple methods onto one CFG. */
    private boolean begunMethod = false;

    /** Stores the CFG representing the method analyzed. */
    private final CFGGraph graph;
    /** Nodes that haven't yet been connected to another one.
     * The next node will be the destination, they are the source. */
    private final List<GraphNode<?>> hangingNodes = new LinkedList<>();
    /** Stack of break statements collected in various (nestable) breakable blocks. */
    private final Stack<List<GraphNode<BreakStmt>>> breakStack = new Stack<>();
    /** Stack of continue statements collected in various (nestable) continuable blocks. */
    private final Stack<List<GraphNode<ContinueStmt>>> continueStack = new Stack<>();
    /** Lists of labelled break statements, mapped according to their label. */
    private final Map<SimpleName, List<GraphNode<BreakStmt>>> breakMap = new HashMap<>();
    /** Lists of labelled continue statements, mapped according to their label. */
    private final Map<SimpleName, List<GraphNode<ContinueStmt>>> continueMap = new HashMap<>();
    /** Return statements that should be connected to the final node, if it is created at the end of the  */
    private final List<GraphNode<ReturnStmt>> returnList = new LinkedList<>();
    /** Stack of lists of hanging cases on switch statements */
    private final Stack<List<GraphNode<SwitchEntryStmt>>> switchEntriesStack = new Stack<>();

    public CFGBuilder(CFGGraph graph) {
        this.graph = graph;
    }

    private <T extends Node> GraphNode<T> connectTo(T n) {
        return connectTo(n, n.toString());
    }

    private <T extends Node> GraphNode<T> connectTo(T n, String text) {
        GraphNode<T> dest = graph.addNode(text, n);
        connectTo(dest);
        return dest;
    }

    private void connectTo(GraphNode<?> node) {
        for (GraphNode<?> src : hangingNodes)
            graph.addControlFlowEdge(src, node);
        hangingNodes.clear();
        hangingNodes.add(node);
    }

    @Override
    public void visit(ExpressionStmt expressionStmt, Void arg) {
        connectTo(expressionStmt);
    }

    @Override
    public void visit(IfStmt ifStmt, Void arg) {
        // *if* -> {then else} -> after
        GraphNode<?> cond = connectTo(ifStmt, String.format("if (%s)", ifStmt.getCondition()));

        // if -> {*then* else} -> after
        ifStmt.getThenStmt().accept(this, arg);
        List<GraphNode<?>> hangingThenNodes = new LinkedList<>(hangingNodes);

        if (ifStmt.getElseStmt().isPresent()) {
            // if -> {then *else*} -> after
            hangingNodes.clear();
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
        assert continueMap.remove(n.getLabel()).isEmpty();
    }

    private void hangLabelledContinueStmts(Node loopParent) {
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
        forStmt.getInitialization().forEach(expression -> new ExpressionStmt(expression).accept(this, arg));

        // Condition
        Expression condition = forStmt.getCompare().orElse(new BooleanLiteralExpr(true));
        GraphNode<?> cond = connectTo(forStmt, String.format("for (;%s;)", condition));

        // Body and update expressions
        forStmt.getBody().accept(this, arg);
        forStmt.getUpdate().forEach(e -> new ExpressionStmt(e).accept(this, arg));

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

        forEachStmt.getBody().accept(this, arg);

        hangingNodes.addAll(continueStack.pop());
        hangLabelledContinueStmts(forEachStmt.getParentNode().orElse(null));
        if (hangingNodes.size() != 1 || hangingNodes.get(0) != cond)
            connectTo(cond);
        hangingNodes.addAll(breakStack.pop());
    }

    /** Switch entry, considered part of the condition of the switch. */
    @Override
    public void visit(SwitchEntryStmt entryStmt, Void arg) {
        // Case header (prev -> case EXPR)
        GraphNode<SwitchEntryStmt> node;
        if (entryStmt.getLabel().isPresent()) {
            node = connectTo(entryStmt, "case " + entryStmt.getLabel().get());
        } else {
            node = connectTo(entryStmt, "default");
        }
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
        switchEntriesStack.pop();
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
        hangingNodes.clear();
    }

    @Override
    public void visit(ContinueStmt continueStmt, Void arg) {
        GraphNode<ContinueStmt> node = connectTo(continueStmt);
        if (continueStmt.getLabel().isPresent())
            continueMap.get(continueStmt.getLabel().get()).add(node);
        else
            continueStack.peek().add(node);
        hangingNodes.clear();
    }

    @Override
    public void visit(ReturnStmt returnStmt, Void arg) {
        GraphNode<ReturnStmt> node = connectTo(returnStmt);
        returnList.add(node);
        hangingNodes.clear();
    }

    @Override
    public void visit(MethodDeclaration methodDeclaration, Void arg) {
        if (begunMethod)
            throw new IllegalStateException("CFG is only allowed for one method, not multiple!");
        begunMethod = true;

        hangingNodes.add(graph.getRootNode());
        super.visit(methodDeclaration, arg);
        returnList.stream().filter(node -> !hangingNodes.contains(node)).forEach(hangingNodes::add);
        connectTo(new EmptyStmt(), "Exit");
    }
}
