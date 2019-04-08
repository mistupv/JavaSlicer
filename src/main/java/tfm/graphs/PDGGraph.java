package tfm.graphs;

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

import java.util.*;
import java.util.stream.Collectors;

public abstract class PDGGraph extends Graph<PDGNode> {

    private VariableSet variableSet;

    public PDGGraph() {
        setRootVertex(new PDGNode(NodeId.getVertexId(), getRootNodeData(), 0));

        variableSet = new VariableSet();
    }

    protected abstract String getRootNodeData();

    @Override
    public PDGNode addNode(String instruction, int fileNumber) {
        PDGNode vertex = new PDGNode(NodeId.getVertexId(), instruction, fileNumber);
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

    public boolean containsVariable(String name) {
        return variableSet.containsVariable(name);
    }

    public Variable addNewVariable(String name, Node declarationNode) {
        return variableSet.addVariable(name, new VariableDeclaration(declarationNode));
    }

    public void addVariableDefinition(String variable, Node currentNode) {
        variableSet.addDefinition(variable, new VariableDefinition(currentNode));
    }

    public void addVariableUse(String variable, Node currentNode) {
        variableSet.addUse(variable, new VariableUse(currentNode));
    }

    public VariableSet getVariableSet() {
        return variableSet;
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
