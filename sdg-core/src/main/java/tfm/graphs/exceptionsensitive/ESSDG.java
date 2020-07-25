package tfm.graphs.exceptionsensitive;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import tfm.arcs.sdg.ReturnArc;
import tfm.graphs.augmented.ACFG;
import tfm.graphs.cfg.CFG;
import tfm.graphs.pdg.PDG;
import tfm.graphs.sdg.SDG;
import tfm.graphs.sdg.SDGBuilder;
import tfm.graphs.sdg.sumarcs.NaiveSummaryArcsBuilder;
import tfm.nodes.ExitNode;
import tfm.nodes.GraphNode;
import tfm.nodes.ReturnNode;
import tfm.nodes.SyntheticNode;
import tfm.nodes.type.NodeType;
import tfm.slicing.ExceptionSensitiveSlicingAlgorithm;
import tfm.slicing.Slice;
import tfm.slicing.SlicingCriterion;
import tfm.utils.Context;

import java.util.Optional;
import java.util.Set;

public class ESSDG extends SDG {
    protected static final Set<NodeType> NOT_PP_TYPES = Set.of(NodeType.METHOD_CALL, NodeType.METHOD_OUTPUT, NodeType.METHOD_CALL_RETURN);

    @Override
    protected SDGBuilder createBuilder() {
        return new Builder();
    }

    @Override
    public Slice slice(SlicingCriterion slicingCriterion) {
        Optional<GraphNode<?>> optSlicingNode = slicingCriterion.findNode(this);
        if (optSlicingNode.isEmpty())
            throw new IllegalArgumentException("Could not locate the slicing criterion in the SDG");
        return new ExceptionSensitiveSlicingAlgorithm(ESSDG.this).traverse(optSlicingNode.get());
    }

    @Override
    public void build(NodeList<CompilationUnit> nodeList) {
        nodeList.accept(createBuilder(), new Context());
        Set<GraphNode<?>> vertices = Set.copyOf(vertexSet());
        vertices.forEach(n -> new ExceptionSensitiveMethodCallReplacerVisitor(this).startVisit(n));
        new NaiveSummaryArcsBuilder(this).visit();
        compilationUnits = nodeList;
        built = true;
    }

    public boolean isPseudoPredicate(GraphNode<?> node) {
        if (NOT_PP_TYPES.contains(node.getNodeType()) || node instanceof SyntheticNode)
            return false;
        for (CFG cfg : cfgs)
            if (cfg.containsVertex(node))
                return ((ACFG) cfg).isPseudoPredicate(node);
        throw new IllegalArgumentException("Node " + node.getId() + "'s associated CFG cannot be found!");
    }

    public void addReturnArc(ExitNode source, ReturnNode target) {
        addEdge(source, target, new ReturnArc());
    }

    class Builder extends SDGBuilder {
        public Builder() {
            super(ESSDG.this);
        }

        @Override
        protected PDG createPDG() {
            return new ESPDG();
        }
    }
}
