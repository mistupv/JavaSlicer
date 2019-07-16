package tfm.visitors;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.graphs.CFGGraph;
import tfm.nodes.CFGNode;
import tfm.utils.Utils;

import java.util.*;
import java.util.stream.Collectors;

public class CFGVisitor extends VoidVisitorAdapter<Void> {

    private CFGGraph graph;

    private Queue<CFGNode> lastParentNodes;
    private List<CFGNode> bodyBreaks;

    public CFGVisitor(CFGGraph graph) {
        this.graph = graph;
        this.lastParentNodes = Collections.asLifoQueue(
                new ArrayDeque<>(
                        Collections.singletonList(graph.getRootNode())
                )
        );

        this.bodyBreaks = new ArrayList<>();
    }

    @Override
    public void visit(ExpressionStmt expressionStmt, Void arg) {
        String expression = expressionStmt.toString().replace("\"", "\\\"");

        CFGNode nextNode = addNodeAndArcs(expression, expressionStmt);

        lastParentNodes.add(nextNode);
    }

//    @Override
//    public void visit(VariableDeclarationExpr variableDeclarationExpr, Void arg) {
//        CFGNode<String> nextNode = addNodeAndArcs(variableDeclarationExpr.toString());
//
//        lastParentNodes.add(nextNode);
//
//        Logger.log(variableDeclarationExpr);
//
//        super.visit(variableDeclarationExpr, arg);
//    }

    @Override
    public void visit(IfStmt ifStmt, Void arg) {
        CFGNode ifCondition = addNodeAndArcs(
                String.format("if (%s)", ifStmt.getCondition().toString()),
                ifStmt
        );

        lastParentNodes.add(ifCondition);

        // Visit "then"
        ifStmt.getThenStmt().accept(this, arg);

        Queue<CFGNode> lastThenNodes = new ArrayDeque<>(lastParentNodes);

        if (ifStmt.hasElseBranch()) {
            lastParentNodes.clear();
            lastParentNodes.add(ifCondition); // Set if nodes as root

            ifStmt.getElseStmt().get().accept(this, arg);

            lastParentNodes.addAll(lastThenNodes);
        } else {
            lastParentNodes.add(ifCondition);
        }
    }

    @Override
    public void visit(WhileStmt whileStmt, Void arg) {
        CFGNode whileCondition = addNodeAndArcs(
                String.format("while (%s)", whileStmt.getCondition().toString()),
                whileStmt
        );

        lastParentNodes.add(whileCondition);

        whileStmt.getBody().accept(this, arg);

        while (!lastParentNodes.isEmpty()) {
            graph.addControlFlowEdge(lastParentNodes.poll(), whileCondition);
        }

        lastParentNodes.add(whileCondition);
        lastParentNodes.addAll(bodyBreaks);
        bodyBreaks.clear();
    }

    @Override
    public void visit(DoStmt doStmt, Void arg) {
        BlockStmt body = Utils.blockWrapper(doStmt.getBody());

        body.accept(this, arg);

        CFGNode doWhileNode = addNodeAndArcs(
                String.format("while (%s)", doStmt.getCondition()),
                doStmt
        );

        if (!body.isEmpty()) {
            Statement firstBodyStatement = body.getStatement(0);

            graph.findNodeByASTNode(firstBodyStatement)
                    .ifPresent(node -> graph.addControlFlowEdge(doWhileNode, node));
        }

        lastParentNodes.add(doWhileNode);
        lastParentNodes.addAll(bodyBreaks);
        bodyBreaks.clear();
    }

    @Override
    public void visit(ForStmt forStmt, Void arg) {
        String inizialization = forStmt.getInitialization().stream()
                .map(Node::toString)
                .collect(Collectors.joining(","));

        String update = forStmt.getUpdate().stream()
                .map(Node::toString)
                .collect(Collectors.joining(","));

        Expression comparison = forStmt.getCompare().orElse(new BooleanLiteralExpr(true));
//
//        forStmt.getInitialization().forEach(expression -> new ExpressionStmt(expression).accept(this, null));

        CFGNode forNode = addNodeAndArcs(
                String.format("for (%s;%s;%s)", inizialization, comparison, update),
                forStmt
        );

        lastParentNodes.add(forNode);

        BlockStmt body = Utils.blockWrapper(forStmt.getBody());

//        forStmt.getUpdate().forEach(body::addStatement);

        body.accept(this, arg);

        while (!lastParentNodes.isEmpty()) {
            graph.addControlFlowEdge(lastParentNodes.poll(), forNode);
        }

        lastParentNodes.add(forNode);
        lastParentNodes.addAll(bodyBreaks);
        bodyBreaks.clear();
    }

