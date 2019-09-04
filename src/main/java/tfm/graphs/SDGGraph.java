package tfm.graphs;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.stmt.EmptyStmt;
import edg.graphlib.Visitor;
import tfm.arcs.data.ArcData;
import tfm.nodes.GraphNode;
import tfm.nodes.PDGNode;
import tfm.slicing.SlicingCriterion;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SDGGraph extends Graph {

    private List<PDGGraph> pdgGraphList;

    public SDGGraph() {
        this.pdgGraphList = new ArrayList<>();
    }

    @Override
    public <ASTNode extends Node> GraphNode<ASTNode> addNode(String instruction, ASTNode node) {
        GraphNode<ASTNode> sdgNode = new GraphNode<>(getNextVertexId(), instruction, node);
        super.addVertex(sdgNode);

        return sdgNode;
    }

    @Override
    public String toGraphvizRepresentation() {
        return pdgGraphList.stream().map(PDGGraph::toGraphvizRepresentation).collect(Collectors.joining("\n"));
    }

    @Override
    public Graph slice(SlicingCriterion slicingCriterion) {
        return this;
    }

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

    public GraphNode<MethodDeclaration> addMethod(MethodDeclaration methodDeclaration, PDGGraph pdgGraph) {
        GraphNode<MethodDeclaration> node = new GraphNode<>(
                getNextVertexId(),
                "ENTER " + methodDeclaration.getDeclarationAsString(false, false, true),
                methodDeclaration
        );

        pdgGraph.depthFirstSearch(pdgGraph.getRootNode(), (Visitor<String, ArcData>) (g, v) -> {
            if (Objects.equals(g.getRootVertex(), v)) {
                return; // We don't care about root node (entry node)
            }

            PDGNode<?> pdgNode = (PDGNode) v;

            GraphNode<?> sdgNode = new GraphNode<>(
                    getNextVertexId(),
                    pdgNode.getData(),
                    pdgNode.getAstNode()
            );


        });

        super.addVertex(node);

        return node;
    }
}
