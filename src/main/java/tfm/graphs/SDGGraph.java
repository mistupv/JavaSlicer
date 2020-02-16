package tfm.graphs;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
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
import java.util.stream.Collectors;

public class SDG extends Graph implements Sliceable, Buildable<NodeList<CompilationUnit>> {
    private boolean built = false;

    private Map<Context, Integer> contextToRootNodeId;

    public SDG() {
        this.contextToRootNodeId = new HashMap<>();
    }

    @Override
    public <ASTNode extends Node> GraphNode<ASTNode> addNode(String instruction, ASTNode node) {
        GraphNode<ASTNode> sdgNode = new GraphNode<>(getNextVertexId(), instruction, node);
        super.addVertex(sdgNode);

        return sdgNode;
    }

    @Override
    public void build(NodeList<CompilationUnit> nodeList) {
        nodeList.accept(new SDGBuilder(this), new Context());
    }

    @Override
    public Graph slice(SlicingCriterion slicingCriterion) {
        return this;
    }

    public Set<Context> getContexts() {
        return contextToRootNodeId.keySet();
    }

    public Optional<GraphNode<?>> getRootNode(Context context) {
        Integer id = this.contextToRootNodeId.get(context);

        return id != null ? findNodeById(id) : Optional.empty();
    }

    public void addControlDependencyArc(GraphNode<?> from, GraphNode<?> to) {
        this.addEdge(from, to, new ControlDependencyArc());
    }

    public void addDataDependencyArc(GraphNode<?> from, GraphNode<?> to, String variable) {
        this.addEdge(from, to, new DataDependencyArc(variable));
    }

    public void addCallArc(MethodCallNode from, MethodRootNode to) {
        this.addEdge(from, to, new CallArc());
    }

    public void addParameterInOutArc(InOutVariableNode from, InOutVariableNode to) {
        this.addEdge(from, to, new ParameterInOutArc());
    }
}
