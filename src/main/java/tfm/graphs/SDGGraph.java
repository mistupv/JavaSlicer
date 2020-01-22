package tfm.graphs;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.stmt.EmptyStmt;
import org.jgrapht.io.DOTExporter;
import tfm.arcs.Arc;
import tfm.nodes.GraphNode;
import tfm.nodes.NodeFactory;
import tfm.slicing.SlicingCriterion;
import tfm.utils.Context;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SDGGraph extends Graph implements Sliceable<SDGGraph> {

    private Map<Context, PDGGraph> contextPDGGraphMap;

    public SDGGraph() {
        this.contextPDGGraphMap = new HashMap<>();
    }

    @Override
    public SDGGraph slice(SlicingCriterion slicingCriterion) {
        throw new IllegalStateException("Not implemented (yet)");
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
        for (Parameter parameter : methodDeclaration.getParameters()) {
            GraphNode<?> sdgNode = NodeFactory.graphNode(
                    getNextVertexId(),
                    String.format("%s = %s_in", parameter.getNameAsString(), parameter.getNameAsString()),
                    new EmptyStmt()
            );

            addVertex(sdgNode);
        }

        for (GraphNode<?> node : pdgGraph.vertexSet()) {
            if (!this.containsVertex(node)) {
                GraphNode<?> sdgNode = NodeFactory.computedGraphNode(
                        getNextVertexId(),
                        node.getInstruction(),
                        node.getAstNode(),
                        node.getDeclaredVariables(),
                        node.getDefinedVariables(),
                        node.getUsedVariables()
                );

                addVertex(sdgNode);
            }
        }
    }

    public void addMethod(MethodDeclaration methodDeclaration, PDGGraph pdgGraph) {
        GraphNode<MethodDeclaration> methodRootNode = NodeFactory.graphNode(
                getNextVertexId(),
                "ENTER " + methodDeclaration.getDeclarationAsString(false, false, true),
                methodDeclaration
        );

        super.addVertex(methodRootNode);
    }
}
