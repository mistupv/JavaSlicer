package tfm.graphlib.visitors;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.graphlib.graphs.CFGGraph;
import tfm.graphlib.nodes.CFGVertex;

import java.util.*;

public class CFGVisitor extends VoidVisitorAdapter<Void> {

    private CFGGraph graph;

    private Queue<CFGVertex> lastParentNodes;

    public CFGVisitor(CFGGraph graph) {
        this.graph = graph;
        this.lastParentNodes = Collections.asLifoQueue(
                new ArrayDeque<>(
                        Collections.singletonList((CFGVertex) graph.getRootVertex())
                )
        );
    }

    @Override
    public void visit(ExpressionStmt expressionStmt, Void arg) {
        CFGVertex nextNode = addNodeAndArcs(expressionStmt.toString());

        lastParentNodes.add(nextNode);

        System.out.println(expressionStmt);

        super.visit(expressionStmt, arg);
    }

//    @Override
//    public void visit(VariableDeclarationExpr variableDeclarationExpr, Void arg) {
//        CFGVertex<String> nextNode = addNodeAndArcs(variableDeclarationExpr.toString());
//
//        lastParentNodes.add(nextNode);
//
//        System.out.println(variableDeclarationExpr);
//
//        super.visit(variableDeclarationExpr, arg);
//    }

    @Override
    public void visit(IfStmt ifStmt, Void arg) {
        CFGVertex ifCondition = addNodeAndArcs(
                String.format("if (%s)", ifStmt.getCondition().toString())
        );

        lastParentNodes.add(ifCondition);

        // Visit "then"
        super.visit(blockStmtWrapper(ifStmt.getThenStmt()), arg);

        Queue<CFGVertex> lastThenNodes = new ArrayDeque<>(lastParentNodes);

        if (ifStmt.hasElseBranch()) {
            lastParentNodes.clear();
            lastParentNodes.add(ifCondition); // Set if nodes as root

            super.visit(ifStmt.getElseStmt().get().asBlockStmt(), arg);

            lastParentNodes.addAll(lastThenNodes);
        } else {
            lastParentNodes.add(ifCondition);
        }
    }

    @Override
    public void visit(WhileStmt whileStmt, Void arg) {
        CFGVertex whileCondition = addNodeAndArcs(
                String.format("while (%s)", whileStmt.getCondition().toString())
        );

        lastParentNodes.add(whileCondition);

        super.visit(whileStmt.getBody().asBlockStmt(), arg);

        while (!lastParentNodes.isEmpty()) {
            graph.addControlFlowEdge(lastParentNodes.poll(), whileCondition);
        }

        lastParentNodes.add(whileCondition);
    }

    @Override
    public void visit(ForStmt forStmt, Void arg) {
        forStmt.getInitialization().forEach(expression -> new ExpressionStmt(expression).accept(this, null));

        BlockStmt blockStatement = blockStmtWrapper(forStmt.getBody());

        forStmt.getUpdate().forEach(blockStatement::addStatement);

        Expression comparison = forStmt.getCompare().orElse(new BooleanLiteralExpr(true));

        visit(new WhileStmt(comparison, blockStatement), null);
    }

    @Override
    public void visit(MethodDeclaration methodDeclaration, Void arg) {
        super.visit(methodDeclaration, arg);

        addNodeAndArcs("Stop");
    }

    private CFGVertex addNodeAndArcs(String nodeData) {
        CFGVertex node = graph.addVertex(nodeData);

        CFGVertex parent = lastParentNodes.poll(); // ALWAYS exists a parent
        graph.addControlFlowEdge(parent, node);

        while (!lastParentNodes.isEmpty()) {
            parent = lastParentNodes.poll();
            graph.addControlFlowEdge(parent, node);
        }

        return node;
    }

    private BlockStmt blockStmtWrapper(Statement node) {
        if (node.isBlockStmt()) {
            return (BlockStmt) node;
        }

        NodeList<Statement> nodeList = new NodeList<>();
        nodeList.add(node);
        return new BlockStmt(nodeList);
    }
}
