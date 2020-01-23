package tfm.visitors.pdg;

import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.graphs.CFG;
import tfm.graphs.PDG;
import tfm.nodes.GraphNode;
import tfm.variables.VariableExtractor;

import java.util.Optional;
import java.util.Set;

public class DataDependencyBuilder extends VoidVisitorAdapter<Void> {

    private CFG cfg;
    private PDG pdg;

    public DataDependencyBuilder(PDG pdg, CFG cfg) {
        this.pdg = pdg;
        this.cfg = cfg;
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
        GraphNode<ForStmt> forNode = pdg.findNodeByASTNode(forStmt).get();

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

    @Override
    public void visit(SwitchStmt switchStmt, Void ignored) {
        buildDataDependency(switchStmt);

        switchStmt.getEntries().accept(this, null);
    }

    @Override
    public void visit(SwitchEntryStmt switchEntryStmt, Void ignored) {
        buildDataDependency(switchEntryStmt);

        switchEntryStmt.getStatements().accept(this, null);
    }

    private void buildDataDependency(Statement statement) {
        buildDataDependency(pdg.findNodeByASTNode(statement).get());
    }

    private void buildDataDependency(GraphNode<?> node) {
        new VariableExtractor()
                .setOnVariableUseListener(variable -> {
                    node.addUsedVariable(variable);

                    Optional<? extends GraphNode<?>> nodeOptional = cfg.findNodeByASTNode(node.getAstNode());

                    if (!nodeOptional.isPresent()) {
                        return;
                    }

                    GraphNode<?> cfgNode = nodeOptional.get();

                    Set<GraphNode<?>> lastDefinitions = cfg.findLastDefinitionsFrom(cfgNode, variable);

                    for (GraphNode<?> definitionNode : lastDefinitions) {
                        pdg.findNodeByASTNode(definitionNode.getAstNode())
                                .ifPresent(pdgNode -> pdg.addDataDependencyArc(pdgNode, node, variable));
                    }
                })
                .setOnVariableDefinitionListener(node::addDefinedVariable)
                .setOnVariableDeclarationListener(node::addDeclaredVariable)
                .visit(node.getAstNode());
    }

    // For statement special case
    private void buildDataDependency(GraphNode<?> forNode, Statement statement) {
        new VariableExtractor()
                .setOnVariableUseListener(variable -> {
                    forNode.addUsedVariable(variable);

                    Optional<? extends GraphNode<?>> nodeOptional = cfg.findNodeByASTNode(statement);

                    if (!nodeOptional.isPresent()) {
                        return;
                    }

                    pdg.addDataDependencyArc(forNode, forNode, variable);
                })
                .setOnVariableDefinitionListener(forNode::addDefinedVariable)
                .setOnVariableDeclarationListener(forNode::addDeclaredVariable)
                .visit(statement);
    }
}
