package tfm.visitors;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.graphs.CFGGraph;
import tfm.graphs.PDGGraph;
import tfm.nodes.CFGNode;
import tfm.nodes.PDGNode;
import tfm.utils.Utils;
import tfm.variables.VariableExtractor;

import java.util.Optional;
import java.util.Set;

public class DataDependencyVisitor extends VoidVisitorAdapter<Void> {

    private CFGGraph cfgGraph;
    private PDGGraph pdgGraph;

    public DataDependencyVisitor(PDGGraph pdgGraph, CFGGraph cfgGraph) {
        this.pdgGraph = pdgGraph;
        this.cfgGraph = cfgGraph;
    }

    @Override
    public void visit(ExpressionStmt expressionStmt, Void ignored) {
        buildDataDependency(expressionStmt);
    }

    @Override
    public void visit(IfStmt ifStmt, Void ignored) {
        buildDataDependency(ifStmt);

        ifStmt.getThenStmt().accept(this, null);

        ifStmt.getElseStmt().ifPresent(statement -> statement.accept(this, null));
    }

    @Override
    public void visit(WhileStmt whileStmt, Void ignored) {
        buildDataDependency(whileStmt);

        whileStmt.getBody().accept(this, null);
    }

    @Override
    public void visit(ForStmt forStmt, Void ignored) {
        PDGNode forNode = pdgGraph.findNodeByASTNode(forStmt).get();

        forStmt.getInitialization().stream()
                .map(ExpressionStmt::new)
                .forEach(expressionStmt -> buildDataDependency(forNode, expressionStmt));

        buildDataDependency(forStmt); // Only for comparison

        forStmt.getUpdate().stream()
                .map(ExpressionStmt::new)
                .forEach(expressionStmt -> buildDataDependency(forNode, expressionStmt));

        forStmt.getBody().accept(this, null);
    }

    @Override
    public void visit(ForEachStmt forEachStmt, Void ignored) {
        buildDataDependency(forEachStmt);

        forEachStmt.getBody().accept(this, null);
    }

    private void buildDataDependency(Statement statement) {
        buildDataDependency(pdgGraph.findNodeByASTNode(statement).get());
    }

    private void buildDataDependency(PDGNode node) {
        new VariableExtractor()
                .setOnVariableUseListener(variable -> {
                    node.addUsedVariable(variable);

                    Optional<CFGNode> nodeOptional = cfgGraph.findNodeByASTNode(node.getAstNode());

                    if (!nodeOptional.isPresent()) {
                        return;
                    }

                    CFGNode cfgNode = nodeOptional.get();

                    Set<CFGNode> lastDefinitions = Utils.findLastDefinitionsFrom(cfgNode, variable);

                    for (CFGNode definitionNode : lastDefinitions) {
                        pdgGraph.findNodeByASTNode(definitionNode.getAstNode())
                                .ifPresent(pdgNode -> pdgGraph.addDataDependencyArc(pdgNode, node, variable));
                    }
                })
                .setOnVariableDefinitionListener(node::addDefinedVariable)
                .setOnVariableDeclarationListener(node::addDeclaredVariable)
                .visit(node.getAstNode());
    }

    // For statement special case
    private void buildDataDependency(PDGNode forNode, Statement statement) {
        new VariableExtractor()
                .setOnVariableUseListener(variable -> {
                    forNode.addUsedVariable(variable);

                    Optional<CFGNode> nodeOptional = cfgGraph.findNodeByASTNode(statement);

                    if (!nodeOptional.isPresent()) {
                        return;
                    }

                    pdgGraph.addDataDependencyArc(forNode, forNode, variable);
                })
                .setOnVariableDefinitionListener(forNode::addDefinedVariable)
                .setOnVariableDeclarationListener(forNode::addDeclaredVariable)
                .visit(statement);
    }
}
