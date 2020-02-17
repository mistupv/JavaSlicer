package tfm.graphs.sdg;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import tfm.arcs.pdg.ControlDependencyArc;
import tfm.arcs.pdg.DataDependencyArc;
import tfm.arcs.sdg.CallArc;
import tfm.arcs.sdg.ParameterInOutArc;
import tfm.graphs.Buildable;
import tfm.graphs.Graph;
import tfm.nodes.*;
import tfm.slicing.Slice;
import tfm.slicing.Sliceable;
import tfm.slicing.SlicingCriterion;
import tfm.utils.Context;

import java.util.*;

public class SDG extends Graph implements Sliceable, Buildable<NodeList<CompilationUnit>> {
    private boolean built = false;

    private Map<Context, Long> contextToRootNodeId;

    public SDG() {
        this.contextToRootNodeId = new HashMap<>();
    }

    @Override
    public Slice slice(SlicingCriterion slicingCriterion) {
        throw new RuntimeException("Slicing not implemented for the SDG");
    }

    @Override
    public void build(NodeList<CompilationUnit> nodeList) {
        nodeList.accept(new SDGBuilder(this), new Context());
    }

    @Override
    public boolean isBuilt() {
        return built;
    }

    public Set<Context> getContexts() {
        return contextToRootNodeId.keySet();
    }

    public Optional<GraphNode<?>> getRootNode(Context context) {
        Long id = this.contextToRootNodeId.get(context);

        return id != null ? findNodeById(id) : Optional.empty();
    }

    public void addRootNode(Context context, long id) {
        if (!findNodeById(id).isPresent()) {
            throw new IllegalArgumentException("Cannot add root node to SDG: " + id + " is not in graph!");
        }

        this.contextToRootNodeId.put(new Context(context), id);
    }

    public void addControlDependencyArc(GraphNode<?> from, GraphNode<?> to) {
        this.addEdge(from, to, new ControlDependencyArc());
    }

    public void addDataDependencyArc(GraphNode<?> from, GraphNode<?> to, String variable) {
        this.addEdge(from, to, new DataDependencyArc(variable));
    }

    public void addCallArc(GraphNode<ExpressionStmt> from, GraphNode<MethodDeclaration> to) {
        this.addEdge(from, to, new CallArc());
    }

    public void addParameterInOutArc(GraphNode<ExpressionStmt> from, GraphNode<ExpressionStmt> to) {
        this.addEdge(from, to, new ParameterInOutArc());
    }
}
