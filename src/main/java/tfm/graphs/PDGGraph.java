package tfm.graphs;

import tfm.arcs.Arc;
import tfm.arcs.pdg.ControlDependencyArc;
import tfm.arcs.pdg.DataDependencyArc;
import tfm.nodes.PDGVertex;
import tfm.nodes.Vertex;
import tfm.variables.*;
import tfm.variables.actions.VariableDeclaration;
import tfm.variables.actions.VariableUse;
import tfm.variables.actions.VariableDefinition;

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

    public void addVariableDefinition(String variable, Vertex currentNode) {
        variableSet.addDefinition(variable, new VariableDefinition(currentNode));
    }

    public void addVariableUse(String variable, Vertex currentNode) {
        variableSet.addUse(variable, new VariableUse(currentNode));
    }

    public VariableSet getVariableSet() {
        return variableSet;
    }
}
