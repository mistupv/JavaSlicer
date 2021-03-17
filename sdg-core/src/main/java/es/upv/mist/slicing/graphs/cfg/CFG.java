package es.upv.mist.slicing.graphs.cfg;

import com.github.javaparser.ast.body.CallableDeclaration;
import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.arcs.cfg.ControlFlowArc;
import es.upv.mist.slicing.graphs.GraphWithRootNode;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.VariableAction;
import es.upv.mist.slicing.nodes.io.MethodExitNode;
import es.upv.mist.slicing.utils.NodeNotFoundException;

import java.util.*;
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
public class CFG extends GraphWithRootNode<CallableDeclaration<?>> {
    protected GraphNode<?> exitNode;

    /** Obtains the declaration on which this CFG is based. */
    public CallableDeclaration<?> getDeclaration() {
        assert rootNode != null;
        return rootNode.getAstNode();
    }

    public GraphNode<?> getExitNode() {
        if (exitNode == null)
            throw new IllegalStateException("There is no exit node!");
        return exitNode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), exitNode);
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && obj instanceof CFG && Objects.equals(exitNode, ((CFG) obj).exitNode);
    }

    public void addControlFlowArc(GraphNode<?> from, GraphNode<?> to) {
        addControlFlowArc(from, to, new ControlFlowArc());
    }

    protected void addControlFlowArc(GraphNode<?> from, GraphNode<?> to, ControlFlowArc arc) {
        addEdge(from, to, arc);
    }

    /** Whether the given node is a predicate or not. A node is a predicate if it has more than one outgoing edge. */
    public boolean isPredicate(GraphNode<?> graphNode) {
        return outgoingEdgesOf(graphNode).size() > 1;
    }

    /** Obtain the definitions that may have reached the given variable action. */
    public List<VariableAction> findLastDefinitionsFrom(VariableAction variable) {
        return findLastVarActionsFrom(variable, VariableAction::isDefinition);
    }

    /** Obtain the declaration of a given variable action, if any. */
    public Optional<VariableAction> findDeclarationFor(VariableAction variable) {
        List<VariableAction> declarations = findLastVarActionsFrom(variable, VariableAction::isDeclaration);
        assert declarations.size() <= 1;
        return Optional.ofNullable(declarations.isEmpty() ? null : declarations.get(0));
    }

    /** Check whether or not there is a definition in all paths from the argument to the start of the graph. */
    public boolean isCompletelyDefined(VariableAction.Usage usage) {
        return findLastVarActionsFrom(new HashSet<>(), new LinkedList<>(), usage.getGraphNode(), usage, VariableAction::isDefinition);
    }

    /** Obtain a list of actions that can reach the variable, and match the variable and filter. */
    protected List<VariableAction> findLastVarActionsFrom(VariableAction variable, Predicate<VariableAction> actionFilter) {
        if (!this.containsVertex(variable.getGraphNode()))
            throw new NodeNotFoundException(variable.getGraphNode(), this);
        List<VariableAction> list = new LinkedList<>();
        findLastVarActionsFrom(new HashSet<>(), list, variable.getGraphNode(), variable, actionFilter);
        return list;
    }

    protected boolean findLastVarActionsFrom(Set<GraphNode<?>> visited, List<VariableAction> result,
                                             GraphNode<?> currentNode, VariableAction var,
                                             Predicate<VariableAction> filter) {
        // Base case
        if (visited.contains(currentNode))
            return true;
        visited.add(currentNode);

        Stream<VariableAction> stream = currentNode.getVariableActions().stream();
        if (var.getGraphNode().equals(currentNode))
            stream = stream.takeWhile(va -> va != var);
        List<VariableAction> list = stream.filter(var::matches).filter(filter).collect(Collectors.toList());
        if (!list.isEmpty()) {
            boolean found = false;
            for (int i = list.size() - 1; i >= 0 && !found; i--) {
                result.add(list.get(i));
                if (!list.get(i).isOptional())
                    found = true;
            }
            if (found)
                return true;
        }

        // Not found: traverse backwards!
        boolean allBranches = !incomingEdgesOf(currentNode).isEmpty();
        for (Arc arc : incomingEdgesOf(currentNode))
            if (arc.isExecutableControlFlowArc())
                allBranches &= findLastVarActionsFrom(visited, result, getEdgeSource(arc), var, filter);
        return allBranches;
    }

    /** Create and set the root node of this CFG, given a callable declaration. */
    public void buildRootNode(CallableDeclaration<?> rootNodeAst) {
        super.buildRootNode("ENTER " + rootNodeAst.getDeclarationAsString(false, false, false), rootNodeAst);
    }

    @Override
    public boolean removeVertex(GraphNode<?> graphNode) {
        // Cannot remove exit node
        // Enter node's removal is checked in super#removeVertex(GraphNode)
        if (graphNode == exitNode)
            return false;
        return super.removeVertex(graphNode);
    }

    @Override
    public void build(CallableDeclaration<?> declaration) {
        declaration.accept(newCFGBuilder(), null);
        exitNode = vertexSet().stream().filter(MethodExitNode.class::isInstance).findFirst()
                .orElseThrow(() -> new IllegalStateException("Built graph has no exit node!"));
        built = true;
    }

    /** Create a new CFGBuilder. Child classes that wish to alter the creation of the graph
     * should create a new CFGBuilder and override this method. */
    protected CFGBuilder newCFGBuilder() {
        return new CFGBuilder(this);
    }
}
