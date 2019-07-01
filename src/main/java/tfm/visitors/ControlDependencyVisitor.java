package tfm.visitors;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.graphs.CFGGraph;
import tfm.graphs.PDGGraph;
import tfm.nodes.PDGNode;

public class ControlDependencyVisitor extends VoidVisitorAdapter<PDGNode> {

    private CFGGraph cfgGraph;
    private PDGGraph pdgGraph;

    public ControlDependencyVisitor(PDGGraph pdgGraph, CFGGraph cfgGraph) {
        this.pdgGraph = pdgGraph;
        this.cfgGraph = cfgGraph;
    }

    @Override
    public void visit(ExpressionStmt expressionStmt, PDGNode parent) {
        addNodeAndControlDependency(expressionStmt, parent);
    }

    @Override
    public void visit(IfStmt ifStmt, PDGNode parent) {
        PDGNode node = addNodeAndControlDependency(ifStmt, parent);

        ifStmt.getThenStmt().accept(this, node);

        ifStmt.getElseStmt().ifPresent(statement -> statement.accept(this, node));
    }

    @Override
    public void visit(WhileStmt whileStmt, PDGNode parent) {
        PDGNode node = addNodeAndControlDependency(whileStmt, parent);

        whileStmt.getBody().accept(this, node);
    }

    @Override
    public void visit(ForStmt forStmt, PDGNode parent) {
        PDGNode node = addNodeAndControlDependency(forStmt, parent);

        forStmt.getBody().accept(this, node);
    }

    @Override
    public void visit(ForEachStmt forEachStmt, PDGNode parent) {
        PDGNode node = addNodeAndControlDependency(forEachStmt, parent);

        forEachStmt.getBody().accept(this, node);
    }

    private PDGNode addNodeAndControlDependency(Statement statement, PDGNode parent) {
        PDGNode node = pdgGraph.addNode(cfgGraph.findNodeByStatement(statement).get());
        pdgGraph.addControlDependencyArc(parent, node);

        return node;
    }
}
