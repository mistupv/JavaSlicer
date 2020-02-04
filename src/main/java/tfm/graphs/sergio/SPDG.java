package tfm.graphs.sergio;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import tfm.arcs.Arc;
import tfm.graphs.augmented.ACFG;
import tfm.graphs.augmented.APDG;
import tfm.graphs.cfg.CFG;
import tfm.nodes.GraphNode;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class SPDG extends APDG {
    public SPDG() {
        super();
    }

    public SPDG(ACFG acfg) {
        super(acfg);
    }

    @Override
    public void build(MethodDeclaration method) {
        super.build(method);
        Set<Arc> toBeCleared = new HashSet<>();
        for (GraphNode<?> node : vertexSet()) {
            // Is an unconditional jump
            if (isUnconditionalJump(node)) {
                // Has an unconditional jump as parent (control dependence)
                for (Arc arc : incomingEdgesOf(node)) {
                    GraphNode<?> src = getEdgeSource(arc);
                    // Both node and src lead to the same spot
                    if (isUnconditionalJump(src) && Objects.equals(dest(node, getCfg()), dest(src, getCfg()))) {
                        // All conditions are ok! Remove all incoming edges of node.
                        toBeCleared.addAll(incomingEdgesOf(node));
                    }
                }
            }
        }
        for (Arc arc : toBeCleared)
            removeEdge(arc);
    }

    /**
     * Obtains the destination of a jump statement.
     * @param jump The jump instruction.
     * @param cfg The CFG in which the jump instruction is located.
     * @return The graphnode that corresponds to the jump's destination.
     */
    private static GraphNode<?> dest(GraphNode<?> jump, CFG cfg) {
        if (cfg.outDegreeOf(jump) != 2) {
            throw new IllegalStateException("This unconditional jump should have 2 outgoing edges!");
        }
        for (Arc arc : cfg.outgoingEdgesOf(jump))
            if (arc.isExecutableControlFlowArc())
                return cfg.getEdgeTarget(arc);
        throw new IllegalStateException("Could not found an executable outgoing edge of the jump in the CFG");
    }

    private static boolean isUnconditionalJump(GraphNode<?> node) {
        return node.getAstNode() instanceof BreakStmt ||
                node.getAstNode() instanceof ContinueStmt ||
                (node.getAstNode() instanceof ReturnStmt && !((ReturnStmt) node.getAstNode()).getExpression().isPresent());
    }
}
