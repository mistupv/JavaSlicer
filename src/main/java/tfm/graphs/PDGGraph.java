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
import java.util.stream.Stream;

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

    public List<PDGNode> getNodesAtLevel(int level) {
        return getVerticies().stream()
                .map(vertex -> (PDGNode) vertex)
                .filter(node -> node.getLevel() == level)
                .collect(Collectors.toList());
    }

    public int getLevels() {
        return getVerticies().stream()
                .map(vertex -> (PDGNode) vertex)
                .max(Comparator.comparingInt(PDGNode::getLevel))
                .map(PDGNode::getLevel)
                .get() + 1;
    }

    @Override
    public String toGraphvizRepresentation() {
        String lineSep = System.lineSeparator();

        String nodesDeclaration = getVerticies().stream()
                .map(vertex -> ((Node) vertex).toGraphvizRepresentation())
                .collect(Collectors.joining(lineSep));

        StringBuilder rankedNodes = new StringBuilder();

        // No level 0 is needed (only one node)
        for (int i = 0; i < getLevels(); i++) {
            List<PDGNode> levelNodes = getNodesAtLevel(i);

            if (levelNodes.size() <= 1) {
                continue;
            }

            // rank same
            rankedNodes.append("{ rank = same; ")
                    .append(levelNodes.stream()
                        .map(node -> String.valueOf(node.getId()))
                        .collect(Collectors.joining(";")))
                    .append(" }")
                    .append(lineSep);

            // invisible arrows for ordering
            rankedNodes.append(levelNodes.stream()
                        .sorted(Comparator.comparingInt(PDGNode::getId))
                        .map(node -> String.valueOf(node.getId()))
                        .collect(Collectors.joining(" -> ")))
                    .append("[style = invis];")
                    .append(lineSep);
        }

        String arrows =
                getArrows().stream()
                        .sorted(Comparator.comparingInt(arrow -> ((Node) arrow.getFrom()).getId()))
                        .map(arrow -> ((Arc) arrow).toGraphvizRepresentation())
                        .collect(Collectors.joining(lineSep));


        return "digraph g{" + lineSep +
                "splines=true;" + lineSep +
                nodesDeclaration + lineSep +
                arrows + lineSep +
                rankedNodes.toString() +
                "}";
    }
}
