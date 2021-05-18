package es.upv.mist.slicing.graphs.exceptionsensitive;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import es.upv.mist.slicing.arcs.cfg.ControlFlowArc;
import es.upv.mist.slicing.graphs.augmented.ACFG;
import es.upv.mist.slicing.graphs.augmented.ACFGBuilder;
import es.upv.mist.slicing.graphs.augmented.PPControlDependencyBuilder;
import es.upv.mist.slicing.graphs.cfg.CFGBuilder;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.exceptionsensitive.*;
import es.upv.mist.slicing.nodes.io.MethodExitNode;
import es.upv.mist.slicing.utils.Logger;

import java.util.*;

/**
 * An exception-sensitive version of the CFG. It builds upon the ACFG, and adds support
 * for exception-related instructions, such as {@link ThrowStmt throw}, {@link TryStmt try},
 * {@link CatchClause catch} and calls to declarations that may throw exceptions. <br/>
 * Limitations: {@code finally} is not handled, unchecked exceptions may present problems,
 * and multiple calls with exceptions per CFG node are not considered.
 */
public class ESCFG extends ACFG {
    /** The name for the currently active exception variable. */
    public static final String ACTIVE_EXCEPTION_VARIABLE = "-activeException-";

    @Override
    protected CFGBuilder newCFGBuilder() {
        return new Builder(this);
    }

    protected ExceptionExitNode addExceptionExitNode(CallableDeclaration<?> method, ResolvedType type) {
        ExceptionExitNode node = new ExceptionExitNode(method, type);
        addVertex(node);
        return node;
    }

    protected ExceptionReturnNode addExceptionReturnNode(Resolvable<? extends ResolvedMethodLikeDeclaration> call, ResolvedType type) {
        ExceptionReturnNode node = ExceptionReturnNode.create(call, type);
        addVertex(node);
        return node;
    }

    /**
     * An instruction which may the source of an exception.
     * The exception's types are cached in this object.
     */
    protected static class ExceptionSource {
        private final GraphNode<?> source;
        private final Map<ResolvedType, Boolean> exceptions = new HashMap<>();

        protected ExceptionSource(GraphNode<?> source) {
            this.source = Objects.requireNonNull(source);
        }

        public ExceptionSource(GraphNode<?> source, ResolvedType... exceptionTypes) {
            this(source);
            if (exceptionTypes.length == 0)
                throw new IllegalArgumentException("There must be at least one exception type");
            for (ResolvedType t : exceptionTypes)
                exceptions.put(t, true);
        }

        public void deactivateTypes(ResolvedReferenceType type) {
            exceptions.keySet().stream().filter(type::isAssignableBy).forEach(t -> exceptions.put(t, false));
        }

        public boolean isActive() {
            return exceptions.containsValue(true);
        }

        /** Creates a single ExceptionSource from a list of sources.
         * Each type is marked active if it was active in any of the sources. */
        public static ExceptionSource merge(GraphNode<?> summary, Collection<ExceptionSource> sources) {
            ExceptionSource result = new ExceptionSource(summary);
            for (ExceptionSource es : sources)
                for (ResolvedType rt : es.exceptions.keySet())
                    result.exceptions.merge(rt, es.exceptions.get(rt), Boolean::logicalOr);
            return result;
        }
    }

    public class Builder extends ACFGBuilder {
        /** Map of the currently relevant exception sources, mapped by type. */
        protected Map<ResolvedType, List<ExceptionSource>> exceptionSourceMap = new HashMap<>();
        /** Stack the 'try's that surround the element we're visiting now. */
        protected Deque<TryStmt> tryStack = new LinkedList<>();
        /** Stack of statements that surround the element we're visiting now. */
        protected Deque<Statement> stmtStack = new LinkedList<>();
        /** Stack of hanging nodes that need to be connected by
         * non-executable edges at the end of the current try statement. */
        protected Deque<Set<GraphNode<?>>> tryNonExecHangingStack = new LinkedList<>();
        /** Nodes that need to be connected by non-executable edges to the 'Exit' node. */
        protected List<GraphNode<?>> exitNonExecHangingNodes = new LinkedList<>();
        /** Map of return nodes from each method call, mapped by the normal return node of said call. */
        protected Map<NormalReturnNode, Set<ReturnNode>> pendingNormalReturnNodes = new HashMap<>();

