package es.upv.mist.slicing.graphs.exceptionsensitive;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.CatchClause;
import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.arcs.pdg.ConditionalControlDependencyArc;
import es.upv.mist.slicing.arcs.pdg.ControlDependencyArc;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.exceptionsensitive.ExceptionReturnNode;
import es.upv.mist.slicing.nodes.exceptionsensitive.NormalReturnNode;
import es.upv.mist.slicing.nodes.exceptionsensitive.ReturnNode;
import es.upv.mist.slicing.slicing.PseudoPredicateSlicingAlgorithm;
import es.upv.mist.slicing.utils.ASTUtils;
import es.upv.mist.slicing.utils.Utils;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Adds {@link ConditionalControlDependencyArc CCD arcs} to a
 * {@link ESPDG ES-PDG}, according to the algorithm
 * outlined in Algorithm I of [SAS2020]. All auxiliary functions are implemented
 * here ({@link #getBlockInstructs(CatchClause) getBlockInstructs/1}, {@link
 * #getTryBlockInstructs(CatchClause) getTryBlockInstructs/1}, {@link
 * #isExceptionSource(GraphNode) isExceptionSource/1}), except {@link
 * ESSDG#isPseudoPredicate(GraphNode) isPseudoPredicate/1}. <br/>
 * <b>[SAS2020]</b>: dinsa://Areas/Program Slicing/Trabajos/Slicing Exceptions/Papers/SAS 2020
 * @see ConditionalControlDependencyArc
 */
public class ConditionalControlDependencyBuilder {
    protected ESCFG cfg;
    protected ESPDG pdg;

    public ConditionalControlDependencyBuilder(ESCFG cfg, ESPDG pdg) {
        this.cfg = Objects.requireNonNull(cfg);
        this.pdg = Objects.requireNonNull(pdg);
    }

    /** Adds the {@link ConditionalControlDependencyArc CCD arcs}. This method should only be called
     * once per {@link ESPDG}, as multiple executions may create duplicate arcs. */
    @SuppressWarnings("unchecked")
    public void build() {
        for (GraphNode<?> node : pdg.vertexSet()) {
            if (node.getAstNode() instanceof CatchClause) {
                buildCC1((GraphNode<CatchClause>) node);
                buildCC2((GraphNode<CatchClause>) node);
            }
        }
    }

    /** Create the {@link ConditionalControlDependencyArc.CC1 CC1} arcs associated to a given {@link
     * CatchClause}. This process removes some {@link ControlDependencyArc CD arcs} in the process. */
    protected void buildCC1(GraphNode<CatchClause> cc) {
        Set<Node> blockInstructs = getBlockInstructs(cc.getAstNode());
        Set<Arc> cdArcs = new HashSet<>();
        for (Arc arc : pdg.outgoingEdgesOf(cc))
            if (arc.isControlDependencyArc() && !blockInstructs.contains(pdg.getEdgeTarget(arc).getAstNode()))
                cdArcs.add(arc);
        for (Arc arc : cdArcs) {
            pdg.addCC1Arc(pdg.getEdgeSource(arc), pdg.getEdgeTarget(arc));
            pdg.removeEdge(arc);
        }
    }

    /** Create the {@link ConditionalControlDependencyArc.CC2 CC2}
     * arcs associated to a given {@link CatchClause}. */
    protected void buildCC2(GraphNode<CatchClause> cc) {
        Set<Node> tryBlockInstructs = getTryBlockInstructs(cc.getAstNode());
        for (Node node : tryBlockInstructs)
            for (GraphNode<?> dst : pdg.findAllNodes(n -> ASTUtils.equalsWithRangeInCU(n.getAstNode(), node)))
                if (isExceptionSource(dst) && hasControlDependencePath(dst, cc, tryBlockInstructs))
                    pdg.addCC2Arc(cc, dst);
    }

    /** Obtains the set of AST nodes found within a given {@link CatchClause}. */
    protected static Set<Node> getBlockInstructs(CatchClause cc) {
        return childNodesOf(cc);
    }

    /** Obtains the set of AST nodes found within the {@link com.github.javaparser.ast.stmt.TryStmt try}
     * associated with the given {@link CatchClause}. */
    protected static Set<Node> getTryBlockInstructs(CatchClause cc) {
        Optional<Node> parent = cc.getParentNode();
        assert parent.isPresent();
        return childNodesOf(parent.get());
    }

    /** Checks whether the argument is or contains an exception source. */
    protected static boolean isExceptionSource(GraphNode<?> node) {
        if (node instanceof ReturnNode) {
            if (node instanceof ExceptionReturnNode)
                return true;
            if (node instanceof NormalReturnNode)
                return false;
        }
        return !new ExceptionSourceSearcher().search(node).isEmpty();
    }

    /**
     * Checks whether there is a control dependence path from {@code a} to {@code b},
     * where all nodes in the path are in the given {@code universe}. The path is found
     * following the rules of the PPDG traversal.
     * @see PseudoPredicateSlicingAlgorithm
     */
    protected boolean hasControlDependencePath(GraphNode<?> a, GraphNode<?> b, Set<Node> universe) {
        Set<GraphNode<?>> visited = new HashSet<>();
        Set<GraphNode<?>> pending = new HashSet<>();
        pending.add(b);
        boolean first = true;

        // First step: consider any path
        // Rest of steps: do not keep traversing backwards from a pseudo-predicate
        while (!pending.isEmpty()) {
            GraphNode<?> node = Utils.setPop(pending);
            if (node.equals(a))
                return true;
            // PPDG rule: Skip if already visited or if it is a pseudo-predicate; do not check first time (criterion)
            if (!first && (cfg.isPseudoPredicate(node) || visited.contains(node)))
                continue;
            pdg.incomingEdgesOf(node).stream()
                    .filter(Arc::isControlDependencyArc)
                    .map(pdg::getEdgeSource)
                    .filter(gn -> universe.contains(gn.getAstNode()))
                    .forEach(pending::add);
            visited.add(node);
            first = false;
        }
        return false;
    }

    /** Internal method to find all possible AST nodes that descend from the given argument. */
    protected static Set<Node> childNodesOf(Node parent) {
        Set<Node> result = new HashSet<>();
        Set<Node> pending = new HashSet<>();
        pending.add(parent);

        while (!pending.isEmpty()) {
            Set<Node> newPending = new HashSet<>();
            for (Node n : pending) {
                newPending.addAll(n.getChildNodes());
                result.add(n);
            }
            pending.clear();
            pending.addAll(newPending);
        }

        // Some elements are never going to match nodes: remove Expression except MethodCallExpr
        result.removeIf(n -> n instanceof Expression && !(n instanceof MethodCallExpr) && !(n instanceof ObjectCreationExpr));
        return result;
    }
}
