package tfm.graphs.sdg;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import tfm.arcs.pdg.ControlDependencyArc;
import tfm.arcs.pdg.DataDependencyArc;
import tfm.arcs.sdg.CallArc;
import tfm.arcs.sdg.ParameterInOutArc;
import tfm.arcs.sdg.SummaryArc;
import tfm.graphs.Buildable;
import tfm.graphs.Graph;
import tfm.graphs.cfg.CFG;
import tfm.graphs.sdg.sumarcs.NaiveSummaryArcsBuilder;
import tfm.nodes.GraphNode;
import tfm.nodes.VariableAction;
import tfm.slicing.ClassicSlicingAlgorithm;
import tfm.slicing.Slice;
import tfm.slicing.Sliceable;
import tfm.slicing.SlicingCriterion;
import tfm.utils.Context;

import java.util.*;
import java.util.stream.Collectors;

public class SDG extends Graph implements Sliceable, Buildable<NodeList<CompilationUnit>> {
    protected final List<CFG> cfgs = new LinkedList<>();

    protected boolean built = false;
    protected NodeList<CompilationUnit> compilationUnits;

    public NodeList<CompilationUnit> getCompilationUnits() {
        return compilationUnits;
    }

    @Override
    public Slice slice(SlicingCriterion slicingCriterion) {
        Optional<GraphNode<?>> optSlicingNode = slicingCriterion.findNode(this);
        if (optSlicingNode.isEmpty())
            throw new IllegalArgumentException("Could not locate the slicing criterion in the SDG");
        return new ClassicSlicingAlgorithm(this).traverse(optSlicingNode.get());
    }

    @Override
    public void build(NodeList<CompilationUnit> nodeList) {
        nodeList.accept(createBuilder(), new Context());
        Set<GraphNode<?>> vertices = Set.copyOf(vertexSet());
        vertices.forEach(n -> new MethodCallReplacerVisitor(this).startVisit(n));
        new NaiveSummaryArcsBuilder(this).visit();
        compilationUnits = nodeList;
        built = true;
    }

    protected SDGBuilder createBuilder() {
        return new SDGBuilder(this);
    }

    @Override
    public boolean isBuilt() {
        return built;
    }

    public void setMethodCFG(CFG cfg) {
        this.cfgs.add(cfg);
    }

    public Collection<CFG> getCFGs() {
        return cfgs;
    }

    public void addControlDependencyArc(GraphNode<?> from, GraphNode<?> to) {
        this.addEdge(from, to, new ControlDependencyArc());
    }

    public void addDataDependencyArc(VariableAction src, VariableAction tgt) {
        DataDependencyArc arc;
        if (src instanceof VariableAction.Definition && tgt instanceof VariableAction.Usage)
            arc = new DataDependencyArc((VariableAction.Definition) src, (VariableAction.Usage) tgt);
        else if (src instanceof VariableAction.Declaration && tgt instanceof VariableAction.Definition)
            arc = new DataDependencyArc((VariableAction.Declaration) src, (VariableAction.Definition) tgt);
        else
            throw new UnsupportedOperationException("Unsupported combination of VariableActions");
        addEdge(src.getGraphNode(), tgt.getGraphNode(), arc);
    }

    public void addCallArc(GraphNode<?> from, GraphNode<MethodDeclaration> to) {
        this.addEdge(from, to, new CallArc());
    }

    public void addParameterInOutArc(GraphNode<?> from, GraphNode<?> to) {
        this.addEdge(from, to, new ParameterInOutArc());
    }

    public void addSummaryArc(GraphNode<ExpressionStmt> from, GraphNode<ExpressionStmt> to) {
        this.addEdge(from, to, new SummaryArc());
    }

    public List<GraphNode<?>> findDeclarationsOfVariable(String variable, GraphNode<?> root) {
        return this.cfgs.stream()
                .filter(cfg -> cfg.containsVertex(root))
                .findFirst()
                .map(cfg -> cfg.findLastDeclarationsFrom(root, new VariableAction.Definition(new NameExpr(variable), root)))
                .orElseThrow()
                .stream()
                .map(VariableAction::getGraphNode)
                .collect(Collectors.toList());
    }
}