        protected Builder(ESCFG escfg) {
            super(ESCFG.this);
            assert escfg == ESCFG.this;
        }

        @Override
        protected void buildExit(CallableDeclaration<?> callableDeclaration) {
            if (!exceptionSourceMap.isEmpty()) {
                // Normal exit
                NormalExitNode normalExit = new NormalExitNode(callableDeclaration);
                addVertex(normalExit);
                connectTo(normalExit);
                addMethodOutput(callableDeclaration, normalExit);
                List<GraphNode<?>> lastNodes = new LinkedList<>(hangingNodes);
                clearHanging();
                // Exception exit
                Collection<ExceptionExitNode> exceptionExits = processExceptionSources(callableDeclaration);
                for (ExceptionExitNode node : exceptionExits) {
                    node.addVAUseActiveException();
                    hangingNodes.add(node);
                    lastNodes.addAll(hangingNodes);
                    clearHanging();
                }
                hangingNodes.addAll(lastNodes);
            }
            nonExecHangingNodes.add(getRootNode());

            MethodExitNode exit = new MethodExitNode(callableDeclaration);
            addVertex(exit);
            if (exceptionSourceMap.isEmpty())
                addMethodOutput(callableDeclaration, exit);
            nonExecHangingNodes.addAll(exitNonExecHangingNodes);
            connectTo(exit);

            processPendingNormalResultNodes();
        }

        /** Converts the remaining exception sources into a collection of exception exit nodes.
         * Each exception source is connected to at least one of the exception exit nodes. */
        protected Collection<ExceptionExitNode> processExceptionSources(CallableDeclaration<?> declaration) {
            if (!tryStack.isEmpty())
                throw new IllegalStateException("Can't process exception sources inside a Try statement.");
            Map<ResolvedType, ExceptionExitNode> exceptionExitMap = new HashMap<>();
            for (var entry : exceptionSourceMap.entrySet()) {
                // 1. Create "T exit" if it does not exist
                if (!exceptionExitMap.containsKey(entry.getKey()))
                    exceptionExitMap.put(entry.getKey(), addExceptionExitNode(declaration, entry.getKey()));
                ExceptionExitNode ee = exceptionExitMap.get(entry.getKey());
                for (ExceptionSource es : entry.getValue())
                    // 2. Connect exception source to "T exit"
                    addEdge(es.source, ee, es.isActive() ? new ControlFlowArc() : new ControlFlowArc.NonExecutable());
            }
            return exceptionExitMap.values();
        }

        /**
         * Creates the non-executable edges from "normal return" nodes to their target.
         * (the first node shared by every path from all normal and exceptional return nodes of
         * this method call -- which is equivalent to say the first node that post-dominates the call
         * or that post-dominates all return nodes).
         */
        protected void processPendingNormalResultNodes() {
            for (Map.Entry<NormalReturnNode, Set<ReturnNode>> entry : pendingNormalReturnNodes.entrySet())
                createNonExecArcFor(entry.getKey(), entry.getValue());
        }

