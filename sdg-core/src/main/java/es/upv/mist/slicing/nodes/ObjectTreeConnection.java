package es.upv.mist.slicing.nodes;

import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.arcs.pdg.FlowDependencyArc;
import es.upv.mist.slicing.arcs.pdg.ObjectFlowDependencyArc;
import es.upv.mist.slicing.arcs.sdg.ParameterInOutArc;
import es.upv.mist.slicing.graphs.Graph;
import es.upv.mist.slicing.graphs.jsysdg.JSysDG;
import es.upv.mist.slicing.graphs.jsysdg.JSysPDG;
import es.upv.mist.slicing.nodes.oo.MemberNode;

import java.util.function.Supplier;

class ObjectTreeConnection {
    protected final VariableAction sourceAction;
    protected final VariableAction targetAction;
    protected final String sourceMember;
    protected final String targetMember;

    protected boolean applied = false;

    public ObjectTreeConnection(VariableAction sourceAction, VariableAction targetAction, String sourceMember, String targetMember) {
        this.sourceAction = sourceAction;
        this.targetAction = targetAction;
        this.sourceMember = sourceMember;
        this.targetMember = targetMember;
    }

    public void applySDG(JSysDG graph) {
        if (!applied) {
            connectTrees(graph, ParameterInOutArc::new, ParameterInOutArc.ObjectFlow::new);
            applied = true;
        }
    }

    public void applyPDG(JSysPDG graph) {
        if (!applied) {
            connectTrees(graph, FlowDependencyArc::new, ObjectFlowDependencyArc::new);
            applied = true;
        }
    }

    protected void connectTrees(Graph graph, Supplier<Arc> flowSupplier, Supplier<Arc> objFlowSupplier) {
        ObjectTree source = null, target = null;
        GraphNode<?> rootSrc, rootTgt;
        assert sourceMember.isEmpty() || sourceAction.hasObjectTree();
        assert targetMember.isEmpty() || targetAction.hasObjectTree();
        if (sourceAction.hasObjectTree()) {
            source = sourceAction.getObjectTree().findObjectTreeOfMember(sourceMember);
            rootSrc = source.getMemberNode();
        } else {
            rootSrc = sourceAction.getGraphNode();
        }
        if (targetAction.hasObjectTree()) {
            target = targetAction.getObjectTree().findObjectTreeOfMember(targetMember);
            rootTgt = target.getMemberNode();
        } else {
            rootTgt = targetAction.getGraphNode();
        }
        if (source == null || target == null) {
            if (!rootSrc.equals(rootTgt))
                graph.addEdge(rootSrc, rootTgt, new FlowDependencyArc()); // VALUE DEPENDENCE
        } else {
            graph.addEdge(rootSrc, rootTgt, objFlowSupplier.get());
            for (ObjectTree tree : target.treeIterable()) {
                MemberNode src = source.getNodeForNonRoot(tree.getMemberName());
                MemberNode tgt = tree.getMemberNode();
                if (tree.hasChildren())
                    graph.addEdge(src, tgt, objFlowSupplier.get());
                else
                    graph.addEdge(src, tgt, flowSupplier.get());
            }
        }
    }
}
