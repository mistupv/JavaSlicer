package es.upv.mist.slicing.graphs.jsysdg;

import es.upv.mist.slicing.arcs.pdg.FlowDependencyArc;
import es.upv.mist.slicing.arcs.pdg.ObjectFlowDependencyArc;
import es.upv.mist.slicing.arcs.pdg.StructuralArc;
import es.upv.mist.slicing.arcs.pdg.TotalDefinitionDependenceArc;
import es.upv.mist.slicing.graphs.exceptionsensitive.ESCFG;
import es.upv.mist.slicing.graphs.exceptionsensitive.ESPDG;
import es.upv.mist.slicing.graphs.pdg.PDG;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.VariableAction;
import es.upv.mist.slicing.nodes.io.ActualIONode;
import es.upv.mist.slicing.nodes.io.CallNode;
import es.upv.mist.slicing.nodes.oo.MemberNode;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import static es.upv.mist.slicing.nodes.ObjectTree.ROOT_NAME;
import static es.upv.mist.slicing.nodes.ObjectTree.ROOT_NODE;

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

    protected void addStructuralArc(GraphNode<?> source, GraphNode<?> target) {
        addEdge(source, target, new StructuralArc());
    }

    // definicion de raiz --object-flow--> uso de raiz
    protected void addObjectFlowDependencyArc(VariableAction definition, VariableAction usage) {
        addEdge(graphNodeOf(definition), graphNodeOf(usage), new ObjectFlowDependencyArc());
    }

    // definicion de miembro --object-flow--> definicion de raiz
    protected void addObjectFlowDependencyArc(VariableAction nextDefinitionRoot, String[] memberDefined, VariableAction definition) {
        MemberNode defMember = definition.getObjectTree().getNodeFor(true, memberDefined);
        addEdge(defMember, graphNodeOf(nextDefinitionRoot), new ObjectFlowDependencyArc());
    }

    // def --flow--> use || dec --flow--> def
    protected void addFlowDependencyArc(VariableAction source, VariableAction target) {
        addEdge(graphNodeOf(source), graphNodeOf(target), new FlowDependencyArc(source.getName()));
    }

    protected void addDeclarationFlowDependencyArc(VariableAction declaration, VariableAction definition) {
        for (MemberNode target : definition.getObjectTree().getNodesForPoly(declaration.getName()))
            addEdge(graphNodeOf(declaration), target, new FlowDependencyArc());
    }

    // definicion de miembro --flow--> uso de miembro
    protected void addFlowDependencyArc(VariableAction definition, VariableAction usage, String[] objMember) {
        GraphNode<?> defMember = definition.getObjectTree().getNodeFor(true, objMember);
        GraphNode<?> useMember = usage.getObjectTree().getNodeFor(true, objMember);
        addEdge(defMember, useMember, new FlowDependencyArc(objMember));
    }

    protected void addValueDependencyArc(VariableAction usage, GraphNode<?> statement) {
        addEdge(usage.getObjectTree().getMemberNode(), statement, new FlowDependencyArc(ROOT_NAME));
    }

    protected void addTotalDefinitionDependencyArc(VariableAction totalDefinition, VariableAction target, String[] member) {
        if (member.length == 1 && member[0].equals(ROOT_NAME))
            addEdge(graphNodeOf(totalDefinition), graphNodeOf(target), new TotalDefinitionDependenceArc());
        else
            addEdge(totalDefinition.getObjectTree().getNodeFor(true, member),
                    target.getObjectTree().getNodeFor(true, member),
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

        @Override
        protected void buildDataDependency() {
            addSyntheticNodesToPDG();
            applyTreeConnections();
            buildJSysDataDependency();
            valueDependencyForThrowStatements();
        }

        /** Compute flow, object flow and total definition dependence. */
        protected void buildJSysDataDependency() {
            JSysCFG jSysCFG = (JSysCFG) cfg;
            for (GraphNode<?> node : vertexSet()) {
                for (VariableAction varAct : node.getVariableActions()) {
                    // Total definition dependence
                    buildTotalDefinitionDependence(jSysCFG, varAct);
                    if (varAct.isUsage())
                        buildUsageDependencies(jSysCFG, varAct);
                    else if (varAct.isDefinition())
                        buildDefinitionDependencies(jSysCFG, varAct);
                    else if (varAct.isDeclaration())
                        buildDeclarationDependencies(jSysCFG, varAct);
                }
            }
        }

        /** Generate total definition dependence. Only generated for non-primitive usages and non-primitive,
         *  non-synthetic definitions. Connects each member to its previous total definition. */
        private void buildTotalDefinitionDependence(JSysCFG jSysCFG, VariableAction varAct) {
            if (!varAct.isPrimitive() && (varAct.isUsage() || (varAct.isDefinition() && !varAct.isSynthetic()))) {
                jSysCFG.findLastTotalDefinitionOf(varAct, ROOT_NODE).forEach(totalDef -> addTotalDefinitionDependencyArc(totalDef, varAct, ROOT_NODE));
                if (!varAct.hasObjectTree())
                    return;
                for (String[] member : varAct.getObjectTree().nameAsArrayIterable())
                    jSysCFG.findLastTotalDefinitionOf(varAct, member).forEach(totalDef -> addTotalDefinitionDependencyArc(totalDef, varAct, member));
            }
        }

        /** Generate dependencies to usages, including flow dependency for primitives,
         *  object flow for object roots and flow for object members. */
        private void buildUsageDependencies(JSysCFG jSysCFG, VariableAction varAct) {
            if (varAct.isPrimitive()) {
                jSysCFG.findLastDefinitionOfPrimitive(varAct).forEach(def -> addFlowDependencyArc(def, varAct));
            } else {
                jSysCFG.findLastDefinitionOfObjectRoot(varAct).forEach(def -> addObjectFlowDependencyArc(def, varAct));
                if (!varAct.hasObjectTree())
                    return;
                for (String[] member : varAct.getObjectTree().nameAsArrayIterable())
                    jSysCFG.findLastDefinitionOfObjectMember(varAct, member).forEach(def -> addFlowDependencyArc(def, varAct, member));
            }
        }

        /** Generates dec --> def flow and def --> def object flow dependencies. */
        private void buildDefinitionDependencies(JSysCFG jSysCFG, VariableAction varAct) {
            // Flow declaration --> definition
            if (!varAct.isSynthetic())
                jSysCFG.findDeclarationFor(varAct).ifPresent(dec -> addFlowDependencyArc(dec, varAct));
            // Object flow definition --> definition
            if (varAct.isPrimitive() || !varAct.hasObjectTree())
                return;
            for (String[] member : varAct.getObjectTree().nameAsArrayIterable())
                jSysCFG.findNextObjectDefinitionsFor(varAct, member).forEach(def -> addObjectFlowDependencyArc(varAct, member, def));
        }

        /** Generates dec --> def declaration dependencies for objects (constructors only). */
        private void buildDeclarationDependencies(JSysCFG jSysCFG, VariableAction varAct) {
            if (!varAct.getName().startsWith("this."))
                return;
            jSysCFG.findAllFutureObjectDefinitionsFor(varAct).forEach(def -> addDeclarationFlowDependencyArc(varAct, def));
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

        /** Add movables to the PDG, and all MemberNodes contained in object trees. */
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
                            addStructuralArc(node, callNode);
                            callNodeStack.push(callNode);
                        }
                        continue;
                    }
                    GraphNode<?> parentNode; // node that represents the root of the object tree
                    if (va instanceof VariableAction.Movable) {
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

        @Override
        protected void connectRealNode(GraphNode<?> graphNode, CallNode callNode, GraphNode<?> realNode) {
            if (realNode instanceof ActualIONode || realNode instanceof CallNode.Return) {
                assert callNode != null;
                addStructuralArc(callNode, realNode);
            } else {
                addStructuralArc(graphNode == cfg.getExitNode() ? rootNode : graphNode, realNode);
            }
        }

        /** Apply the pre-assigned connections between object trees. */
        protected void applyTreeConnections() {
            cfg.vertexSet().stream()
                    .flatMap(node -> node.getVariableActions().stream())
                    .forEach(va -> va.applyPDGTreeConnections(JSysPDG.this));
        }

        /** Inserts a member node from an object tree onto the PDG. */
        protected void insertMemberNode(MemberNode memberNode, GraphNode<?> parentNode) {
            if (memberNode.getParent() == null)
                memberNode.setParent(parentNode);
            assert containsVertex(memberNode.getParent());
            addVertex(memberNode);
            addStructuralArc(memberNode.getParent(), memberNode);
        }

        /** Connects the tree that represents the active exception to its parent graph node. */
        protected void valueDependencyForThrowStatements() {
            for (GraphNode<?> node : vertexSet())
                for (VariableAction action : node.getVariableActions())
                    if (action.isDefinition()
                            && action.hasObjectTree()
                            && action.getName().equals(ESCFG.ACTIVE_EXCEPTION_VARIABLE))
                        addValueDependencyArc(action, node);
        }
    }
}
