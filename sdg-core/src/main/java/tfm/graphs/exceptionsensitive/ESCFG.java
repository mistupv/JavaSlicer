package tfm.graphs.exceptionsensitive;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import tfm.arcs.cfg.ControlFlowArc;
import tfm.graphs.augmented.ACFG;
import tfm.graphs.augmented.ACFGBuilder;
import tfm.graphs.augmented.ControlDependencyBuilder;
import tfm.graphs.cfg.CFGBuilder;
import tfm.nodes.*;
import tfm.nodes.type.NodeType;
import tfm.utils.Logger;

import java.util.*;

public class ESCFG extends ACFG {
    protected final static String ACTIVE_EXCEPTION_VARIABLE = "-activeException-";

    @Override
    protected CFGBuilder newCFGBuilder() {
        return new Builder();
    }

    protected ExceptionExitNode addExceptionExitNode(MethodDeclaration method, ResolvedType type) {
        ExceptionExitNode node = new ExceptionExitNode(method, type);
        addNode(node);
        return node;
    }

    protected ExceptionReturnNode addExceptionReturnNode(MethodCallExpr call, ResolvedType type) {
        ExceptionReturnNode node = new ExceptionReturnNode(call, type);
        addNode(node);
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

        protected Builder() {
            super(ESCFG.this);
        }

        @Override
        public void visit(MethodDeclaration methodDeclaration, Void arg) {
            if (getRootNode().isPresent())
                throw new IllegalStateException("CFG is only allowed for one method, not multiple!");
            if (methodDeclaration.getBody().isEmpty())
                throw new IllegalStateException("The method must have a body!");

            buildRootNode("ENTER " + methodDeclaration.getDeclarationAsString(false, false, false),
                    methodDeclaration, TypeNodeFactory.fromType(NodeType.METHOD_ENTER));

            hangingNodes.add(getRootNode().get());
            for (Parameter param : methodDeclaration.getParameters())
                connectTo(addFormalInGraphNode(methodDeclaration, param));
            methodDeclaration.getBody().get().accept(this, arg);
            returnList.stream().filter(node -> !hangingNodes.contains(node)).forEach(hangingNodes::add);
            if (exceptionSourceMap.isEmpty()) {
                createAndConnectFormalOutNodes(methodDeclaration);
            } else {
                // Normal exit
                NormalExitNode normalExit = new NormalExitNode(methodDeclaration);
                addNode(normalExit);
                connectTo(normalExit);
                createAndConnectFormalOutNodes(methodDeclaration);
                List<GraphNode<?>> lastNodes = new LinkedList<>(hangingNodes);
                clearHanging();
                // Exception exit
                Collection<ExceptionExitNode> exceptionExits = processExceptionSources(methodDeclaration);
                for (ExceptionExitNode node : exceptionExits) {
                    node.addUsedVariable(new NameExpr(ACTIVE_EXCEPTION_VARIABLE));
                    hangingNodes.add(node);
                    createAndConnectFormalOutNodes(methodDeclaration, false);
                    lastNodes.addAll(hangingNodes);
                    clearHanging();
                }
                hangingNodes.addAll(lastNodes);
            }
            ExitNode exit = new ExitNode(methodDeclaration);
            addNode(exit);
            nonExecHangingNodes.addAll(exitNonExecHangingNodes);
            nonExecHangingNodes.add(getRootNode().get());
            connectTo(exit);

            processPendingNormalResultNodes();
        }

        protected Collection<ExceptionExitNode> processExceptionSources(MethodDeclaration method) {
            if (!tryStack.isEmpty())
                throw new IllegalStateException("Can't process exception sources inside a Try statement.");
            Map<ResolvedType, ExceptionExitNode> exceptionExitMap = new HashMap<>();
            for (ResolvedType type : exceptionSourceMap.keySet()) {
                // 1. Create "T exit" if it does not exist
                if (!exceptionExitMap.containsKey(type))
                    exceptionExitMap.put(type, addExceptionExitNode(method, type));
                ExceptionExitNode ee = exceptionExitMap.get(type);
                for (ExceptionSource es : exceptionSourceMap.get(type))
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

        protected void createNonExecArcFor(NormalReturnNode node, Set<ReturnNode> returnNodes) {
            ControlDependencyBuilder cdBuilder = new ControlDependencyBuilder(ESCFG.this, null);
            vertexSet().stream()
                    .sorted(Comparator.comparingLong(GraphNode::getId))
                    .filter(candidate -> {
                        for (ReturnNode retNode : returnNodes)
                            if (!cdBuilder.postdominates(retNode, candidate))
                                return false;
                        return true;
                    })
                    .findFirst()
                    .ifPresentOrElse(
                            n -> addNonExecutableControlFlowEdge(node, n),
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

        protected void populateExceptionSourceMap(ExceptionSource source) {
            for (ResolvedType type : source.exceptions.keySet()) {
                exceptionSourceMap.computeIfAbsent(type, (t) -> new LinkedList<>());
                exceptionSourceMap.get(type).add(source);
            }
        }

        @Override
        public void visit(ThrowStmt n, Void arg) {
            stmtStack.push(n);
            GraphNode<ThrowStmt> stmt = connectTo(n);
            n.getExpression().accept(this, arg);
            stmt.addDefinedVariable(new NameExpr(ACTIVE_EXCEPTION_VARIABLE));
            populateExceptionSourceMap(new ExceptionSource(stmt, n.getExpression().calculateResolvedType()));
            clearHanging();
            nonExecHangingNodes.add(stmt);
            stmtStack.pop();
        }

        @Override
        public void visit(MethodCallExpr n, Void arg) {
            ResolvedMethodDeclaration resolved = n.resolve();
            if (resolved.getNumberOfSpecifiedExceptions() == 0)
                return;

            Set<ReturnNode> returnNodes = new HashSet<>();

            // Normal return
            NormalReturnNode normalReturn = new NormalReturnNode(n);
            addNode(normalReturn);
            GraphNode<?> stmtNode = findNodeByASTNode(stmtStack.peek()).orElseThrow();
            assert hangingNodes.size() == 1 && hangingNodes.get(0) == stmtNode;
            assert nonExecHangingNodes.size() == 0;
            returnNodes.add(normalReturn);
            connectTo(normalReturn);
            clearHanging();

            // Exception return
            for (ResolvedType type : resolved.getSpecifiedExceptions()) {
                hangingNodes.add(stmtNode);
                ExceptionReturnNode exceptionReturn = addExceptionReturnNode(n, type);
                exceptionReturn.addDefinedVariable(new NameExpr(ACTIVE_EXCEPTION_VARIABLE));
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
            node.addUsedVariable(new NameExpr(ACTIVE_EXCEPTION_VARIABLE));
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
