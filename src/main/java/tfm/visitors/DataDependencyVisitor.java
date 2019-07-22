package tfm.visitors;

import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import edg.graphlib.Arrow;
import tfm.arcs.cfg.ControlFlowArc;
import tfm.graphs.CFGGraph;
import tfm.graphs.PDGGraph;
import tfm.nodes.CFGNode;
import tfm.nodes.PDGNode;
import tfm.utils.Utils;
import tfm.variables.VariableExtractor;

import java.util.HashSet;
import java.util.Objects;
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
        buildDataDependency(forStmt);

        forStmt.getInitialization().accept(this, null);

        forStmt.getBody().accept(this, null);

        forStmt.getUpdate().accept(this, null);
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
}
