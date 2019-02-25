package tfm.jacosro.visitors;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.jacosro.graphs.CFGGraph;
import tfm.jacosro.nodes.CFGNode;

import java.util.*;

public class CFGVisitor extends VoidVisitorAdapter<Void> {

    private CFGGraph<String> graph;

    private Queue<CFGNode<String>> lastParentNodes;

    public CFGVisitor(CFGGraph<String> graph) {
        this.graph = graph;
        this.lastParentNodes = Collections.asLifoQueue(new ArrayDeque<>(Collections.singletonList(graph.getRoot())));
    }

    @Override
    public void visit(ExpressionStmt expressionStmt, Void arg) {
        CFGNode<String> nextNode = addNodeAndArcs(expressionStmt.toString());

        lastParentNodes.add(nextNode);

        System.out.println(expressionStmt);

        super.visit(expressionStmt, arg);
    }

    @Override
    public void visit(IfStmt ifStmt, Void arg) {
        CFGNode<String> ifCondition = addNodeAndArcs(
                String.format("if (%s)", ifStmt.getCondition().toString())
        );

        lastParentNodes.add(ifCondition);

        // Visit "then"
        super.visit(ifStmt.getThenStmt().asBlockStmt(), arg);

        Queue<CFGNode<String>> lastThenNodes = new ArrayDeque<>(lastParentNodes);

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
        CFGNode<String> whileCondition = addNodeAndArcs(
                String.format("while (%s)", whileStmt.getCondition().toString())
        );

        lastParentNodes.add(whileCondition);

        super.visit(whileStmt.getBody().asBlockStmt(), arg);

        while (!lastParentNodes.isEmpty()) {
            lastParentNodes.poll().controlFlowArcTo(whileCondition);
        }

        lastParentNodes.add(whileCondition);
    }

    @Override
    public void visit(MethodDeclaration methodDeclaration, Void arg) {
        super.visit(methodDeclaration, arg);

        addNodeAndArcs("Stop");
    }

    private CFGNode<String> addNodeAndArcs(String nodeData) {
        CFGNode<String> node = graph.addNode(nodeData);
        CFGNode<String> parent = lastParentNodes.poll(); // ALWAYS exists a parent
        graph.addControlFlowArc(parent, node);

        while (!lastParentNodes.isEmpty()) {
            parent = lastParentNodes.poll();
            graph.addControlFlowArc(parent, node);
        }

        return node;
    }
}
