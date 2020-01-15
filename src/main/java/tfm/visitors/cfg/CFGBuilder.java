package tfm.visitors.cfg;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.graphs.CFG;
import tfm.graphs.CFG.ACFG;
import tfm.nodes.GraphNode;
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
 * <b>Configuration:</b> this builder can be used to generate both the CFG and ACFG,
 * depending on the type of the empty graph.
 * Before performing the visit you should set the configuration to the one you prefer.
 */
public class CFGBuilder extends VoidVisitorAdapter<Void> {
    /** Stores whether a method visit has started, to avoid mapping multiple methods onto one CFG. */
    private boolean begunMethod = false;

    /** Stores the CFG representing the method analyzed. */
    private final CFG graph;
    /** Nodes that haven't yet been connected to another one.
     * The next node will be the destination, they are the source. */
    private final List<GraphNode<?>> hangingNodes = new LinkedList<>();
    /** Same as {@link CFGBuilder#hangingNodes}, but to be connected as non-executable edges. */
    private final List<GraphNode<?>> nonExecHangingNodes = new LinkedList<>();
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

    public CFGBuilder(CFG graph) {
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
        assert nonExecHangingNodes.isEmpty() || graph instanceof ACFG;
        for (GraphNode<?> src : nonExecHangingNodes)
            ((ACFG) graph).addNonExecutableControlFlowEdge(src, node);
        hangingNodes.clear();
        nonExecHangingNodes.clear();
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
        List<GraphNode<?>> hangingThenFalseNodes = new LinkedList<>(nonExecHangingNodes);

        if (ifStmt.getElseStmt().isPresent()) {
            // if -> {then *else*} -> after
            hangingNodes.clear();
            nonExecHangingNodes.clear();
            hangingNodes.add(cond);
            ifStmt.getElseStmt().get().accept(this, arg);
            hangingNodes.addAll(hangingThenNodes);
            nonExecHangingNodes.addAll(hangingThenFalseNodes);
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
        else if (!nonExecHangingNodes.isEmpty())
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
        else if (!nonExecHangingNodes.isEmpty())
            connectTo(cond);
        hangingNodes.addAll(breakStack.pop());
    }

    @Override
    public void visit(ForStmt forStmt, Void arg) {
        breakStack.push(new LinkedList<>());
        continueStack.push(new LinkedList<>());

        // Initialization
        forStmt.getInitialization().forEach(this::connectTo);

        // Condition
        Expression condition = forStmt.getCompare().orElse(new BooleanLiteralExpr(true));
        GraphNode<?> cond = connectTo(forStmt, String.format("for (;%s;)", condition));

        // Body and update expressions
        forStmt.getBody().accept(this, arg);
        forStmt.getUpdate().forEach(this::connectTo);

        // Condition if body contained anything
        hangingNodes.addAll(continueStack.pop());
        hangLabelledContinueStmts(forStmt.getParentNode().orElse(null));
        if ((hangingNodes.size()) != 1 || hangingNodes.get(0) != cond)
            connectTo(cond);
        else if (!nonExecHangingNodes.isEmpty())
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
        else if (!nonExecHangingNodes.isEmpty())
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
        List<GraphNode<SwitchEntryStmt>> entries = switchEntriesStack.pop();
        if (graph instanceof ACFG) {
            GraphNode<SwitchEntryStmt> def = null;
            for (GraphNode<SwitchEntryStmt> entry : entries) {
                if (!entry.getAstNode().getLabel().isPresent()) {
                    def = entry;
                    break;
                }
            }
            if (def != null) {
                List<GraphNode<?>> aux = new LinkedList<>(hangingNodes);
                List<GraphNode<?>> aux2 = new LinkedList<>(nonExecHangingNodes);
                hangingNodes.clear();
                nonExecHangingNodes.clear();
                entries.remove(def);
                nonExecHangingNodes.addAll(entries);
                connectTo(def);
                hangingNodes.clear();
                hangingNodes.addAll(aux);
                nonExecHangingNodes.add(def);
                nonExecHangingNodes.addAll(aux2);
            } else {
                nonExecHangingNodes.addAll(entries);
            }
        }
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
        if (graph instanceof ACFG)
            nonExecHangingNodes.add(node);
    }

    @Override
    public void visit(ContinueStmt continueStmt, Void arg) {
        GraphNode<ContinueStmt> node = connectTo(continueStmt);
        if (continueStmt.getLabel().isPresent())
            continueMap.get(continueStmt.getLabel().get()).add(node);
        else
            continueStack.peek().add(node);
        hangingNodes.clear();
        if (graph instanceof ACFG)
            nonExecHangingNodes.add(node);
    }

    @Override
    public void visit(ReturnStmt returnStmt, Void arg) {
        GraphNode<ReturnStmt> node = connectTo(returnStmt);
        returnList.add(node);
        hangingNodes.clear();
        if (graph instanceof ACFG)
            nonExecHangingNodes.add(node);
    }

    @Override
    public void visit(MethodDeclaration methodDeclaration, Void arg) {
        if (begunMethod)
            throw new IllegalStateException("CFG is only allowed for one method, not multiple!");
        begunMethod = true;

        hangingNodes.add(graph.getRootNode());
        super.visit(methodDeclaration, arg);
        returnList.stream().filter(node -> !hangingNodes.contains(node)).forEach(hangingNodes::add);
        if (graph instanceof ACFG)
            nonExecHangingNodes.add(graph.getRootNode());
        connectTo(new EmptyStmt(), "Exit");
    }
}
