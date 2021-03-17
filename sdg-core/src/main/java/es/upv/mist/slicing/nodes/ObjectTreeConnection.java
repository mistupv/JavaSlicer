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

/** A connection between two object trees. This object can specify the connection between two different
 *  levels of object trees, for example to represent the assignment {@code a.b = c.d}. */
class ObjectTreeConnection implements VariableAction.PDGConnection {

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

    /** Apply the connection represented by this object on an SDG. This means that all arcs will be interprocedural. */
    public void applySDG(JSysDG graph) {
        if (!applied) {
            connectTrees(graph, ParameterInOutArc::new, ParameterInOutArc.ObjectFlow::new);
            applied = true;
        }
    }

    @Override
    public void apply(JSysPDG graph) {
        if (!applied) {
            connectTrees(graph, FlowDependencyArc::new, ObjectFlowDependencyArc::new);
            applied = true;
        }
    }

    protected void connectTrees(Graph graph, Supplier<Arc> flowSupplier, Supplier<Arc> objFlowSupplier) {
        Supplier<Arc> valueSupplier = flowSupplier;
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
                graph.addEdge(rootSrc, rootTgt, valueSupplier.get());
        } else {
            graph.addEdge(rootSrc, rootTgt, objFlowSupplier.get());
            graph.addEdge(rootSrc, rootTgt, valueSupplier.get());
            for (ObjectTree tree : target.treeIterable()) {
                MemberNode src = source.getNodeForNonRoot(tree.getMemberName());
                MemberNode tgt = tree.getMemberNode();
                if (tree.hasChildren()) {
                    graph.addEdge(src, tgt, objFlowSupplier.get());
                    graph.addEdge(src, tgt, valueSupplier.get());
                } else
                    graph.addEdge(src, tgt, flowSupplier.get());
            }
        }
    }
}
