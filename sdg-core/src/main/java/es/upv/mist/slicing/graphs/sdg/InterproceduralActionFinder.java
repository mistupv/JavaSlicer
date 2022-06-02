package es.upv.mist.slicing.graphs.sdg;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import es.upv.mist.slicing.graphs.BackwardDataFlowAnalysis;
import es.upv.mist.slicing.graphs.CallGraph;
import es.upv.mist.slicing.graphs.cfg.CFG;
import es.upv.mist.slicing.nodes.VariableAction;
import es.upv.mist.slicing.utils.ASTUtils;
import es.upv.mist.slicing.utils.Logger;
import es.upv.mist.slicing.utils.Utils;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A backward data flow analysis on the call graph and a map of CFGs, to find which callable
 * declarations define, use or declare which variables, interprocedurally.
 * @param <A> The action to be searched for
 */
public abstract class InterproceduralActionFinder<A extends VariableAction> extends BackwardDataFlowAnalysis<CallGraph.Vertex, CallGraph.Edge<?>, Set<A>> {
    protected final Map<CallableDeclaration<?>, CFG> cfgMap;
    /** A map from vertex and action to its corresponding stored action, to avoid generating duplicate nodes. */
    protected final Map<CallGraph.Vertex, Map<A, StoredAction>> actionStoredMap = new HashMap<>();

    protected InterproceduralActionFinder(CallGraph callGraph, Map<CallableDeclaration<?>, CFG> cfgMap) {
        super(callGraph);
        this.cfgMap = cfgMap;
    }

    // ===========================================================
    // ===================== SAVE DATA ===========================
    // ===========================================================

    /** Entry-point to the class. Performs the analysis and then saves the results to the CFG nodes. */
    public void save() {
        if (!built) analyze();
        graph.vertexSet().forEach(this::saveDeclarationFormalNodes);
    }

    /** Obtains the StoredAction object with information on which actions have been stored. */
    protected StoredAction getStored(CallGraph.Vertex vertex, A action) {
        return actionStoredMap.get(vertex).get(action);
    }

    /** Save the current set of actions associated with the given declaration. This method will
     *  only generate actual-in and actual-out nodes. It is idempotent, and won't generate duplicates. */
    protected void saveDeclarationActualNodes(CallGraph.Vertex vertex) {
        var actions = vertexDataMap.get(vertex);
        // Update stored action map
        actionStoredMap.computeIfAbsent(vertex, v -> new HashMap<>());
        for (A a : actions)
            actionStoredMap.get(vertex).computeIfAbsent(a, __ -> new StoredAction());
        // ACTUAL: per call (n)
        for (CallGraph.Edge<?> edge : graph.incomingEdgesOf(vertex))
            actions.stream().sorted(new ParameterFieldSorter(edge)).forEach(a ->
                    getStored(vertex, a).storeActual(edge, a, e -> sandBoxedHandler(e, a, this::handleActualAction)));
    }

    /** Save the current set of actions associated with the given declaration. This method will
     *  only generate formal-in and formal-out nodes. It is idempotent, and won't generate duplicates. */
    protected void saveDeclarationFormalNodes(CallGraph.Vertex vertex) {
        var actions = vertexDataMap.get(vertex);
        // Update stored action map
        actionStoredMap.computeIfAbsent(vertex, __ -> new HashMap<>());
        for (A a : actions)
            actionStoredMap.get(vertex).computeIfAbsent(a, __ -> new StoredAction());
        // 1 formal per declaration and action
        for (A a : actions)
            getStored(vertex, a).storeFormal(a, () -> sandBoxedHandler(vertex, a, this::handleFormalAction));
    }

    /** A sandbox to avoid resolution errors when a variable is included that is a class name
     *  for static usage of methods and fields. */
    protected final <T> void sandBoxedHandler(T location, A action, BiConsumer<T, A> handler) {
        try {
            handler.accept(location, action);
        } catch (UnsolvedSymbolException e) {
            Logger.log("Skipping a symbol, cannot be resolved: " + action.getName());
        }
    }

    /** Generate the formal node(s) related to this action and declaration. */
    protected abstract void handleFormalAction(CallGraph.Vertex vertex, A action);

    /** Generate the actual node(s) related to this action and call. */
    protected abstract void handleActualAction(CallGraph.Edge<?> edge, A action);

    // ===========================================================
    // ============== AUXILIARY METHODS FOR CHILDREN =============
    // ===========================================================

    /** Obtains the expression passed as argument for the given action at the given call. If {@code input}
     * is false, primitive parameters will be skipped, as their value cannot be redefined.*/
    protected Optional<Expression> extractArgument(VariableAction action, CallGraph.Edge<?> edge, boolean input) {
        CallableDeclaration<?> callTarget = graph.getEdgeTarget(edge).getDeclaration();
        if (!input && action.isPrimitive())
            return Optional.empty(); // primitives do not have actual-out!
        int paramIndex = ASTUtils.getMatchingParameterIndex(callTarget, action.getName());
        return Optional.of(ASTUtils.getResolvableArgs(edge.getCall()).get(paramIndex));
    }

