package tfm.graphs;

import tfm.arcs.Arc;
import tfm.arcs.pdg.ControlDependencyArc;
import tfm.arcs.pdg.DataDependencyArc;
import tfm.nodes.PDGNode;
import tfm.nodes.Node;
import tfm.variables.*;
import tfm.variables.actions.VariableDeclaration;
import tfm.variables.actions.VariableUse;
import tfm.variables.actions.VariableDefinition;

public abstract class PDGGraph extends Graph<PDGNode> {

    private VariableSet variableSet;

    public PDGGraph() {
        setRootVertex(new PDGNode(NodeId.getVertexId(), getRootNodeData()));

        variableSet = new VariableSet();
    }

    protected abstract String getRootNodeData();

    @Override
    public PDGNode addNode(String instruction) {
        PDGNode vertex = new PDGNode(NodeId.getVertexId(), instruction);
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
}
