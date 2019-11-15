package tfm.graphs;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.stmt.EmptyStmt;
import tfm.nodes.GraphNode;
import tfm.slicing.SlicingCriterion;
import tfm.utils.Context;

import java.util.*;
import java.util.stream.Collectors;

public class SDGGraph extends Graph {

    private Map<Context, PDGGraph> contextPDGGraphMap;

    public SDGGraph() {
        this.contextPDGGraphMap = new HashMap<>();
    }

    @Override
    public <ASTNode extends Node> GraphNode<ASTNode> addNode(String instruction, ASTNode node) {
        GraphNode<ASTNode> sdgNode = new GraphNode<>(getNextVertexId(), instruction, node);
        super.addVertex(sdgNode);

        return sdgNode;
    }

    @Override
    public String toGraphvizRepresentation() {
        return contextPDGGraphMap.values().stream()
                .map(PDGGraph::toGraphvizRepresentation).collect(Collectors.joining("\n"));
    }

    @Override
    public Graph slice(SlicingCriterion slicingCriterion) {
        return this;
    }

    public Map<Context, PDGGraph> getContextPDGGraphMap() {
        return contextPDGGraphMap;
    }

    public Set<Context> getContexts() {
        return contextPDGGraphMap.keySet();
    }

    public Set<MethodDeclaration> getMethods() {
        return getContexts().stream()
                .filter(context -> context.getCurrentMethod().isPresent())
                .map(context -> context.getCurrentMethod().get())
                .collect(Collectors.toSet());
    }

    public Collection<PDGGraph> getPDGs() {
        return contextPDGGraphMap.values();
    }

    @Deprecated
    public void addPDG(PDGGraph pdgGraph, MethodDeclaration methodDeclaration) {
        if (this.rootVertex == null) {
            this.setRootVertex(new GraphNode<>(getNextVertexId(), methodDeclaration.getNameAsString(), methodDeclaration));
        }

        for (Parameter parameter : methodDeclaration.getParameters()) {
            GraphNode<?> sdgNode = new GraphNode<>(
                    getNextVertexId(),
                    String.format("%s = %s_in", parameter.getNameAsString(), parameter.getNameAsString()),
                    new EmptyStmt()
            );

            addVertex(sdgNode);
        }

        for (GraphNode<?> node : pdgGraph.getNodes()) {
            if (!this.verticies.contains(node)) {
                GraphNode<?> sdgNode = new GraphNode<>(
                        getNextVertexId(),
                        node.getData(),
                        node.getAstNode(),
                        node.getIncomingArcs(),
                        node.getOutgoingArcs(),
                        node.getDeclaredVariables(),
                        node.getDefinedVariables(),
                        node.getUsedVariables()
                );

                addVertex(sdgNode);
            }
        }
    }

    public void addMethod(MethodDeclaration methodDeclaration, PDGGraph pdgGraph) {
        GraphNode<MethodDeclaration> methodRootNode = new GraphNode<>(
                getNextVertexId(),
                "ENTER " + methodDeclaration.getDeclarationAsString(false, false, true),
                methodDeclaration
        );

        super.addVertex(methodRootNode);
    }
}