        // TODO: improve accuracy, if there are multiple, select the one that is post-dominated by the others.
        // TODO: improve accuracy and speed by implementing the Roman Chariots problem.
        /** Creates the non-executable arc from "normal return" node to its target, given a set of its sibling return nodes. */
        protected void createNonExecArcFor(NormalReturnNode node, Set<ReturnNode> returnNodes) {
            PPControlDependencyBuilder cdBuilder = new PPControlDependencyBuilder(ESCFG.this, null);
            vertexSet().stream().sorted()
                    .filter(candidate -> {
                        for (ReturnNode retNode : returnNodes)
                            if (!cdBuilder.postDominates(retNode, candidate))
                                return false;
                        return true;
                    })
                    .findFirst()
                    .ifPresentOrElse(
                            n -> addNonExecutableControlFlowArc(node, n),
                            () -> {throw new IllegalStateException("A common post-dominator cannot be found for a normal exit!");});
        }

        @Override
        public void visit(TryStmt n, Void arg) {
            if (n.getFinallyBlock().isPresent())
                Logger.log("ES-CFG Builder", "try statement with unsupported finally block");
            stmtStack.push(n);
            tryStack.push(n);
            tryNonExecHangingStack.push(new HashSet<>());
            if (n.getResources().isNonEmpty())
                throw new IllegalStateException("try-with-resources is not supported");
            if (n.getFinallyBlock().isPresent())
                throw new IllegalStateException("try-finally is not supported");
            GraphNode<TryStmt> node = connectTo(n, "try");
            n.getTryBlock().accept(this, arg);
            List<GraphNode<?>> hanging = new LinkedList<>(hangingNodes);
            List<GraphNode<?>> nonExecHanging = new LinkedList<>(nonExecHangingNodes);
            clearHanging();
            for (CatchClause cc : n.getCatchClauses()) {
                cc.accept(this, arg);
                hanging.addAll(hangingNodes);
                nonExecHanging.addAll(nonExecHangingNodes);
                clearHanging();
            }
            hangingNodes.addAll(hanging);
            nonExecHangingNodes.addAll(nonExecHanging);
            nonExecHangingNodes.add(node);
            nonExecHangingNodes.addAll(tryNonExecHangingStack.pop());
            tryStack.pop();
            stmtStack.pop();
        }

        // =====================================================
        // ================= Exception sources =================
        // =====================================================

        /** Save an exception source to a map under all the exception's possible types. */
        protected void populateExceptionSourceMap(ExceptionSource source) {
            for (ResolvedType type : source.exceptions.keySet()) {
                exceptionSourceMap.computeIfAbsent(type, t -> new LinkedList<>());
                exceptionSourceMap.get(type).add(source);
            }
        }

        @Override
        public void visit(ThrowStmt n, Void arg) {
            stmtStack.push(n);
            GraphNode<ThrowStmt> stmt = connectTo(n);
            n.getExpression().accept(this, arg);
            populateExceptionSourceMap(new ExceptionSource(stmt, n.getExpression().calculateResolvedType()));
            clearHanging();
            nonExecHangingNodes.add(stmt);
            stmtStack.pop();
        }

        @Override
        public void visit(MethodCallExpr n, Void arg) {
            visitCallForExceptions(n);
        }

        @Override
        public void visit(ObjectCreationExpr n, Void arg) {
            visitCallForExceptions(n);
        }

        @Override
        public void visit(ExplicitConstructorInvocationStmt n, Void arg) {
            stmtStack.push(n);
            connectTo(n);
            visitCallForExceptions(n);
            stmtStack.pop();
        }

