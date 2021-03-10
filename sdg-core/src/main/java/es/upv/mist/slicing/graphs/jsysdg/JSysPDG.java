package es.upv.mist.slicing.graphs.jsysdg;

import es.upv.mist.slicing.arcs.pdg.FlowDependencyArc;
import es.upv.mist.slicing.arcs.pdg.ObjectFlowDependencyArc;
import es.upv.mist.slicing.arcs.pdg.TotalDefinitionDependenceArc;
import es.upv.mist.slicing.graphs.exceptionsensitive.ESCFG;
import es.upv.mist.slicing.graphs.exceptionsensitive.ESPDG;
import es.upv.mist.slicing.graphs.pdg.PDG;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.ObjectTree;
import es.upv.mist.slicing.nodes.VariableAction;
import es.upv.mist.slicing.nodes.io.CallNode;
import es.upv.mist.slicing.nodes.oo.MemberNode;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

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
        addEdge(graphNodeOf(source), graphNodeOf(target), new FlowDependencyArc(source.getName()));
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

    protected void addTotalDefinitionDependencyArc(VariableAction totalDefinition, VariableAction target, String member) {
        if (member.equals(ObjectTree.ROOT_NAME))
            addEdge(graphNodeOf(totalDefinition), graphNodeOf(target), new TotalDefinitionDependenceArc());
        else
            addEdge(totalDefinition.getObjectTree().getNodeFor(member),
                    target.getObjectTree().getNodeFor(member),
                    new TotalDefinitionDependenceArc());
    }

    protected GraphNode<?> graphNodeOf(VariableAction action) {
        if (action.hasObjectTree())
            return action.getObjectTree().getMemberNode();
        if (action instanceof VariableAction.Movable)
            return ((VariableAction.Movable) action).getRealNode();
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
            applyTreeConnections();
            buildJSysDataDependency();
            valueDependencyForThrowStatements();
        }

        protected void buildJSysDataDependency() {
            JSysCFG jSysCFG = (JSysCFG) cfg;
            for (GraphNode<?> node : vertexSet()) {
                for (VariableAction varAct : node.getVariableActions()) {
                    // Total definition dependence
                    if (varAct.isUsage() || varAct.isDefinition()) {
                        // root
                        jSysCFG.findLastTotalDefinitionOf(varAct, "-root-").forEach(totalDef -> addTotalDefinitionDependencyArc(totalDef, varAct, "-root-"));
                        // members
                        if (varAct.hasObjectTree())
                            for (String member : varAct.getObjectTree().nameIterable())
                                jSysCFG.findLastTotalDefinitionOf(varAct, member).forEach(totalDef -> addTotalDefinitionDependencyArc(totalDef, varAct, member));
                    }
                    // Object flow, flow and declaration-definition dependencies
                    if (varAct.isUsage()) {
                        if (varAct.isPrimitive())
                            jSysCFG.findLastDefinitionOfPrimitive(varAct).forEach(def -> addFlowDependencyArc(def, varAct));
                        else {
                            jSysCFG.findLastDefinitionOfObjectRoot(varAct).forEach(def -> addObjectFlowDependencyArc(def, varAct));
                            if (varAct.hasObjectTree())
                                for (String member : varAct.getObjectTree().nameIterable())
                                    jSysCFG.findLastDefinitionOfObjectMember(varAct, member).forEach(def -> addFlowDependencyArc(def, varAct, member));
                        }
                    } else if (varAct.isDefinition()) {
                        if (!varAct.isSynthetic())
                            jSysCFG.findDeclarationFor(varAct).ifPresent(dec -> addFlowDependencyArc(dec, varAct));
                        if (!varAct.isPrimitive() && varAct.hasObjectTree())
                            for (String member : varAct.getObjectTree().nameIterable())
                                jSysCFG.findNextObjectDefinitionsFor(varAct, member).forEach(def -> addObjectFlowDependencyArc(varAct, member, def));
                    }
                }
            }
        }

        @Override
        protected void expandCalls() {
            for (GraphNode<?> graphNode : vertexSet()) {
                for (VariableAction action : List.copyOf(graphNode.getVariableActions())) {
                    if (action instanceof VariableAction.Movable) {
                        ((VariableAction.Movable) action).moveOnly();
                    }
                }
            }
        }

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
                        VariableAction.Movable movable = (VariableAction.Movable) va;
                        addVertex(movable.getRealNode());
                        connectRealNode(node, callNodeStack.peek(), movable.getRealNode());
                        parentNode = movable.getRealNode();
                    } else {
                        parentNode = node;
                    }
                    if (!va.hasObjectTree())
                        continue;
                    // Extract the member nodes contained within the object tree
                    insertMemberNode(va.getObjectTree().getMemberNode(), parentNode);
                    for (MemberNode memberNode : va.getObjectTree().nodeIterable()) {
                        insertMemberNode(memberNode, parentNode);
                    }
                }
                assert callNodeStack.isEmpty();
            }
        }

        protected void applyTreeConnections() {
            cfg.vertexSet().stream()
                    .flatMap(node -> node.getVariableActions().stream())
                    .forEach(va -> va.applyPDGTreeConnections(JSysPDG.this));
        }

        protected void insertMemberNode(MemberNode memberNode, GraphNode<?> parentNode) {
            if (memberNode.getParent() == null)
                memberNode.setParent(parentNode);
            assert containsVertex(memberNode.getParent());
            addVertex(memberNode);
            addControlDependencyArc(memberNode.getParent(), memberNode);
        }

        protected void valueDependencyForThrowStatements() {
            for (GraphNode<?> node : vertexSet()) {
                for (VariableAction action : node.getVariableActions()) {
                    if (action.isDefinition() && action.getName().equals(ESCFG.ACTIVE_EXCEPTION_VARIABLE)) {
                        addValueDependencyArc(action, "-root-", node);
                    }
                }
            }
        }
    }
}
