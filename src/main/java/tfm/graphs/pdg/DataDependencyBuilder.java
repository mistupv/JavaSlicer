package tfm.graphs.pdg;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.graphs.cfg.CFG;
import tfm.nodes.GraphNode;

class DataDependencyBuilder extends VoidVisitorAdapter<Void> {

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
        forStmt.getInitialization()
                .forEach(this::buildDataDependency);

        buildDataDependency(forStmt); // Only for comparison

        forStmt.getUpdate()
                .forEach(this::buildDataDependency);

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

    private void buildDataDependency(Node node) {
        buildDataDependency(pdg.findNodeByASTNode(node).get());
    }

    private void buildDataDependency(GraphNode<?> node) {
        for (String usedVariable : node.getUsedVariables()) {
            cfg.findLastDefinitionsFrom(node, usedVariable)
                    .forEach(definitionNode -> pdg.addDataDependencyArc(definitionNode, node, usedVariable));
        }
    }
}