        /** Process a call that may throw exceptions. Generates normal and return nodes, and
         * registers the appropriate exception source. */
        protected void visitCallForExceptions(Resolvable<? extends ResolvedMethodLikeDeclaration> call) {
            ResolvedMethodLikeDeclaration resolved = call.resolve();
            if (resolved.getNumberOfSpecifiedExceptions() == 0)
                return;

            Set<ReturnNode> returnNodes = new HashSet<>();

            // Normal return
            NormalReturnNode normalReturn = NormalReturnNode.create(call);
            addVertex(normalReturn);
            GraphNode<?> stmtNode = findNodeByASTNode(stmtStack.peek()).orElseThrow();
            assert hangingNodes.size() == 1 && hangingNodes.get(0) == stmtNode;
            assert nonExecHangingNodes.isEmpty();
            returnNodes.add(normalReturn);
            connectTo(normalReturn);
            clearHanging();

            // Exception return
            for (ResolvedType type : resolved.getSpecifiedExceptions()) {
                hangingNodes.add(stmtNode);
                ExceptionReturnNode exceptionReturn = addExceptionReturnNode(call, type);
                exceptionReturn.addVADefineActiveException(null);
                populateExceptionSourceMap(new ExceptionSource(exceptionReturn, type));
                returnNodes.add(exceptionReturn);
                connectTo(exceptionReturn);
                if (tryNonExecHangingStack.isEmpty())
                    exitNonExecHangingNodes.add(exceptionReturn);
                else
                    tryNonExecHangingStack.peek().add(exceptionReturn);
                clearHanging();
            }

            // Register set of return nodes
            pendingNormalReturnNodes.put(normalReturn, returnNodes);

            // Ready for next instruction
            hangingNodes.add(normalReturn);
        }

        @Override
        public void visit(CatchClause n, Void arg) {
            // 1. Connect all available exception sources here
            Set<ExceptionSource> sources = new HashSet<>();
            for (List<ExceptionSource> list : exceptionSourceMap.values())
                sources.addAll(list);
            for (ExceptionSource src : sources)
                (src.isActive() ? hangingNodes : nonExecHangingNodes).add(src.source);
            GraphNode<?> node = connectTo(n, "catch (" + n.getParameter().toString() + ")");
            node.addVAUseActiveException();
            exceptionSourceMap.clear();
            // 2. Set up as exception source
            ExceptionSource catchES = ExceptionSource.merge(node, sources);
            Type type = n.getParameter().getType();
            if (type.isUnionType())
                type.asUnionType().getElements().forEach(t -> catchES.deactivateTypes(t.resolve().asReferenceType()));
            else if (type.isReferenceType())
                catchES.deactivateTypes(type.resolve().asReferenceType());
            else
                throw new IllegalStateException("catch node with type different to union/reference type");
            populateExceptionSourceMap(catchES);

            // 3. Connect and visit body
            n.getBody().accept(this, arg);
        }

        // =====================================================================================
        // ================= Statements that may directly contain method calls =================
        // =====================================================================================
        // Note: expressions are visited as part of super.visit(Node, Void), see ACFG and CFG.

        @Override
        public void visit(IfStmt ifStmt, Void arg) {
            stmtStack.push(ifStmt);
            super.visit(ifStmt, arg);
            stmtStack.pop();
        }

        @Override
        public void visit(WhileStmt whileStmt, Void arg) {
            stmtStack.push(whileStmt);
            super.visit(whileStmt, arg);
            stmtStack.pop();
        }

        @Override
        public void visit(DoStmt doStmt, Void arg) {
            stmtStack.push(doStmt);
            super.visit(doStmt, arg);
            stmtStack.pop();
        }

        @Override
        public void visit(ForStmt forStmt, Void arg) {
            stmtStack.push(forStmt);
            super.visit(forStmt, arg);
            stmtStack.pop();
        }

        @Override
        public void visit(ForEachStmt forEachStmt, Void arg) {
            stmtStack.push(forEachStmt);
            super.visit(forEachStmt, arg);
            stmtStack.pop();
        }

        @Override
        public void visit(SwitchStmt switchStmt, Void arg) {
            stmtStack.push(switchStmt);
            super.visit(switchStmt, arg);
            stmtStack.pop();
        }

        @Override
        public void visit(ReturnStmt returnStmt, Void arg) {
            stmtStack.push(returnStmt);
            super.visit(returnStmt, arg);
            stmtStack.pop();
        }

        @Override
        public void visit(ExpressionStmt expressionStmt, Void arg) {
            stmtStack.push(expressionStmt);
            super.visit(expressionStmt, arg);
            stmtStack.pop();
        }
    }
}
