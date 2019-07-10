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
import tfm.slicing.SlicingCriterion;
import tfm.utils.Logger;
import tfm.utils.NodeNotFoundException;
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

    public PDGNode addNode(PDGNode node) {
        PDGNode vertex = new PDGNode(node);
        super.addVertex(vertex);

        return vertex;
    }

    @Override
    public PDGNode addNode(String instruction, Statement statement) {
        return addNode(getNextVertexId(), instruction, statement);
    }

    public PDGNode addNode(int id, String instruction, Statement statement) {
        PDGNode vertex = new PDGNode(id, instruction, statement);
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
    public PDGGraph slice(SlicingCriterion slicingCriterion) {
        Optional<PDGNode> optionalPDGNode = slicingCriterion.findNode(this);

        if (!optionalPDGNode.isPresent()) {
            throw new NodeNotFoundException(slicingCriterion);
        }

        PDGNode node = optionalPDGNode.get();

        Logger.format("Slicing node: %s", node);

        Set<Integer> sliceNodes = getSliceNodes(new HashSet<>(), node);

        PDGGraph sliceGraph = new PDGGraph();

        for (PDGNode graphNode : getNodes()) {
            sliceGraph.addNode(new PDGNode(graphNode.getId(), graphNode.getData(), graphNode.getAstNode().clone()));
        }

        for (PDGNode sliceNode : sliceGraph.getNodes()) {
            if (!sliceNodes.contains(sliceNode.getId())) {
                sliceNode.getAstNode().removeForced();
                sliceGraph.removeVertex(sliceNode);
            }
        }

        for (Arc arc : getArcs()) {
            Optional<PDGNode> fromOptional = sliceGraph.findNodeById(arc.getFromNode().getId());
            Optional<PDGNode> toOptional = sliceGraph.findNodeById(arc.getToNode().getId());

            if (fromOptional.isPresent() && toOptional.isPresent()) {
                PDGNode from = fromOptional.get();
                PDGNode to = toOptional.get();

                if (arc.isControlDependencyArrow()) {
                    sliceGraph.addControlDependencyArc(from, to);
                } else {
                    DataDependencyArc dataDependencyArc = (DataDependencyArc) arc;
                    sliceGraph.addDataDependencyArc(from, to, dataDependencyArc.getData().getVariables().get(0));
                }
            }
        }

        return sliceGraph;
    }

    private Set<Integer> getSliceNodes(Set<Integer> visited, PDGNode root) {
        visited.add(root.getId());

//        Set<String> searchVariables = new HashSet<>(variables);

        for (Arrow arrow : root.getIncomingArrows()) {
            Arc arc = (Arc) arrow;

//            if (arc.isDataDependencyArrow()
//                    && Collections.disjoint(((DataDependencyArc) arc).getData().getVariables(), searchVariables)) {
//                continue;
//            }

            PDGNode from = (PDGNode) arc.getFromNode();

//            Logger.log("Arrow from node: " + from);

            if (visited.contains(from.getId())) {
//                Logger.log("It's already visited. Continuing...");
                continue;
            }

            getSliceNodes(visited, from);
        }

//        Logger.format("Done with node %s", root.getId());

        return visited;
    }
}
