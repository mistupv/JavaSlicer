package es.upv.mist.slicing.graphs.pdg;

import com.github.javaparser.ast.body.CallableDeclaration;
import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.arcs.pdg.ControlDependencyArc;
import es.upv.mist.slicing.arcs.pdg.DataDependencyArc;
import es.upv.mist.slicing.graphs.GraphWithRootNode;
import es.upv.mist.slicing.graphs.cfg.CFG;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.VariableAction;
import es.upv.mist.slicing.nodes.io.ActualIONode;
import es.upv.mist.slicing.nodes.io.CallNode;

import java.util.List;
import java.util.Set;

/**
 * The <b>Program Dependence Graph</b> represents the statements of a method in
 * a graph, connecting statements according to their {@link ControlDependencyArc control}
 * and {@link DataDependencyArc data} relationships. You can build one manually or use
 * the {@link Builder PDGBuilder}.
 * The variations of the PDG are represented as child types.
 */
public class PDG extends GraphWithRootNode<CallableDeclaration<?>> {
    protected final CFG cfg;

    public PDG() {
        this(new CFG());
    }

    public PDG(CFG cfg) {
        this.cfg = cfg;
    }

    public void addControlDependencyArc(GraphNode<?> from, GraphNode<?> to) {
        this.addEdge(from, to, new ControlDependencyArc());
    }

    public void addDataDependencyArc(VariableAction src, VariableAction tgt) {
        addEdge(src.getGraphNode(), tgt.getGraphNode(), new DataDependencyArc(src, tgt));
    }

    public CFG getCfg() {
        return cfg;
    }

    @Override
    public void build(CallableDeclaration<?> declaration) {
        createBuilder().build(declaration);
        built = true;
    }

    /** Create a new PDG builder. Child classes that wish to alter the creation of the graph
     * should create a new PDG builder and override this method. */
    protected Builder createBuilder() {
        return new Builder();
    }

    /**
     * Populates a PDG, given an empty PDG, an AST declaration and, optionally, a complete {@link CFG}.
     * It can only accept a single {@link CallableDeclaration}, and can't be re-used. <br/>
     * Entry-point: {@link #build(CallableDeclaration)}
     */
    public class Builder {
        protected Builder() {
            assert PDG.this.getCfg() != null;
        }

        /**
         * Populates the PDG with the correct control and data dependencies.
         * If {@link VariableAction.Movable movable variable actions} are present,
         * they will be processed and moved to their real nodes. The CFG of the PDG object
         * will be built as a side-effect, if it hasn't been already.
         */
        public void build(CallableDeclaration<?> declaration) {
            buildAndCopyCFG(declaration); // 4.1
            buildControlDependency();     // 4.2
            buildDataDependency();        // 4.2
            expandCalls();                // 4.3
            assert incomingEdgesOf(cfg.getExitNode()).stream().noneMatch(Arc::isDataDependencyArc);
            removeVertex(cfg.getExitNode());
        }

        /** Builds the CFG, if necessary, and copies all elements to the PDG. */
        protected void buildAndCopyCFG(CallableDeclaration<?> declaration) {
            if (!cfg.isBuilt())
                cfg.build(declaration);
            cfg.vertexSet().forEach(PDG.this::addVertex);
            PDG.this.setRootNode(cfg.getRootNode());
        }

        /** Computes all the control dependencies between nodes of this graph. */
        protected void buildControlDependency() {
            new ControlDependencyBuilder(cfg, PDG.this).build();
        }

        /** Computes all the data dependencies between {@link VariableAction variable actions} of this graph. */
        protected void buildDataDependency() {
            for (GraphNode<?> node : vertexSet())
                for (VariableAction varAct : node.getVariableActions())
                    if (varAct.isUsage())
                        cfg.findLastDefinitionsFrom(varAct).forEach(def -> addDataDependencyArc(def, varAct));
                    else if (varAct.isDefinition() && !varAct.isSynthetic())
                        cfg.findDeclarationFor(varAct).ifPresent(dec -> addDataDependencyArc(dec, varAct));
        }

        /**
         * Creates nodes for connectable calls, and moves all {@link VariableAction.Movable movable
         * variable actions} to their proper destinations, adding them to the graph and connecting
         * them via control dependency to the node they were located at.
         */
        protected void expandCalls() {
            for (GraphNode<?> graphNode : Set.copyOf(vertexSet())) {
                CallNode callNode = null;
                for (VariableAction action : List.copyOf(graphNode.getVariableActions())) {
                    if (action instanceof VariableAction.CallMarker) {
                        callNode = updateCallNode(graphNode, (VariableAction.CallMarker) action);
                    } else if (action instanceof VariableAction.Movable) {
                        var movable = (VariableAction.Movable) action;
                        movable.move(PDG.this);
                        connectRealNode(graphNode, callNode, movable.getRealNode());
                    }
                }
                assert callNode == null;
            }
        }

        /** Compute the call node, if entering the marker. Additionally, it places the node
         * in the graph and makes it control-dependent on its container. */
        protected CallNode updateCallNode(GraphNode<?> graphNode, VariableAction.CallMarker marker) {
            if (!marker.isEnter())
                return null;
            var callNode = CallNode.create(marker.getCall());
            addVertex(callNode);
            addControlDependencyArc(graphNode, callNode);
            return callNode;
        }

        /** Connects the real node to the proper parent, control-dependent-wise. */
        protected void connectRealNode(GraphNode<?> graphNode, CallNode callNode, GraphNode<?> realNode) {
            if (realNode instanceof ActualIONode || realNode instanceof CallNode.Return) {
                assert callNode != null;
                addControlDependencyArc(callNode, realNode);
            } else {
                addControlDependencyArc(graphNode == cfg.getExitNode() ? rootNode : graphNode, realNode);
            }
        }
    }
}
