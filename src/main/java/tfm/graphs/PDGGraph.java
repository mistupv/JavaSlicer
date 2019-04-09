package tfm.graphs;

import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.Statement;
import edg.graphlib.Vertex;
import edg.graphlib.Visitor;
import tfm.arcs.Arc;
import tfm.arcs.data.ArcData;
import tfm.arcs.pdg.ControlDependencyArc;
import tfm.arcs.pdg.DataDependencyArc;
import tfm.nodes.PDGNode;
import tfm.nodes.Node;
import tfm.variables.*;
import tfm.variables.actions.VariableDeclaration;
import tfm.variables.actions.VariableUse;
import tfm.variables.actions.VariableDefinition;

import javax.swing.plaf.nimbus.State;
import java.util.*;
import java.util.stream.Collectors;

public abstract class PDGGraph extends Graph<PDGNode> {

    public PDGGraph() {
        setRootVertex(new PDGNode(NodeId.getVertexId(), getRootNodeData(), new EmptyStmt()));
    }

    protected abstract String getRootNodeData();

    @Override
    public PDGNode addNode(String instruction, Statement statement) {
        PDGNode vertex = new PDGNode(NodeId.getVertexId(), instruction, statement);
        super.addVertex(vertex);

        return vertex;
    }

    @SuppressWarnings("unchecked")
    private void addArc(Arc arc) {
        super.addEdge(arc);
    }

    public void addControlDependencyArc(PDGNode from, PDGNode to) {
        ControlDependencyArc controlDependencyArc = new ControlDependencyArc(from, to);

        this.addArc(controlDependencyArc);
    }

    public void addDataDependencyArc(PDGNode from, PDGNode to, String variable) {
        DataDependencyArc dataDataDependencyArc = new DataDependencyArc(from, to, variable);

        this.addArc(dataDataDependencyArc);
    }

    @Override
    public String toGraphvizRepresentation() {
        String lineSep = System.lineSeparator();

        String nodesDeclaration = getVerticies().stream()
                .map(vertex -> ((Node) vertex).toGraphvizRepresentation())
                .collect(Collectors.joining(lineSep));

        String arrows =
                getArrows().stream()
                        .sorted(Comparator.comparingInt(arrow -> ((Node) arrow.getFrom()).getId()))
                        .map(arrow -> ((Arc) arrow).toGraphvizRepresentation())
                        .collect(Collectors.joining(lineSep));


        return "digraph g{" + lineSep +
                "splines=true;" + lineSep +
                nodesDeclaration + lineSep +
                arrows + lineSep +
                "}";
    }
}
