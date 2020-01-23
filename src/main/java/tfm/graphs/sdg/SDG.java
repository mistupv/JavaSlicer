package tfm.graphs.sdg;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.stmt.EmptyStmt;
import tfm.graphs.Buildable;
import tfm.graphs.Graph;
import tfm.graphs.pdg.PDG;
import tfm.nodes.GraphNode;
import tfm.nodes.NodeFactory;
import tfm.slicing.Slice;
import tfm.slicing.Sliceable;
import tfm.slicing.SlicingCriterion;
import tfm.utils.Context;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SDG extends Graph implements Sliceable, Buildable<NodeList<CompilationUnit>> {
    private boolean built = false;
    private Map<Context, PDG> contextPDGGraphMap;

    public SDG() {
        this.contextPDGGraphMap = new HashMap<>();
    }

    @Override
    public Slice slice(SlicingCriterion slicingCriterion) {
        throw new RuntimeException("Slicing not implemented for the SDG");
    }

    @Override
    public void build(NodeList<CompilationUnit> nodeList) {
        nodeList.accept(new SDGBuilder(this), null);
    }

    @Override
    public boolean isBuilt() {
        return built;
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
