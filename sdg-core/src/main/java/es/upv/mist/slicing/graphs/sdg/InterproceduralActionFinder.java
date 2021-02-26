package es.upv.mist.slicing.graphs.sdg;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedParameterDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import es.upv.mist.slicing.graphs.BackwardDataFlowAnalysis;
import es.upv.mist.slicing.graphs.CallGraph;
import es.upv.mist.slicing.graphs.cfg.CFG;
import es.upv.mist.slicing.nodes.VariableAction;
import es.upv.mist.slicing.utils.ASTUtils;
import es.upv.mist.slicing.utils.Logger;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO: this approach of generating actual nodes may skip an argument; this is only a problem if there is a definition
// TODO: update placement of actual and formal outputs for ESSDG (see if the definition/usage reaches all/any exits).
/**
 * A backward data flow analysis on the call graph and a map of CFGs, to find which callable
 * declarations define, use or declare which variables, interprocedurally.
 * @param <A> The action to be searched for
 */
public abstract class InterproceduralActionFinder<A extends VariableAction> extends BackwardDataFlowAnalysis<CallGraph.Vertex, CallGraph.Edge<?>, Set<InterproceduralActionFinder.StoredAction<A>>> {
    protected final Map<CallableDeclaration<?>, CFG> cfgMap;

    public InterproceduralActionFinder(CallGraph callGraph, Map<CallableDeclaration<?>, CFG> cfgMap) {
        super(callGraph);
        this.cfgMap = cfgMap;
    }

    // ===========================================================
    // ===================== SAVE DATA ===========================
    // ===========================================================

    /** Entry-point to the class. Performs the analysis and then saves the results to the CFG nodes. */
    public void save() {
        if (!built) analyze();
        graph.vertexSet().forEach(this::saveDeclaration);
    }

