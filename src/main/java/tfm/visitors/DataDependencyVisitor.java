package tfm.visitors;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import edg.graphlib.Arrow;
import tfm.arcs.cfg.ControlFlowArc;
import tfm.graphs.CFGGraph;
import tfm.graphs.PDGGraph;
import tfm.nodes.CFGNode;
import tfm.nodes.PDGNode;
import tfm.utils.Logger;
import tfm.utils.Utils;
import tfm.variables.VariableExtractor;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class DataDependencyVisitor extends VoidVisitorAdapter<PDGNode> {

    private CFGGraph cfgGraph;
    private PDGGraph pdgGraph;

    public DataDependencyVisitor(PDGGraph pdgGraph, CFGGraph cfgGraph) {
        this.pdgGraph = pdgGraph;
        this.cfgGraph = cfgGraph;
    }

    @Override
    public void visit(ExpressionStmt expressionStmt, PDGNode parent) {
        buildDataDependency(expressionStmt);
    }

    private Set<CFGNode> findLastDefinitionsFrom(CFGNode startNode, String variable) {
        Logger.log("=======================================================");
        Logger.log("Starting from " + startNode);
        Logger.log("Looking for variable " + variable);
//        Logger.log(cfgGraph.toString());
        return findLastDefinitionsFrom(new HashSet<>(), startNode, startNode, variable);
    }

    private Set<CFGNode> findLastDefinitionsFrom(Set<Integer> visited, CFGNode startNode, CFGNode currentNode, String variable) {
        visited.add(currentNode.getId());

        Logger.log("On " + currentNode);

        Set<CFGNode> res = new HashSet<>();

        for (Arrow arrow : currentNode.getIncomingArrows()) {
            ControlFlowArc controlFlowArc = (ControlFlowArc) arrow;

            CFGNode from = (CFGNode) controlFlowArc.getFromNode();

            Logger.log("Arrow from node: " + from);

            if (!Objects.equals(startNode, from) && visited.contains(from.getId())) {
                Logger.log("It's already visited. Continuing...");
                continue;
            }

            if (from.getDefinedVariables().contains(variable)) {
                Logger.log("Contains defined variable: " + variable);
                res.add(from);
            } else {
                Logger.log("Doesn't contain the variable, searching inside it");
                res.addAll(findLastDefinitionsFrom(visited, startNode, from, variable));
            }
        }

        Logger.format("Done with node %s", currentNode.getId());

        return res;
    }

    @Override
    public void visit(IfStmt ifStmt, PDGNode parent) {
        buildDataDependency(ifStmt);

        ifStmt.getThenStmt().accept(this, null);

        ifStmt.getElseStmt().ifPresent(statement -> statement.accept(this, null));
    }

    @Override
    public void visit(WhileStmt whileStmt, PDGNode parent) {
        buildDataDependency(whileStmt);

        whileStmt.getBody().accept(this, null);
    }

    @Override
    public void visit(ForStmt forStmt, PDGNode parent) {
        buildDataDependency(forStmt);

        forStmt.getBody().accept(this, null);
    }

    @Override
    public void visit(ForEachStmt forEachStmt, PDGNode parent) {
        buildDataDependency(forEachStmt);

        forEachStmt.getBody().accept(this, null);
    }

    private void buildDataDependency(Statement statement) {
        buildDataDependency(pdgGraph.findNodeByStatement(statement).get());
    }

    private void buildDataDependency(PDGNode node) {
        new VariableExtractor()
                .setOnVariableUseListener(variable -> {
                    node.addUsedVariable(variable);

                    Optional<CFGNode> nodeOptional = cfgGraph.findNodeByStatement(node.getStatement());

                    if (!nodeOptional.isPresent()) {
                        return;
                    }

                    CFGNode cfgNode = nodeOptional.get();

                    Set<CFGNode> lastDefinitions = findLastDefinitionsFrom(cfgNode, variable);

                    for (CFGNode definitionNode : lastDefinitions) {
                        pdgGraph.findNodeByStatement(definitionNode.getStatement())
                                .ifPresent(pdgNode -> pdgGraph.addDataDependencyArc(pdgNode, node, variable));
                    }
                })
                .setOnVariableDefinitionListener(node::addDefinedVariable)
                .setOnVariableDeclarationListener(node::addDeclaredVariable)
                .visit(node.getStatement());
    }
}
