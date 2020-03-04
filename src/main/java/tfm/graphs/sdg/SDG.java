package tfm.graphs.sdg;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import tfm.arcs.Arc;
import tfm.arcs.pdg.ControlDependencyArc;
import tfm.arcs.pdg.DataDependencyArc;
import tfm.arcs.sdg.CallArc;
import tfm.arcs.sdg.ParameterInOutArc;
import tfm.graphs.Buildable;
import tfm.graphs.Graph;
import tfm.graphs.pdg.PDG;
import tfm.nodes.*;
import tfm.slicing.Slice;
import tfm.slicing.Sliceable;
import tfm.slicing.SlicingCriterion;
import tfm.utils.Context;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SDG extends Graph implements Sliceable, Buildable<NodeList<CompilationUnit>> {
    private boolean built = false;

    private Map<Context, Long> contextToMethodRoot;

    public SDG() {
        this.contextToMethodRoot = new HashMap<>();
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
        return contextToMethodRoot.keySet();
    }

    @SuppressWarnings("unchecked")
    public List<GraphNode<MethodDeclaration>> getMethodRoots() {
        return contextToMethodRoot.values().stream()
                .map(id -> findNodeById(id))
                .filter(Optional::isPresent)
                .map(optional -> (GraphNode<MethodDeclaration>) optional.get())
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public Optional<GraphNode<MethodDeclaration>> getRootNode(Context context) {
        Long id = this.contextToMethodRoot.get(context);

        if (id == null) {
            return Optional.empty();
        }

        return findNodeById(id).map(node -> (GraphNode<MethodDeclaration>) node);
    }

    public void addRootNode(Context context, long id) {
        if (!findNodeById(id).isPresent())
            throw new IllegalArgumentException("Root node with id " + id + " is not contained in graph!");

        this.contextToMethodRoot.put(new Context(context), id);
    }

    public void addRootNode(Context context, GraphNode<MethodDeclaration> node) {
        addRootNode(context, node.getId());
    }

    public Optional<Context> getContext(long id) {
        return contextToMethodRoot.entrySet().stream()
                .filter(entry -> Objects.equals(entry.getValue(), id))
                .findFirst()
                .map(Map.Entry::getKey);
    }

    public void addControlDependencyArc(GraphNode<?> from, GraphNode<?> to) {
        this.addEdge(from, to, new ControlDependencyArc());
    }

    public void addDataDependencyArc(GraphNode<?> from, GraphNode<?> to, String variable) {
        this.addEdge(from, to, new DataDependencyArc(variable));
    }

    public void addCallArc(GraphNode<?> from, GraphNode<MethodDeclaration> to) {
        this.addEdge(from, to, new CallArc());
    }

    public void addParameterInOutArc(GraphNode<ExpressionStmt> from, GraphNode<ExpressionStmt> to) {
        this.addEdge(from, to, new ParameterInOutArc());
    }

    public List<GraphNode<?>> findDeclarationsOfVariable(String variable, GraphNode<?> root) {
        List<GraphNode<?>> res = new ArrayList<>();

        // First, expand the node
        for (Arc arc : incomingEdgesOf(root)) {
            if (arc.isDataDependencyArc() || arc.isControlDependencyArc()) {
                res.addAll(findDeclarationsOfVariable(variable, getEdgeSource(arc)));
            }
        }

        // Finally, the current node
        // This way, the last element of the list is the most recent declaration
        if (root.getDeclaredVariables().contains(variable)) {
            res.add(root);
        }

        return res;
    }
}
