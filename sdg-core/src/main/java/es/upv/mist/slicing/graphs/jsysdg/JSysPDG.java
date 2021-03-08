package es.upv.mist.slicing.graphs.jsysdg;

import es.upv.mist.slicing.arcs.pdg.FlowDependencyArc;
import es.upv.mist.slicing.arcs.pdg.ObjectFlowDependencyArc;
import es.upv.mist.slicing.graphs.exceptionsensitive.ESPDG;
import es.upv.mist.slicing.graphs.pdg.PDG;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.VariableAction;
import es.upv.mist.slicing.nodes.io.CallNode;
import es.upv.mist.slicing.nodes.io.IONode;
import es.upv.mist.slicing.nodes.oo.MemberNode;

import java.util.Deque;
import java.util.LinkedList;

public class JSysPDG extends ESPDG {
    public JSysPDG() {
        this(new JSysCFG());
    }

    public JSysPDG(JSysCFG cfg) {
        super(cfg);
    }

    @Override
    protected PDG.Builder createBuilder() {
        return new Builder();
    }

    // definicion de raiz --object-flow--> uso de raiz
    protected void addObjectFlowDependencyArc(VariableAction definition, VariableAction usage) {
        addEdge(graphNodeOf(definition), graphNodeOf(usage), new ObjectFlowDependencyArc());
    }

    // definicion de miembro --object-flow--> definicion de raiz
    protected void addObjectFlowDependencyArc(VariableAction nextDefinitionRoot, String memberDefined, VariableAction definition) {
        MemberNode defMember = definition.getObjectTree().getNodeFor(memberDefined);
        addEdge(defMember, graphNodeOf(nextDefinitionRoot), new ObjectFlowDependencyArc());
    }

    // def --flow--> use || dec --flow--> def
    protected void addFlowDependencyArc(VariableAction source, VariableAction target) {
        addEdge(graphNodeOf(source), graphNodeOf(target), new FlowDependencyArc(source.getVariable()));
    }

    // definicion de miembro --flow--> uso de miembro
    protected void addFlowDependencyArc(VariableAction definition, VariableAction usage, String objMember) {
        GraphNode<?> defMember = definition.getObjectTree().getNodeFor(objMember);
        GraphNode<?> useMember = usage.getObjectTree().getNodeFor(objMember);
        addEdge(defMember, useMember, new FlowDependencyArc(objMember));
    }

    protected void addValueDependencyArc(VariableAction usage, String member, GraphNode<?> statement) {
        addEdge(usage.getObjectTree().getNodeFor(member), statement, new FlowDependencyArc(member));
    }

    protected GraphNode<?> graphNodeOf(VariableAction action) {
        if (action instanceof VariableAction.Movable)
            return ((VariableAction.Movable) action).getRealNode();
        if (action.getObjectTree().getMemberNode() != null)
            return action.getObjectTree().getMemberNode();
        return action.getGraphNode();
    }

    @Override
    public void addDataDependencyArc(VariableAction src, VariableAction tgt) {
        throw new UnsupportedOperationException("Use flow or object-flow dependency");
    }

    protected class Builder extends ESPDG.Builder {

        /** Computes all the data dependencies between {@link VariableAction variable actions} of this graph. */
        @Override
        protected void buildDataDependency() {
            addSyntheticNodesToPDG();
            JSysCFG jSysCFG = (JSysCFG) cfg;
            for (GraphNode<?> node : vertexSet())
                for (VariableAction varAct : node.getVariableActions())
                    if (varAct.isUsage()) {
                        if (varAct.isPrimitive())
                            jSysCFG.findLastDefinitionOfPrimitive(varAct).forEach(def -> addFlowDependencyArc(def, varAct));
                        else {
                            jSysCFG.findLastDefinitionOfObjectRoot(varAct).forEach(def -> addObjectFlowDependencyArc(def, varAct));
                            for (String member : varAct.getObjectTree().nameIterable()) {
                                jSysCFG.findLastDefinitionOfObjectMember(varAct, member).forEach(def -> addFlowDependencyArc(def, varAct, member));
                                addValueDependencyArc(varAct, member, node);
                            }
                        }
                    } else if (varAct.isDefinition()) {
                        if (!varAct.isSynthetic())
                            jSysCFG.findDeclarationFor(varAct).ifPresent(dec -> addFlowDependencyArc(dec, varAct));
                        if (!varAct.isPrimitive())
                            for (String member : varAct.getObjectTree().nameIterable())
                                jSysCFG.findNextObjectDefinitionsFor(varAct, member).forEach(def -> addObjectFlowDependencyArc(varAct, member, def));
                    }
        }

        @Override
        protected void expandCalls() {}

        protected void addSyntheticNodesToPDG() {
            for (GraphNode<?> node : cfg.vertexSet()) {
                Deque<CallNode> callNodeStack = new LinkedList<>();
                for (VariableAction va : node.getVariableActions()) {
                    if (va instanceof VariableAction.CallMarker) {
                        // Compute the call node, if entering the marker. Additionally, it places the node
                        // in the graph and makes it control-dependent on its container.
                        if (!((VariableAction.CallMarker) va).isEnter()) {
                            callNodeStack.pop();
                        } else {
                            CallNode callNode = CallNode.create(((VariableAction.CallMarker) va).getCall());
                            if (node.isImplicitInstruction())
                                callNode.markAsImplicit();
                            addVertex(callNode);
                            addControlDependencyArc(node, callNode);
                            callNodeStack.push(callNode);
                        }
                        continue;
                    }
                    GraphNode<?> parentNode; // node that represents the root of the object tree
                    if (va instanceof VariableAction.Movable) { // TODO: there are Movables with multiple VA per movable!!!
                        GraphNode<?> realNode = ((VariableAction.Movable) va).getRealNode();
                        addVertex(realNode);
                        connectRealNode(node, callNodeStack.peek(), realNode);
                        parentNode = realNode;
                    } else if (va.getObjectTree().isEmpty() || node instanceof IONode) {
                        parentNode = node;
                    } else {
                        MemberNode memberNode = new MemberNode(va.toString(), null);
                        va.getObjectTree().setMemberNode(memberNode);
                        parentNode = memberNode;
                        addVertex(parentNode);
                        addControlDependencyArc(node, parentNode);
                    }
                    // Extract the member nodes contained within the object tree
                    for (MemberNode memberNode : va.getObjectTree().nodeIterable()) {
                        if (memberNode.getParent() == null)
                            memberNode.setParent(parentNode);
                        assert containsVertex(memberNode.getParent());
                        addVertex(memberNode);
                        addControlDependencyArc(memberNode.getParent(), memberNode);
                    }
                }
                assert callNodeStack.isEmpty();
            }
            // Create the pre-established connections
            cfg.vertexSet().stream()
                    .flatMap(node -> node.getVariableActions().stream())
                    .forEach(va -> va.applyPDGTreeConnections(JSysPDG.this));
        }
    }
}