    @Override
    public void visit(ForEachStmt forEachStmt, Void arg) {
        CFGNode foreachNode = addNodeAndArcs(
                String.format("for (%s : %s)", forEachStmt.getVariable(), forEachStmt.getIterable()),
                forEachStmt
        );

        lastParentNodes.add(foreachNode);

        forEachStmt.getBody().accept(this, arg);

        while (!lastParentNodes.isEmpty()) {
            graph.addControlFlowEdge(lastParentNodes.poll(), foreachNode);
        }

        lastParentNodes.add(foreachNode);
        lastParentNodes.addAll(bodyBreaks);
        bodyBreaks.clear();
    }

    @Override
    public void visit(SwitchStmt switchStmt, Void arg) {
        CFGNode switchNode = addNodeAndArcs(
                String.format("switch (%s)", switchStmt.getSelector()),
                switchStmt
        );

        lastParentNodes.add(switchNode);

        List<CFGNode> allEntryBreaks = new ArrayList<>();

        List<CFGNode> lastEntryStatementsWithNoBreak = new ArrayList<>();

        switchStmt.getEntries().forEach(switchEntryStmt -> {
            String label = switchEntryStmt.getLabel()
                    .map(expression -> "case " + expression)
                    .orElse("default");

            CFGNode switchEntryNode = addNodeAndArcs(label, switchEntryStmt);

            lastParentNodes.add(switchEntryNode);
            lastParentNodes.addAll(lastEntryStatementsWithNoBreak);
            lastEntryStatementsWithNoBreak.clear();

            switchEntryStmt.getStatements().accept(this, null);

            if (!bodyBreaks.isEmpty()) { // means it has break
                allEntryBreaks.addAll(bodyBreaks); // save breaks of entry

                lastParentNodes.clear();
                lastParentNodes.add(switchEntryNode); // Set switch as the only parent

                bodyBreaks.clear(); // Clear breaks
            } else {
                lastEntryStatementsWithNoBreak.addAll(lastParentNodes);
                lastParentNodes.clear();
                lastParentNodes.add(switchEntryNode);
            }
        });

        lastParentNodes.addAll(allEntryBreaks);
    }

    @Override
    public void visit(BreakStmt breakStmt, Void arg) {
        bodyBreaks.addAll(lastParentNodes);
    }

    @Override
    public void visit(ContinueStmt continueStmt, Void arg) {
        Statement continuableStatement = Utils.findFirstAncestorStatementFrom(continueStmt, Utils::isLoop);

        CFGNode continuableNode = graph.findNodeByASTNode(continuableStatement).get();

        lastParentNodes.forEach(parentNode -> graph.addControlFlowEdge(parentNode, continuableNode));
    }

    @Override
    public void visit(MethodDeclaration methodDeclaration, Void arg) {
        if (!lastParentNodes.isEmpty() && Objects.equals(lastParentNodes.peek().getData(), "Stop")) {
            throw new IllegalStateException("CFG is only allowed for one method, not multiple!");
        }

        super.visit(methodDeclaration, arg);

        lastParentNodes.add(addNodeAndArcs("Stop", new EmptyStmt()));
    }

    private CFGNode addNodeAndArcs(String nodeData, Statement statement) {
        CFGNode node = graph.addNode(nodeData, statement);

        CFGNode parent = lastParentNodes.poll(); // ALWAYS exists a parent
        graph.addControlFlowEdge(parent, node);

        while (!lastParentNodes.isEmpty()) {
            parent = lastParentNodes.poll();
            graph.addControlFlowEdge(parent, node);
        }

        return node;
    }


}
