package es.upv.mist.slicing.graphs.jsysdg;

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

    protected class Builder extends ESPDG.Builder {

        /** Computes all the data dependencies between {@link VariableAction variable actions} of this graph. */
        @Override
        protected void buildDataDependency() {
            addSyntheticNodesToPDG();
            super.buildDataDependency();
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
                    if (va instanceof VariableAction.Movable) {
                        GraphNode<?> realNode = ((VariableAction.Movable) va).getRealNode();
                        addVertex(realNode);
                        connectRealNode(node, callNodeStack.peek(), realNode);
                        parentNode = realNode;
                    } else if (va.getObjectTree().isEmpty() || node instanceof IONode) {
                        parentNode = node;
                    } else {
                        parentNode = new MemberNode(va.toString(), null);
                        addVertex(parentNode);
                        addControlDependencyArc(node, parentNode);
                    }
                    // Extract the member nodes contained within the object tree
                    for (MemberNode memberNode : va.getObjectTree().nodeIterable()) {
                        MemberNode memberParent = memberNode.getParent();
                        assert memberParent == null || containsVertex(memberParent);
                        addVertex(memberNode);
                        addControlDependencyArc(memberParent == null ? parentNode : memberParent, memberNode);
                    }
                }
                assert callNodeStack.isEmpty();
            }
        }
    }
}