    /** Save the current set of actions associated to the given declaration. It will avoid saving
     *  duplicates by default, so this method may be called multiple times safely. */
    protected void saveDeclaration(CallGraph.Vertex vertex) {
        Set<StoredAction<A>> storedActions = vertexDataMap.get(vertex);

        // FORMAL: per declaration (1)
        for (StoredAction<A> sa : storedActions)
            sa.storeFormal(a -> sandBoxedHandler(vertex.getDeclaration(), a, this::handleFormalAction));
        // ACTUAL: per call (n)
        for (CallGraph.Edge<?> edge : graph.incomingEdgesOf(vertex))
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

    // ===========================================================
    // ============== AUXILIARY METHODS FOR CHILDREN =============
    // ===========================================================

    /** Given a call, obtains the scope. If none is present it may return null.
     *  ExpressionConstructorInvocations result in a this expression, as they
     *  may be seen as dynamic method calls that can modify 'this'. */
    protected static Expression obtainScope(Resolvable<? extends ResolvedMethodLikeDeclaration> call) {
        if (call instanceof MethodCallExpr) {
            var methodCall = (MethodCallExpr) call;
            return methodCall.getScope().orElse(null);
        } else if (call instanceof ExplicitConstructorInvocationStmt) {
            return new ThisExpr();
        } else {
            throw new IllegalArgumentException("The given call is not of a valid type");
        }
    }

    /** Obtains the expression passed as argument for the given action at the given call. If {@code input}
     * is false, primitive parameters will be skipped, as their value cannot be redefined.*/
    protected Expression extractArgument(ResolvedParameterDeclaration p, CallGraph.Edge<?> edge, boolean input) {
        CallableDeclaration<?> callTarget = graph.getEdgeTarget(edge).getDeclaration();
        if (!input && p.getType().isPrimitive())
            return null; // primitives do not have actual-out!
        int paramIndex = ASTUtils.getMatchingParameterIndex(callTarget, p);
        return ASTUtils.getResolvableArgs(edge.getCall()).get(paramIndex);
    }

    /** Generate the name that should be given to an object in a caller method, given an action
     *  in the callee method. This is used to transform a reference to 'this' into the scope
     *  of a method. */
    protected static String obtainAliasedFieldName(VariableAction action, CallGraph.Edge<?> edge) {
        if (edge.getCall() instanceof MethodCallExpr) {
            Optional<Expression> optScope = ((MethodCallExpr) edge.getCall()).getScope();
            return obtainAliasedFieldName(action, edge, optScope.isPresent() ? optScope.get().toString() : "");
        } else if (edge.getCall() instanceof ExplicitConstructorInvocationStmt) {
            // The only possibility is 'this' or its fields, so we return empty scope and 'type.this.' is generated
            return obtainAliasedFieldName(action, edge, "");
        } else {
            throw new IllegalArgumentException("The given call is not of a valid type");
        }
    }

    /** To be used by {@link #obtainAliasedFieldName(VariableAction, CallGraph.Edge)} exclusively. <br/>
     *  Given a scope, name inside a method and call, translates the name of a variable, such that 'this' becomes
     *  the scope of the method. */
    protected static String obtainAliasedFieldName(VariableAction action, CallGraph.Edge<?> edge, String scope) {
        if (scope.isEmpty()) {
            return action.getVariable();
        } else {
            String newPrefix = scope;
            newPrefix = newPrefix.replaceAll("((\\.)super|^super)(\\.)?", "$2this$3");
            if (newPrefix.equals("this")) {
                String fqName = ASTUtils.getClassNode(edge.getGraphNode().getAstNode()).getFullyQualifiedName().orElseThrow();
                newPrefix = fqName + ".this";
            }
            String withPrefix = action.getVariable();
            String withoutPrefix = withPrefix.replaceFirst("^((.*\\.)?this\\.?)", "");
            String result = newPrefix + withoutPrefix;
            return result.replaceFirst("this(\\.this)+", "this");
        }
    }

    // ===========================================================
    // =============== COMPUTE DATA FOR FIXED POINT ==============
    // ===========================================================

    @Override
    protected Set<StoredAction<A>> compute(CallGraph.Vertex vertex, Set<CallGraph.Vertex> predecessors) {
        saveDeclaration(vertex);
        Set<StoredAction<A>> newValue = new HashSet<>(vertexDataMap.get(vertex));
        newValue.addAll(initialValue(vertex));
        return newValue;
    }

    @Override
    protected Set<StoredAction<A>> initialValue(CallGraph.Vertex vertex) {
        CFG cfg = cfgMap.get(vertex.getDeclaration());
        if (cfg == null)
            return Collections.emptySet();
        Stream<VariableAction> stream =  cfg.vertexSet().stream()
                // Ignore root node, it is literally the entrypoint for interprocedural actions.
                .filter(n -> n != cfg.getRootNode())
                .flatMap(n -> n.getVariableActions().stream())
                // We never analyze synthetic variables (all intraprocedural)
                .filter(Predicate.not(VariableAction::isSynthetic))
                // We skip over non-root variables (for each 'x.a' action we'll find 'x' later)
                .filter(VariableAction::isRootAction);
        return mapAndFilterActionStream(stream, cfg)
                .map(StoredAction::new)
                .collect(Collectors.toSet());
    }

    /** Given a stream of VariableAction objects, map it to the finders' type and
     *  filter unwanted items (only if the filter is specific to that type). */
    protected abstract Stream<A> mapAndFilterActionStream(Stream<VariableAction> stream, CFG cfg);

    // ===========================================================
    // ========================= SUBCLASSES ======================
    // ===========================================================

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
                r1 = o1.getAction().getResolvedValueDeclaration();
                r2 = o2.getAction().getResolvedValueDeclaration();
                if (r1.isParameter() && r2.isParameter())
                    return -Integer.compare(ASTUtils.getMatchingParameterIndex(graph.getEdgeTarget(edge).getDeclaration(), r1.asParameter()),
                            ASTUtils.getMatchingParameterIndex(graph.getEdgeTarget(edge).getDeclaration(), r2.asParameter()));
                else if (r1.isField() && r2.isField())
                    return 0;
                else if (r1.isParameter() && r2.isField())
                    return -1;
                else if (r1.isField() && r2.isParameter())
                    return 1;
            } catch (UnsolvedSymbolException e) {
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