    // ===========================================================
    // =============== COMPUTE DATA FOR FIXED POINT ==============
    // ===========================================================

    @Override
    protected Set<A> compute(CallGraph.Vertex vertex, Set<CallGraph.Vertex> predecessors) {
        saveDeclarationActualNodes(vertex);
        return initialValue(vertex);
    }

    @Override
    protected Set<A> initialValue(CallGraph.Vertex vertex) {
        // Skip abstract vertices

        if (vertex.getDeclaration().isAbstract() ||
                (vertex.getDeclaration().isMethodDeclaration() &&
                        vertex.getDeclaration().asMethodDeclaration().getBody().isEmpty()))
            return new HashSet<>();
        CFG cfg = cfgMap.get(vertex.getDeclaration());
        assert cfg != null;
        Stream<VariableAction> actionStream =  cfg.vertexSet().stream()
                // Ignore root node, it is literally the entrypoint for interprocedural actions.
                .filter(n -> n != cfg.getRootNode())
                .flatMap(n -> n.getVariableActions().stream())
                // We never analyze synthetic variables (all intraprocedural)
                .filter(Predicate.not(VariableAction::isSynthetic))
                // We skip over non-root variables (for each 'x.a' action we'll find 'x' later)
                .filter(VariableAction::isRootAction)
                // We skip local variables, as those can't be interprocedural
                .filter(Predicate.not(VariableAction::isLocalVariable));
        Stream<A> filteredStream = mapAndFilterActionStream(actionStream, cfg);
        Set<A> set = new HashSet<>();
        for (Iterator<A> it = filteredStream.iterator(); it.hasNext(); ) {
            A a = it.next();
            if (set.contains(a)) {
                if (a.hasObjectTree())
                    Utils.setGet(set, a).getObjectTree().addAll(a.getObjectTree());
            } else {
                set.add(a.createCopy());
            }
        }
        return set;
    }

    /** Given a stream of VariableAction objects, map it to the finders' type and
     *  filter unwanted items (only if the filter is specific to that type). */
    protected abstract Stream<A> mapAndFilterActionStream(Stream<VariableAction> stream, CFG cfg);

    @Override
    protected boolean dataMatch(Set<A> oldData, Set<A> newData) {
        if (oldData == newData)
            return true;
        if (oldData.size() != newData.size())
            return false;
        HashMap<String, A> map = new HashMap<>();
        for (A a : oldData)
            map.put(a.getName(), a);
        for (A b : newData)
            if (!VariableAction.objectTreeMatches(map.get(b.getName()), b))
                return false;
        return true;
    }

    // ===========================================================
    // ========================= SUBCLASSES ======================
    // ===========================================================

    /** A comparator to sort parameters and fields in the generation of actual nodes. It will sort
     *  {@link StoredAction}s in the following order: fields, then parameters by descending index number.
     *  The actual nodes will be generated in that order and inserted in reverse order in the graph node. */
    private class ParameterFieldSorter implements Comparator<A> {
        protected final CallGraph.Edge<?> edge;
        public ParameterFieldSorter(CallGraph.Edge<?> edge) {
            this.edge = edge;
        }

        @Override
        public int compare(A o1, A o2) {
            if (o1.isParameter() && o2.isParameter())
                return -Integer.compare(ASTUtils.getMatchingParameterIndex(graph.getEdgeTarget(edge).getDeclaration(), o1.getName()),
                        ASTUtils.getMatchingParameterIndex(graph.getEdgeTarget(edge).getDeclaration(), o2.getName()));
            else if (o1.isField() && o2.isField())
                return 0;
            else if (o1.isParameter() && o2.isField())
                return -1;
            else if (o1.isField() && o2.isParameter())
                return 1;
            throw new IllegalArgumentException("One or more arguments is not a field or parameter");
        }
    }

    /** A wrapper around a variable action, which keeps track of whether formal and actual nodes
     *  have been saved to the graph or not. */
    protected static class StoredAction {
        /** Whether the action has been saved as actual node for each call. */
        private final Map<CallGraph.Edge<?>, VariableAction> actualStoredMap = new HashMap<>();

        /** Whether the action has been saved as formal node. */
        protected VariableAction formalStored = null;

        private StoredAction() {}

        /** If this action has not yet been saved as formal node, use the argument to do so, then mark it as stored. */
        private void storeFormal(VariableAction action, Runnable save) {
            if (formalStored == null || !VariableAction.objectTreeMatches(action, formalStored)) {
                save.run();
                formalStored = action;
            }
        }

        /** If this action has not yet been saved as actual node for the given edge,
         * use the consumer to do so, then mark it as stored. */
        private void storeActual(CallGraph.Edge<?> edge, VariableAction action, Consumer<CallGraph.Edge<?>> save) {
            VariableAction storedAction = actualStoredMap.get(edge);
            if (storedAction == null || !VariableAction.objectTreeMatches(storedAction, action)) {
                save.accept(edge);
                actualStoredMap.put(edge, action);
            }
        }
    }
}
