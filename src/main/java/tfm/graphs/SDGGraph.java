package tfm.graphs;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import edg.graphlib.Vertex;
import edg.graphlib.Visitor;
import tfm.arcs.data.ArcData;
import tfm.nodes.AuxiliarSDGNode;
import tfm.nodes.PDGNode;
import tfm.nodes.SDGNode;
import tfm.slicing.SlicingCriterion;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SDGGraph extends Graph<SDGNode<?>> {

    private List<PDGGraph> pdgGraphList;

    public SDGGraph() {
        this.pdgGraphList = new ArrayList<>();
    }

    @Override
    public <ASTNode extends Node> SDGNode<ASTNode> addNode(String instruction, ASTNode node) {
        SDGNode<ASTNode> sdgNode = new SDGNode<>(getNextVertexId(), instruction, node);
        super.addVertex(sdgNode);

        return sdgNode;
    }

    @Override
    public String toGraphvizRepresentation() {
        return pdgGraphList.stream().map(PDGGraph::toGraphvizRepresentation).collect(Collectors.joining("\n"));
    }

    @Override
    public Graph<SDGNode<?>> slice(SlicingCriterion slicingCriterion) {
        return this;
    }

    public void addPDG(PDGGraph pdgGraph, MethodDeclaration methodDeclaration) {
        if (this.rootVertex == null) {
            this.setRootVertex(new SDGNode<>(getNextVertexId(), methodDeclaration.getNameAsString(), methodDeclaration));
        }

        for (Parameter parameter : methodDeclaration.getParameters()) {
            AuxiliarSDGNode sdgNode = new AuxiliarSDGNode(
                    getNextVertexId(),
                    String.format("%s = %s_in", parameter.getNameAsString(), parameter.getNameAsString())
            );

            addVertex(sdgNode);
        }

        for (PDGNode<?> node : pdgGraph.getNodes()) {
            if (!this.verticies.contains(node)) {
                SDGNode<?> sdgNode = new SDGNode<>(
                        getNextVertexId(),
                        node.getData(),
                        node.getAstNode(),
                        node.getIncomingArrows(),
                        node.getOutgoingArrows(),
                        node.getDeclaredVariables(),
                        node.getDefinedVariables(),
                        node.getUsedVariables()
                );

                addVertex(sdgNode);
            }
        }
    }

    public SDGNode<MethodDeclaration> addMethod(MethodDeclaration methodDeclaration, PDGGraph pdgGraph) {
        SDGNode<MethodDeclaration> node = new SDGNode<>(
                getNextVertexId(),
                "ENTER " + methodDeclaration.getDeclarationAsString(false, false, true),
                methodDeclaration
        );

        pdgGraph.depthFirstSearch(pdgGraph.getRootNode(), (Visitor<String, ArcData>) (g, v) -> {
            if (Objects.equals(g.getRootVertex(), v)) {
                return; // We don't care about root node (entry node)
            }

            PDGNode<?> pdgNode = (PDGNode) v;

            SDGNode<?> sdgNode = new SDGNode<>(
                    getNextVertexId(),
                    pdgNode.getData(),
                    pdgNode.getAstNode()
            );


        });

        super.addVertex(node);

        return node;
    }
}
