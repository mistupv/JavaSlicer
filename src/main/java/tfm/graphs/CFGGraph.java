package tfm.graphs;

import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.Statement;
import edg.graphlib.Arrow;
import tfm.arcs.Arc;
import tfm.arcs.cfg.ControlFlowArc;
import tfm.nodes.CFGNode;
import tfm.nodes.Node;

import java.util.Comparator;
import java.util.stream.Collectors;

public abstract class CFGGraph extends Graph<CFGNode> {

    public CFGGraph() {
        super();
        setRootVertex(new CFGNode(NodeId.getVertexId(), getRootNodeData(), new EmptyStmt()));
    }

    @Override
    public CFGNode addNode(String instruction, Statement statement) {
        CFGNode vertex = new CFGNode(NodeId.getVertexId(), instruction, statement);
        this.addVertex(vertex);

        return vertex;
    }

    protected abstract String getRootNodeData();

    @SuppressWarnings("unchecked")
    public void addControlFlowEdge(CFGNode from, CFGNode to) {
        super.addEdge((Arrow) new ControlFlowArc(from, to));
    }

    @Override
    public String toGraphvizRepresentation() {
        String lineSep = System.lineSeparator();

        String arrows =
                getArrows().stream()
                        .sorted(Comparator.comparingInt(arrow -> ((Node) arrow.getFrom()).getId()))
                        .map(arrow -> ((Arc) arrow).toGraphvizRepresentation())
                        .collect(Collectors.joining(lineSep));

        return "digraph g{" + lineSep +
                arrows + lineSep +
                "}";
    }
}
