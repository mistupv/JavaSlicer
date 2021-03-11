package es.upv.mist.slicing.nodes;

import es.upv.mist.slicing.arcs.pdg.FlowDependencyArc;
import es.upv.mist.slicing.graphs.jsysdg.JSysPDG;

public class ValueConnection implements VariableAction.PDGConnection {
    protected final VariableAction action;
    protected final String member;

    public ValueConnection(VariableAction action, String member) {
        this.action = action;
        if (member.isEmpty())
            this.member = "-root-";
        else
            this.member = "-root-." + member;
    }

    @Override
    public void apply(JSysPDG graph) {
        GraphNode<?> statementNode;
        if (action instanceof VariableAction.Movable)
            statementNode = ((VariableAction.Movable) action).getRealNode();
        else
            statementNode = action.getGraphNode();
        if (action.hasTreeMember(member))
            graph.addEdge(action.getObjectTree().getNodeFor(member), statementNode, new FlowDependencyArc());
    }
}
