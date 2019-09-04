package tfm.visitors;

import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.graphs.CFGGraph;
import tfm.graphs.PDGGraph;
import tfm.nodes.GraphNode;

import java.util.stream.Collectors;

public class ControlDependencyVisitor extends VoidVisitorAdapter<GraphNode> {

    private CFGGraph cfgGraph;
    private PDGGraph pdgGraph;

    public ControlDependencyVisitor(PDGGraph pdgGraph, CFGGraph cfgGraph) {
        this.pdgGraph = pdgGraph;
        this.cfgGraph = cfgGraph;
    }

    @Override
    public void visit(ExpressionStmt expressionStmt, GraphNode parent) {
        addNodeAndControlDependency(expressionStmt, parent);
    }

    @Override
    public void visit(IfStmt ifStmt, GraphNode parent) {
        GraphNode node = addNodeAndControlDependency(ifStmt, parent);

        ifStmt.getThenStmt().accept(this, node);

        ifStmt.getElseStmt().ifPresent(statement -> statement.accept(this, node));
    }

    @Override
    public void visit(WhileStmt whileStmt, GraphNode parent) {
        GraphNode node = addNodeAndControlDependency(whileStmt, parent);

        whileStmt.getBody().accept(this, node);
    }

    @Override
    public void visit(ForStmt forStmt, GraphNode parent) {
        String initialization = forStmt.getInitialization().stream()
                .map(com.github.javaparser.ast.Node::toString)
                .collect(Collectors.joining(","));

        String update = forStmt.getUpdate().stream()
                .map(com.github.javaparser.ast.Node::toString)
                .collect(Collectors.joining(","));

        String compare = forStmt.getCompare()
                .map(com.github.javaparser.ast.Node::toString)
                .orElse("true");


        GraphNode forNode = pdgGraph.addNode(
                String.format("for (%s;%s;%s)", initialization, compare, update),
                forStmt
        );

        pdgGraph.addControlDependencyArc(parent, forNode);

        forStmt.getBody().accept(this, forNode);
    }

    @Override
    public void visit(ForEachStmt forEachStmt, GraphNode parent) {
        GraphNode node = addNodeAndControlDependency(forEachStmt, parent);

        forEachStmt.getBody().accept(this, node);
    }

    @Override
    public void visit(SwitchStmt switchStmt, GraphNode parent) {
        GraphNode node = addNodeAndControlDependency(switchStmt, parent);

        switchStmt.getEntries().accept(this, node);
    }

    @Override
    public void visit(SwitchEntryStmt switchEntryStmt, GraphNode parent) {
        GraphNode node = addNodeAndControlDependency(switchEntryStmt, parent);

        switchEntryStmt.getStatements().accept(this, node);
    }

    private GraphNode addNodeAndControlDependency(Statement statement, GraphNode parent) {
        GraphNode<?> cfgNode = cfgGraph.findNodeByASTNode(statement).get();

        GraphNode node = pdgGraph.addNode(cfgNode.getData(), cfgNode.getAstNode());
        pdgGraph.addControlDependencyArc(parent, node);

        return node;
    }
}
