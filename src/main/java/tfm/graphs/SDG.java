package tfm.graphs;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.stmt.EmptyStmt;
import tfm.nodes.GraphNode;
import tfm.nodes.NodeFactory;
import tfm.slicing.SlicingCriterion;
import tfm.utils.Context;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SDG extends Graph implements Sliceable<SDG> {

    private Map<Context, PDG> contextPDGGraphMap;

    public SDG() {
        this.contextPDGGraphMap = new HashMap<>();
    }

    @Override
    public SDG slice(SlicingCriterion slicingCriterion) {
        throw new IllegalStateException("Not implemented (yet)");
    }

    public Map<Context, PDG> getContextPDGGraphMap() {
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

    public Collection<PDG> getPDGs() {
        return contextPDGGraphMap.values();
    }

    @Deprecated
    public void addPDG(PDG pdg, MethodDeclaration methodDeclaration) {
        for (Parameter parameter : methodDeclaration.getParameters()) {
            GraphNode<?> sdgNode = NodeFactory.graphNode(
                    getNextVertexId(),
                    String.format("%s = %s_in", parameter.getNameAsString(), parameter.getNameAsString()),
                    new EmptyStmt()
            );

            addVertex(sdgNode);
        }

        for (GraphNode<?> node : pdg.vertexSet()) {
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

    public void addMethod(MethodDeclaration methodDeclaration, PDG pdg) {
        GraphNode<MethodDeclaration> methodRootNode = NodeFactory.graphNode(
                getNextVertexId(),
                "ENTER " + methodDeclaration.getDeclarationAsString(false, false, true),
                methodDeclaration
        );

        super.addVertex(methodRootNode);
    }
}
