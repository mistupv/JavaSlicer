package tfm.graphs;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.Statement;
import edg.graphlib.Arrow;
import tfm.arcs.Arc;
import tfm.arcs.cfg.ControlFlowArc;
import tfm.nodes.CFGNode;
import tfm.nodes.GraphNode;
import tfm.slicing.SlicingCriterion;

import java.util.Comparator;
import java.util.stream.Collectors;

public class CFGGraph extends Graph<CFGNode<?>> {

    public CFGGraph() {
        super();
        setRootVertex(new CFGNode<>(getNextVertexId(), getRootNodeData(), new EmptyStmt()));
    }

    @Override
    public <ASTNode extends Node> CFGNode<?> addNode(String instruction, ASTNode node) {
        CFGNode<?> vertex = new CFGNode<>(getNextVertexId(), instruction, node);
        this.addVertex(vertex);

        return vertex;
    }


    protected String getRootNodeData() {
        return "Start";
    }

    @SuppressWarnings("unchecked")
    public void addControlFlowEdge(CFGNode from, CFGNode to) {
        super.addEdge((Arrow) new ControlFlowArc(from, to));
    }

    @Override
    public String toGraphvizRepresentation() {
        String lineSep = System.lineSeparator();

        String nodes = getNodes().stream()
                .sorted(Comparator.comparingInt(GraphNode::getId))
                .map(node -> String.format("%s [label=\"%s: %s\"]", node.getId(), node.getId(), node.getData()))
                .collect(Collectors.joining(lineSep));

        String arrows =
                getArrows().stream()
                        .sorted(Comparator.comparingInt(arrow -> ((GraphNode) arrow.getFrom()).getId()))
                        .map(arrow -> ((Arc) arrow).toGraphvizRepresentation())
                        .collect(Collectors.joining(lineSep));

        return "digraph g{" + lineSep +
                nodes + lineSep +
                arrows + lineSep +
                "}";
    }

    @Override
    public Graph<CFGNode<?>> slice(SlicingCriterion slicingCriterion) {
        return this;
    }
}
