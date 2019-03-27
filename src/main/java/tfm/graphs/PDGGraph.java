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

    public <T> Variable<T> addNewVariable(String name, T value, Vertex declarationNode) {
        Variable<T> variable = new Variable<>(new VariableDeclaration<>(declarationNode, value), name);
        variableSet.addVariable(variable);

        return variable;
    }

    public <T> void addVariableWrite(Vertex currentNode, T newValue, String variable) {
        variableSet.findVariableByName(variable)
                .ifPresent(objectVariable -> objectVariable.addWrite(new VariableWrite<>(currentNode, newValue)));
    }

    public <T> void addVariableRead(Vertex currentNode, T currentValue, String variable) {
        variableSet.findVariableByName(variable)
                .ifPresent(objectVariable -> objectVariable.addRead(new VariableRead<>(currentNode, currentValue)));
    }

    public VariableSet getVariableSet() {
        return variableSet;
    }
}
