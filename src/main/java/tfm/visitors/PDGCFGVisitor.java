package tfm.visitors;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import edg.graphlib.Arrow;
import tfm.arcs.cfg.ControlFlowArc;
import tfm.graphs.CFGGraph;
import tfm.graphs.PDGGraph;
import tfm.nodes.CFGNode;
import tfm.nodes.PDGNode;
import tfm.utils.Logger;
import tfm.variables.VariableExtractor;

import java.util.*;

public class PDGCFGVisitor extends VoidVisitorAdapter<PDGNode> {

    private CFGGraph cfgGraph;
    private PDGGraph pdgGraph;

    private CFGVisitor cfgVisitor;

    public PDGCFGVisitor(PDGGraph pdgGraph) {
        this(pdgGraph, new CFGGraph() {
            @Override
            protected String getRootNodeData() {
                return "Start";
            }
        });
    }

    public PDGCFGVisitor(PDGGraph pdgGraph, CFGGraph cfgGraph) {
        this.pdgGraph = pdgGraph;
        this.cfgGraph = cfgGraph;
        this.cfgVisitor = new CFGVisitor(cfgGraph);
    }

    @Override
    public void visit(ExpressionStmt expressionStmt, PDGNode parent) {
        buildCFG(expressionStmt);

        PDGNode node = pdgGraph.addNode(cfgGraph.findNodeByStatement(expressionStmt).get());
        pdgGraph.addControlDependencyArc(parent, node);

        buildDataDependency(node);
    }

    private Set<CFGNode> findLastDefinitionsFrom(CFGNode startNode, String variable) {
        Logger.log("=======================================================");
        Logger.log("Starting from " + startNode);
        Logger.log("Looking for variable " + variable);
        Logger.log(cfgGraph.toString());
        return findLastDefinitionsFrom(new HashSet<>(), startNode, variable);
    }

    private Set<CFGNode> findLastDefinitionsFrom(Set<CFGNode> visited, CFGNode startNode, String variable) {
        visited.add(startNode);

        Logger.log("On " + startNode);

        Set<CFGNode> res = new HashSet<>();

        for (Arrow arrow : startNode.getIncomingArrows()) {
            ControlFlowArc controlFlowArc = (ControlFlowArc) arrow;

            CFGNode from = (CFGNode) controlFlowArc.getFromNode();

            Logger.log("Arrow from node: " + from);

            if (visited.contains(from)) {
                Logger.log("It's already visited. Continuing...");
                continue;
            }

            if (from.getDefinedVariables().contains(variable)) {
                Logger.log("Contains defined variable: " + variable);
                res.add(from);
            } else {
                res.addAll(findLastDefinitionsFrom(visited, from, variable));
            }
        }

        return res;
    }

    @Override
    public void visit(IfStmt ifStmt, PDGNode parent) {
        buildCFG(ifStmt);

        PDGNode node = pdgGraph.addNode(cfgGraph.findNodeByStatement(ifStmt).get());
        pdgGraph.addControlDependencyArc(parent, node);

        buildDataDependency(node);

        ifStmt.getThenStmt().accept(this, node);

        ifStmt.getElseStmt().ifPresent(statement -> statement.accept(this, node));
    }

    @Override
    public void visit(WhileStmt whileStmt, PDGNode parent) {
        buildCFG(whileStmt);

        PDGNode node = pdgGraph.addNode(cfgGraph.findNodeByStatement(whileStmt).get());
        pdgGraph.addControlDependencyArc(parent, node);

        buildDataDependency(node);

        whileStmt.getBody().accept(this, node);
    }

    @Override
    public void visit(ForStmt forStmt, PDGNode parent) {
        buildCFG(forStmt);

        PDGNode node = pdgGraph.addNode(cfgGraph.findNodeByStatement(forStmt).get());
        pdgGraph.addControlDependencyArc(parent, node);

        buildDataDependency(node);
    }

    @Override
    public void visit(ForEachStmt forEachStmt, PDGNode parent) {
        buildCFG(forEachStmt);

        PDGNode node = pdgGraph.addNode(cfgGraph.findNodeByStatement(forEachStmt).get());
        pdgGraph.addControlDependencyArc(parent, node);

        buildDataDependency(node);

        forEachStmt.getBody().accept(this, node);
    }

    private void buildCFG(Statement statement) {
        statement.accept(this.cfgVisitor, null);
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
