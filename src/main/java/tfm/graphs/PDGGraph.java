package tfm.graphs;

import com.github.javaparser.Position;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.Statement;
import edg.graphlib.Arrow;
import edg.graphlib.Vertex;
import edg.graphlib.Visitor;
import tfm.arcs.Arc;
import tfm.arcs.cfg.ControlFlowArc;
import tfm.arcs.data.ArcData;
import tfm.arcs.pdg.ControlDependencyArc;
import tfm.arcs.pdg.DataDependencyArc;
import tfm.nodes.CFGNode;
import tfm.nodes.PDGNode;
import tfm.nodes.Node;
import tfm.utils.Logger;
import tfm.variables.*;
import tfm.variables.actions.VariableDeclaration;
import tfm.variables.actions.VariableUse;
import tfm.variables.actions.VariableDefinition;

import javax.swing.plaf.nimbus.State;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PDGGraph extends Graph<PDGNode> {

    public PDGGraph() {
        setRootVertex(new PDGNode(getNextVertexId(), getRootNodeData(), new EmptyStmt()));
    }

    protected String getRootNodeData() {
        return "Entry";
    }

    public <N extends Node> PDGNode addNode(N node) {
        PDGNode vertex = new PDGNode(getNextVertexId(), node);
        super.addVertex(vertex);

        return vertex;
    }

    @Override
    public PDGNode addNode(String instruction, Statement statement) {
        PDGNode vertex = new PDGNode(getNextVertexId(), instruction, statement);
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

        String nodesDeclaration = getNodes().stream()
                .sorted(Comparator.comparingInt(Node::getId))
                .map(Node::toGraphvizRepresentation)
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
                getArcs().stream()
                        .sorted(Comparator.comparingInt(arrow -> ((Node) arrow.getFrom()).getId()))
                        .map(Arc::toGraphvizRepresentation)
                        .collect(Collectors.joining(lineSep));


        return "digraph g{" + lineSep +
                "splines=true;" + lineSep +
                nodesDeclaration + lineSep +
                arrows + lineSep +
                rankedNodes.toString() +
                "}";
    }

    @Override
    public Set<PDGNode> slice(String variable, int lineNumber) {
        PDGNode sliceNode = null;

        // find node by line number
        for (PDGNode node : getNodes()) {
            Statement statement = node.getAstNode();

            if (!statement.getBegin().isPresent() || !statement.getEnd().isPresent())
                continue;

            int begin = statement.getBegin().get().line;
            int end = statement.getEnd().get().line;

            Logger.format("begin %s end %s", begin, end);

            if (lineNumber == begin || lineNumber == end) {
                sliceNode = node;
                break;
            }
        }

        if (sliceNode == null) {
            Logger.format("Warning: Slicing node not found for slicing criterion: (%s, %s)", variable, lineNumber);
            return new HashSet<>();
        }

        Logger.log("Slice node: " + sliceNode);

        return getSliceNodes(new HashSet<>(), sliceNode);
    }

    private Set<PDGNode> getSliceNodes(Set<PDGNode> visited, PDGNode root) {
        visited.add(root);

        for (Arrow arrow : root.getIncomingArrows()) {
            Arc arc = (Arc) arrow;

            PDGNode from = (PDGNode) arc.getFromNode();

            Logger.log("Arrow from node: " + from);

            if (visited.contains(from)) {
                Logger.log("It's already visited. Continuing...");
                continue;
            }

            getSliceNodes(visited, from);
        }

        Logger.format("Done with node %s", root.getId());

        return visited;
    }
}
