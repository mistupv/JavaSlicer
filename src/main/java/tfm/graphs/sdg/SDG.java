package tfm.graphs.sdg;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
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
import tfm.graphs.cfg.CFG;
import tfm.graphs.pdg.PDG;
import tfm.nodes.*;
import tfm.slicing.Slice;
import tfm.slicing.Sliceable;
import tfm.slicing.SlicingCriterion;
import tfm.utils.Context;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SDG extends Graph implements Sliceable, Buildable<NodeList<CompilationUnit>> {
    private boolean built = false;

    private Map<MethodDeclaration, CFG> methodCFGMap;

    public SDG() {
        this.methodCFGMap = new HashMap<>();
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

    public Set<MethodDeclaration> getMethodDeclarations() {
        return this.methodCFGMap.keySet();
    }

    public void setMethodCFG(MethodDeclaration methodDeclaration, CFG cfg) {
        this.methodCFGMap.put(methodDeclaration, cfg);
    }

    public Optional<CFG> getMethodCFG(MethodDeclaration methodDeclaration) {
        if (!this.methodCFGMap.containsKey(methodDeclaration)) {
            return Optional.empty();
        }

        return Optional.of(this.methodCFGMap.get(methodDeclaration));
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
