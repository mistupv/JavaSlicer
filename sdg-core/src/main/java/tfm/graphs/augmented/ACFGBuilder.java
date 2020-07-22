package tfm.graphs.augmented;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitor;
import tfm.graphs.cfg.CFGBuilder;
import tfm.nodes.GraphNode;
import tfm.nodes.TypeNodeFactory;
import tfm.nodes.type.NodeType;
import tfm.utils.ASTUtils;

import java.util.LinkedList;
import java.util.List;

/**
 * Populates a {@link ACFG}, given one and an AST root node.
 * For now it only accepts {@link MethodDeclaration} as roots, as it disallows
 * multiple methods.
 * <br/>
 * <b>Usage:</b>
 * <ol>
 *     <li>Create a new {@link ACFG}.</li>
 *     <li>Create a new {@link ACFGBuilder}, passing the graph as argument.</li>
 *     <li>Accept the builder as a visitor of the {@link MethodDeclaration} you
 *     want to analyse using {@link Node#accept(VoidVisitor, Object)}: {@code methodDecl.accept(builder, null)}</li>
 *     <li>Once the previous step is finished, the complete CFG is saved in
 *     the object created in the first step. <emph>The builder should be discarded
 *     and not reused.</emph></li>
 * </ol>
 */
public class ACFGBuilder extends CFGBuilder {
    /** Same as {@link ACFGBuilder#hangingNodes}, but to be connected as non-executable edges. */
    protected final List<GraphNode<?>> nonExecHangingNodes = new LinkedList<>();

    protected ACFGBuilder(ACFG graph) {
        super(graph);
    }

    @Override
    protected void connectTo(GraphNode<?> node) {
        for (GraphNode<?> src : nonExecHangingNodes)
            ((ACFG) graph).addNonExecutableControlFlowEdge(src, node);
        super.connectTo(node);
    }

    @Override
    protected void clearHanging() {
        super.clearHanging();
        nonExecHangingNodes.clear();
    }

    @Override
    public void visit(IfStmt ifStmt, Void arg) {
        // *if* -> {then else} -> after
        GraphNode<?> cond = connectTo(ifStmt, String.format("if (%s)", ifStmt.getCondition()));
        ifStmt.getCondition().accept(this, arg);

        // if -> {*then* else} -> after
        ifStmt.getThenStmt().accept(this, arg);
        List<GraphNode<?>> hangingThenNodes = new LinkedList<>(hangingNodes);
        List<GraphNode<?>> hangingThenFalseNodes = new LinkedList<>(nonExecHangingNodes);

        if (ifStmt.getElseStmt().isPresent()) {
            // if -> {then *else*} -> after
            clearHanging();
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
        else if (!nonExecHangingNodes.isEmpty())
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
        else if (!nonExecHangingNodes.isEmpty())
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
        forEachStmt.getIterable().accept(this, arg);

        forEachStmt.getBody().accept(this, arg);

        hangingNodes.addAll(continueStack.pop());
        hangLabelledContinueStmts(forEachStmt.getParentNode().orElse(null));
        if (hangingNodes.size() != 1 || hangingNodes.get(0) != cond)
            connectTo(cond);
        else if (!nonExecHangingNodes.isEmpty())
            connectTo(cond);
        hangingNodes.addAll(breakStack.pop());
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
            clearHanging();
            entries.remove(def);
            nonExecHangingNodes.addAll(entries);
            connectTo(def);
            clearHanging();
            hangingNodes.addAll(aux);
            nonExecHangingNodes.add(def);
            nonExecHangingNodes.addAll(aux2);
        } else {
            nonExecHangingNodes.addAll(entries);
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
        clearHanging();
        nonExecHangingNodes.add(node);
    }

    @Override
    public void visit(ContinueStmt continueStmt, Void arg) {
        GraphNode<ContinueStmt> node = connectTo(continueStmt);
        if (continueStmt.getLabel().isPresent())
            continueMap.get(continueStmt.getLabel().get()).add(node);
        else
            continueStack.peek().add(node);
        clearHanging();
        nonExecHangingNodes.add(node);
    }

    @Override
    public void visit(ReturnStmt returnStmt, Void arg) {
        GraphNode<ReturnStmt> node = connectTo(returnStmt);
        node.addDefinedVariable(new NameExpr(VARIABLE_NAME_OUTPUT));
        returnStmt.getExpression().ifPresent(n -> n.accept(this, arg));
        returnList.add(node);
        clearHanging();
        nonExecHangingNodes.add(node);
    }

    @Override
    public void visit(MethodDeclaration methodDeclaration, Void arg) {
        if (graph.getRootNode().isPresent())
            throw new IllegalStateException("CFG is only allowed for one method, not multiple!");
        if (!methodDeclaration.getBody().isPresent())
            throw new IllegalStateException("The method must have a body!");

        graph.buildRootNode("ENTER " + methodDeclaration.getNameAsString(), methodDeclaration, TypeNodeFactory.fromType(NodeType.METHOD_ENTER));

        hangingNodes.add(graph.getRootNode().get());
        for (Parameter param : methodDeclaration.getParameters())
            connectTo(addFormalInGraphNode(methodDeclaration, param));
        methodDeclaration.getBody().get().accept(this, arg);
        returnList.stream().filter(node -> !hangingNodes.contains(node)).forEach(hangingNodes::add);
        for (Parameter param : methodDeclaration.getParameters())
            connectTo(addFormalOutGraphNode(methodDeclaration, param));
        nonExecHangingNodes.add(graph.getRootNode().get());
        connectTo(graph.addNode("Exit", new EmptyStmt(), TypeNodeFactory.fromType(NodeType.METHOD_EXIT)));
    }
}
