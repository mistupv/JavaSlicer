package tfm.graphs.cfg;

import com.github.javaparser.ast.body.MethodDeclaration;
import tfm.arcs.Arc;
import tfm.arcs.cfg.ControlFlowArc;
import tfm.graphs.GraphWithRootNode;
import tfm.nodes.GraphNode;
import tfm.nodes.VariableAction;
import tfm.nodes.type.NodeType;
import tfm.utils.NodeNotFoundException;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The <b>Control Flow Graph</b> represents the statements of a method in
 * a graph, displaying the connections between each statement and the ones that
 * may follow it. You can build one manually or use the {@link CFGBuilder CFGBuilder}.
 * The variations of the CFG are implemented as child classes.
 * @see ControlFlowArc
 */
public class CFG extends GraphWithRootNode<MethodDeclaration> {
    public CFG() {
        super();
    }

    public void addControlFlowEdge(GraphNode<?> from, GraphNode<?> to) {
        addControlFlowEdge(from, to, new ControlFlowArc());
    }

    protected void addControlFlowEdge(GraphNode<?> from, GraphNode<?> to, ControlFlowArc arc) {
        super.addEdge(from, to, arc);
    }

    public List<VariableAction.Definition> findLastDefinitionsFrom(GraphNode<?> startNode, VariableAction.Usage variable) {
        if (!this.containsVertex(startNode))
            throw new NodeNotFoundException(startNode, this);
        return findLastVarActionsFrom(startNode, variable, VariableAction::isDefinition);
    }

    public List<VariableAction.Declaration> findLastDeclarationsFrom(GraphNode<?> startNode, VariableAction.Definition variable) {
        if (!this.containsVertex(startNode))
            throw new NodeNotFoundException(startNode, this);
        return findLastVarActionsFrom(startNode, variable, VariableAction::isDeclaration);
    }

    protected <E extends VariableAction> List<E> findLastVarActionsFrom(GraphNode<?> startNode, VariableAction variable, Predicate<VariableAction> actionFilter) {
        return findLastVarActionsFrom(new HashSet<>(), new LinkedList<>(), startNode, startNode, variable, actionFilter);
    }

    @SuppressWarnings("unchecked")
    protected <E extends VariableAction> List<E> findLastVarActionsFrom(Set<GraphNode<?>> visited, List<E> result,
                                                       GraphNode<?> start, GraphNode<?> currentNode, VariableAction var,
                                                       Predicate<VariableAction> filter) {
        // Base case
        if (visited.contains(currentNode))
            return result;
        visited.add(currentNode);

        Stream<VariableAction> stream = currentNode.getVariableActions().stream();
        if (start.equals(currentNode))
            stream = stream.takeWhile(Predicate.not(var::equals));
        List<VariableAction> list = stream.filter(var::matches).filter(filter).collect(Collectors.toList());
        if (!list.isEmpty()) {
            for (int i = list.size() - 1; i >= 0; i--) {
                result.add((E) list.get(i));
                if (!list.get(i).isOptional())
                    break;
            }
            if (!list.get(0).isOptional())
                return result;
        }

        // Not found: traverse backwards!
        for (Arc arc : incomingEdgesOf(currentNode))
            if (arc.isExecutableControlFlowArc())
                findLastVarActionsFrom(visited, result, start, getEdgeSource(arc), var, filter);
        return result;
    }

    @Override
    public boolean removeVertex(GraphNode<?> graphNode) {
        // Cannot remove exit node
        // Enter node's removal is checked in super#removeVertex(GraphNode)
        if (graphNode.getNodeType() == NodeType.METHOD_EXIT)
            return false;
        return super.removeVertex(graphNode);
    }

    @Override
    public void build(MethodDeclaration method) {
        method.accept(newCFGBuilder(), null);
        if (vertexSet().stream().noneMatch(n -> n.getNodeType() == NodeType.METHOD_EXIT))
            throw new IllegalStateException("There is no exit node after building the graph");
        built = true;
    }

    protected CFGBuilder newCFGBuilder() {
        return new CFGBuilder(this);
    }
}
