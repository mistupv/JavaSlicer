package tfm.graphs.sdg;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import tfm.graphs.BackwardDataFlowAnalysis;
import tfm.graphs.CallGraph;
import tfm.graphs.cfg.CFG;
import tfm.nodes.VariableAction;
import tfm.utils.ASTUtils;
import tfm.utils.Logger;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

// TODO: this approach of generating actual nodes may skip an argument; this is only a problem if there is a definition
// TODO: update placement of actual and formal outputs for ESSDG (see if the definition/usage reaches all/any exits).
/**
 * A backward data flow analysis on the call graph and a map of CFGs, to find which callable
 * declarations define, use or declare which variables, interprocedurally.
 * @param <A> The action to be searched for
 */
public abstract class InterproceduralActionFinder<A extends VariableAction> extends BackwardDataFlowAnalysis<CallableDeclaration<?>, CallGraph.Edge<?>, Set<InterproceduralActionFinder.StoredAction<A>>> {
    protected final Map<CallableDeclaration<?>, CFG> cfgMap;

    public InterproceduralActionFinder(CallGraph callGraph, Map<CallableDeclaration<?>, CFG> cfgMap) {
        super(callGraph);
        this.cfgMap = cfgMap;
        this.vertexDataMap = new IdentityHashMap<>(); // CallableDeclarations can't be reliably be compared with equals.
    }

    // ===========================================================
    // ===================== SAVE DATA ===========================
    // ===========================================================

    /** Entry-point to the class. Performs the analysis and then saves the results to the CFG nodes. */
    public void save() {
        if (!built) analyze();
        cfgMap.keySet().forEach(this::saveDeclaration);
    }

    /** Save the current set of actions associated to the given declaration. It will avoid saving
     *  duplicates by default, so this method may be called multiple times safely. */
    protected void saveDeclaration(CallableDeclaration<?> declaration) {
        Set<StoredAction<A>> storedActions = vertexDataMap.get(declaration);

        // FORMAL: per declaration (1)
        for (StoredAction<A> sa : storedActions)
            sa.storeFormal(a -> sandBoxedHandler(declaration, a, this::handleFormalAction));
        // ACTUAL: per call (n)
        for (CallGraph.Edge<?> edge : graph.incomingEdgesOf(declaration))
            storedActions.stream().sorted(new ParameterFieldSorter(edge))
                    .forEach(sa -> sa.storeActual(edge, (e, a) -> sandBoxedHandler(e, a, this::handleActualAction)));
    }

    /** A sandbox to avoid resolution errors when a variable is included that is a class name
     *  for static usage of methods and fields. */
    protected final <T> void sandBoxedHandler(T location, A action, BiConsumer<T, A> handler) {
        try {
            handler.accept(location, action);
        } catch (UnsolvedSymbolException e) {
            Logger.log("Skipping a symbol, cannot be resolved: " + action.getVariable());
        }
    }

    /** Generate the formal node(s) related to this action and declaration. */
    protected abstract void handleFormalAction(CallableDeclaration<?> declaration, A action);

    /** Generate the actual node(s) related to this action and call. */
    protected abstract void handleActualAction(CallGraph.Edge<?> edge, A action);

    /** Obtains the expression passed as argument for the given action at the given call. If {@code input}
     * is false, primitive parameters will be skipped, as their value cannot be redefined.*/
    protected Expression extractArgument(VariableAction action, CallGraph.Edge<?> edge, boolean input) {
        ResolvedValueDeclaration resolved = action.getNameExpr().resolve();
        CallableDeclaration<?> callTarget = graph.getEdgeTarget(edge);
        if (resolved.isParameter()) {
            ResolvedParameterDeclaration p = resolved.asParameter();
            if (!input && p.getType().isPrimitive())
                return null; // primitives do not have actual-out!
            int paramIndex = ASTUtils.getMatchingParameterIndex(callTarget, p);
            return ASTUtils.getResolvableArgs(edge.getCall()).get(paramIndex);
        } else if (resolved.isField()) {
            return action.getNameExpr();
        } else {
            throw new IllegalArgumentException("Variable should be either param or field!");
        }
    }

    @Override
    protected Set<StoredAction<A>> compute(CallableDeclaration<?> declaration, Set<CallableDeclaration<?>> calledDeclarations) {
        saveDeclaration(declaration);
        Set<StoredAction<A>> newValue = new HashSet<>(vertexDataMap.get(declaration));
        newValue.addAll(initialValue(declaration));
        return newValue;
    }

    /** Wrap a variable action in a {@link StoredAction}, to track whether it has been applied to the graph or not. */
    protected StoredAction<A> wrapAction(A action) {
        return new StoredAction<>(action);
    }

    /** A comparator to sort parameters and fields in the generation of actual nodes. It will sort
     *  {@link StoredAction}s in the following order: fields, then parameters by descending index number.
     *  The actual nodes will be generated in that order and inserted in reverse order in the graph node. */
    private class ParameterFieldSorter implements Comparator<StoredAction<A>> {
        protected final CallGraph.Edge<?> edge;
        public ParameterFieldSorter(CallGraph.Edge<?> edge) {
            this.edge = edge;
        }

        @Override
        public int compare(StoredAction<A> o1, StoredAction<A> o2) {
            ResolvedValueDeclaration r1 = null;
            ResolvedValueDeclaration r2 = null;
            try {
                r1 = o1.getAction().getNameExpr().resolve();
                r2 = o2.getAction().getNameExpr().resolve();
                if (r1.isParameter() && r2.isParameter())
                    return -Integer.compare(ASTUtils.getMatchingParameterIndex(graph.getEdgeTarget(edge), r1.asParameter()),
                            ASTUtils.getMatchingParameterIndex(graph.getEdgeTarget(edge), r2.asParameter()));
                else if (r1.isField() && r2.isField())
                    return 0;
                else if (r1.isParameter() && r2.isField())
                    return -1;
                else if (r1.isField() && r2.isParameter())
                    return 1;
            } catch (UnsolvedSymbolException e) {
                Logger.log("Could not resolve a given name expression, it may be a type: " + e.getName());
                if (r1 == null)
                    return 1;
                else if (r2 == null)
                    return -1;
                else
                    return 0;
            }
            throw new IllegalArgumentException("One or more arguments is not a field or parameter");
        }
    }

    /** A wrapper around a variable action, which keeps track of whether formal and actual nodes
     *  have been saved to the graph or not. */
    protected static class StoredAction<A extends VariableAction> {
        protected final A action;
        /** Whether the action has been saved as actual node for each call. */
        private final Map<CallGraph.Edge<?>, Boolean> actualStoredMap = new HashMap<>();

        /** Whether the action has been saved as formal node. */
        protected boolean formalStored = false;

        private StoredAction(A action) {
            this.action = action;
        }

        public A getAction() {
            return action;
        }

        /** If this action has not yet been saved as formal node, use the argument to do so, then mark it as stored. */
        private void storeFormal(Consumer<A> save) {
            if (!formalStored) {
                save.accept(action);
                formalStored = true;
            }
        }

        /** If this action has not yet been saved as actual node for the given edge,
         * use the consumer to do so, then mark it as stored. */
        private void storeActual(CallGraph.Edge<?> edge, BiConsumer<CallGraph.Edge<?>, A> save) {
            if (!actualStoredMap.getOrDefault(edge, false)) {
                save.accept(edge, action);
                actualStoredMap.put(edge, true);
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(action);
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof StoredAction && Objects.equals(action, ((StoredAction<?>) obj).action);
        }
    }
}
