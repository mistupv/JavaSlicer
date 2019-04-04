package tfm.graphs;

import tfm.arcs.Arc;
import tfm.arcs.pdg.ControlDependencyArc;
import tfm.arcs.pdg.DataDependencyArc;
import tfm.nodes.PDGVertex;
import tfm.nodes.Vertex;
import tfm.variables.*;
import tfm.variables.actions.VariableAction;
import tfm.variables.actions.VariableDeclaration;
import tfm.variables.actions.VariableRead;
import tfm.variables.actions.VariableWrite;

import java.util.*;
import java.util.stream.Collectors;

public abstract class PDGGraph extends Graph<PDGVertex> {

    private VariableSet variableSet;

    public PDGGraph() {
        setRootVertex(new PDGVertex(VertexId.getVertexId(), getRootNodeData()));

        variableSet = new VariableSet();
    }

    protected abstract String getRootNodeData();

    @Override
    public PDGVertex addVertex(String instruction) {
        PDGVertex vertex = new PDGVertex(VertexId.getVertexId(), instruction);
        super.addVertex(vertex);

        return vertex;
    }

    @SuppressWarnings("unchecked")
    private void addArc(Arc arc) {
        super.addEdge(arc);
    }

    public void addControlDependencyArc(PDGVertex from, PDGVertex to) {
        ControlDependencyArc controlDependencyArc = new ControlDependencyArc(from, to);

        this.addArc(controlDependencyArc);
    }

    public void addDataDependencyArc(PDGVertex from, PDGVertex to, String variable) {
        DataDependencyArc dataDataDependencyArc = new DataDependencyArc(from, to, variable);

        this.addArc(dataDataDependencyArc);
    }

    public boolean containsVariable(String name) {
        return variableSet.containsVariable(name);
    }

    public Variable addNewVariable(String name, Vertex declarationNode) {
        return variableSet.addVariable(name, new VariableDeclaration(declarationNode));
    }

    public void addVariableWrite(String variable, Vertex currentNode) {
        variableSet.addWrite(variable, new VariableWrite(currentNode));
    }

    public void addVariableRead(String variable, Vertex currentNode) {
        variableSet.addRead(variable, new VariableRead(currentNode));
    }

    public VariableSet getVariableSet() {
        return variableSet;
    }
}
