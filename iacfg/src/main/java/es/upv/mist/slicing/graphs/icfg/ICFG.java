package es.upv.mist.slicing.graphs.icfg;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.arcs.cfg.ControlFlowArc;
import es.upv.mist.slicing.graphs.Buildable;
import es.upv.mist.slicing.graphs.CallGraph;
import es.upv.mist.slicing.graphs.ClassGraph;
import es.upv.mist.slicing.graphs.Graph;
import es.upv.mist.slicing.graphs.augmented.ACFG;
import es.upv.mist.slicing.graphs.cfg.CFG;
import es.upv.mist.slicing.graphs.sdg.InterproceduralDefinitionFinder;
import es.upv.mist.slicing.graphs.sdg.InterproceduralUsageFinder;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.VariableAction;
import es.upv.mist.slicing.nodes.io.ActualIONode;
import es.upv.mist.slicing.nodes.io.CallNode;
import es.upv.mist.slicing.nodes.io.MethodExitNode;
import es.upv.mist.slicing.utils.ASTUtils;

import java.util.*;

import static es.upv.mist.slicing.util.SingletonCollector.toSingleton;

/**
 * An interprocedural CFG, whose component CFGs are built as ACFGs.
 */
public class ICFG extends Graph implements Buildable<NodeList<CompilationUnit>> {

    protected final Map<CallableDeclaration<?>, CFG> cfgMap = ASTUtils.newIdentityHashMap();
    protected boolean built = false;

    public void addControlFlowArc(GraphNode<?> from, GraphNode<?> to) {
        addEdge(from, to, new ControlFlowArc());
    }

    public void addNonExecControlFlowArc(GraphNode<?> from, GraphNode<?> to) {
        addEdge(from, to, new ControlFlowArc.NonExecutable());
    }

    @Override
    public void build(NodeList<CompilationUnit> nodeList) {
        if (built) return;
        new Builder().build(nodeList);
        built = true;
    }

    /** Builds the ICFG on a pre-built set of CFGs and their call graph. */
    public void build(Map<CallableDeclaration<?>, CFG> cfgMap, CallGraph callGraph) {
        if (built) return;
        this.cfgMap.putAll(cfgMap);
        new Builder().build(callGraph);
        built = true;
    }

    @Override
    public boolean isBuilt() {
        return built;
    }

    public class Builder {
        protected CallGraph callGraph;

        public void build(NodeList<CompilationUnit> units) {
            createClassGraph(units);
            buildCFGs(units);
            createCallGraph(units);
            dataFlowAnalysis();
            buildFromCFGs();
        }

        public void build(CallGraph callGraph) {
            this.callGraph = callGraph;
            buildFromCFGs();
        }

        protected void buildFromCFGs() {
            copyCFGs();
            expandCalls();
            joinCFGs();
        }

        protected void buildCFGs(NodeList<CompilationUnit> nodeList) {
            nodeList.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(MethodDeclaration n, Void arg) {
                    visitCallable(n);
                    super.visit(n, arg);
                }

                @Override
                public void visit(ConstructorDeclaration n, Void arg) {
                    visitCallable(n);
                    super.visit(n, arg);
                }

                private void visitCallable(CallableDeclaration<?> n) {
                    boolean isInInterface = n.findAncestor(ClassOrInterfaceDeclaration.class)
                            .map(ClassOrInterfaceDeclaration::isInterface).orElse(false);
                    if (n.isAbstract() || isInInterface)
                        return; // Allow abstract methods
                    ACFG acfg = new ACFG();
                    acfg.build(n);
                    cfgMap.put(n, acfg);
                }
            }, null);
        }

        /** Create class graph from the list of compilation units. */
        protected void createClassGraph(NodeList<CompilationUnit> nodeList){
            ClassGraph.getNewInstance().build(nodeList);
        }

        /** Create call graph from the list of compilation units. */
        protected void createCallGraph(NodeList<CompilationUnit> nodeList) {
            callGraph = new CallGraph(cfgMap, ClassGraph.getInstance());
            callGraph.build(nodeList);
        }

        /** Perform interprocedural analyses to determine the actual and formal nodes. */
        protected void dataFlowAnalysis() {
            new InterproceduralDefinitionFinder(callGraph, cfgMap).save(); // 3.1
            new InterproceduralUsageFinder(callGraph, cfgMap).save();      // 3.2
        }

        /** Build a PDG per declaration, based on the CFGs built previously and enhanced by data analyses. */
        protected void copyCFGs() {
            for (CFG cfg : cfgMap.values()) {
                cfg.vertexSet().forEach(ICFG.this::addVertex);
                cfg.edgeSet().forEach(arc -> addEdge(cfg.getEdgeSource(arc), cfg.getEdgeTarget(arc), arc));
            }
        }

        /**
         * Expands movable variable actions in calls, enter and exit nodes to their
         * own separate nodes, placing them in the order of execution, and maintaining
         * the properties of each CFG (single sink, single source).
         */
        protected void expandCalls() {
            for (GraphNode<?> graphNode : Set.copyOf(vertexSet())) {
                if (isEnter(graphNode) || isExit(graphNode))
                    expandEnterExitNode(graphNode);
                else
                    expandCalls(graphNode);
            }
        }

        /**
         * Extracts movable variable actions from a given Enter or Exit node. For the former,
         * it places new nodes after it, moving the first instructions after formal-in nodes.
         * For the latter, it places new nodes before it, moving the last instructions before
         * formal-out nodes.
         */
        protected void expandEnterExitNode(GraphNode<?> node) {
            LinkedList<GraphNode<?>> list = new LinkedList<>();

            // All actions should be movables, move each and connect them as a linked list
            // movable1 --> movable2 ... --> movableN
            for (VariableAction va : List.copyOf(node.getVariableActions())) {
                if (va instanceof VariableAction.Movable movable) {
                    movable.move(ICFG.this);
                    connectAppend(list, movable.getRealNode());
                } else {
                    throw new IllegalStateException("Enter node has non-movable actions");
                }
            }

            if (!list.isEmpty())
                insertListInGraph(node, list, isEnter(node));
        }

        /**
         * Inserts a list of nodes in the graph before or after the given node. This method
         * requires that the insList of nodes already be linked together in the graph.
         * This method is not affected by and does not modify non-executable control-flow edges. <br>
         * Graph before this method: <code>a, b -> node -> x, y | ins[0] -> ... -> ins[N]</code> <br>
         * Graph after this method (before the node): <code>a, b -> ins[0] -> ... -> ins[N] -> node -> x, y</code> <br>
         * Graph after this method (after the node): <code>a, b -> node -> ins[0] -> ... -> ins[N] -> x, y</code>
         */
        protected void insertListInGraph(GraphNode<?> node, LinkedList<GraphNode<?>> insList, boolean after) {
            if (after) {
                for (Arc e : outgoingEdgesOf(node).stream()
                        .filter(Arc::isExecutableControlFlowArc)
                        .toList()) {
                    GraphNode<?> target = getEdgeTarget(e);
                    removeEdge(e);
                    addEdge(insList.getLast(), target, e);
                }
                addControlFlowArc(node, insList.getFirst());
            } else {
                for (Arc e : incomingEdgesOf(node).stream()
                        .filter(Arc::isExecutableControlFlowArc)
                        .toList()) {
                    GraphNode<?> source = getEdgeSource(e);
                    removeEdge(e);
                    addEdge(source, insList.getFirst(), e);
                }
                addControlFlowArc(insList.getLast(), node);
            }
        }

        /**
         * Expands the calls inside a single node, if any. The node that contains
         * the calls is placed after all the calls happened, with the previous instruction
         * connected to the first actual-in. The sequence of nodes in a single call is:
         * previous instruction, actual-in*, call node, return node, actual-out*, graphNode,
         * next instruction. * means that 0-n nodes may appear.
         */
        protected void expandCalls(GraphNode<?> graphNode) {
            Iterator<VariableAction> iterator = List.copyOf(graphNode.getVariableActions()).iterator();
            LinkedList<GraphNode<?>> extracted = new LinkedList<>();
            while (iterator.hasNext()) {
                VariableAction va = iterator.next();
                if (va instanceof VariableAction.Movable)
                    throw new IllegalStateException("Movable outside Enter/Exit or call");
                else if (va instanceof VariableAction.CallMarker marker && marker.isEnter()) {
                    extracted.addAll(extractMovables(iterator, marker));
                }
            }
            // Place extracted sequence before the node
            if (!extracted.isEmpty())
                insertListInGraph(graphNode, extracted, false);
        }

        /**
         * Extracts movable nodes from a single call, recursively.
         * @param iterator An iterator over a GraphNode's variable actions, which will be consumed
         *                 until the closing CallMarker that matches callMarker is found.
         * @param callMarker The call marker that enters the call to be analyzed.
         * @return The list of nodes, inserted into the graph and connected to each other.
         *     Includes all nested calls. The first node in the list has no incoming edges.
         *     The last node in the list has no outgoing edges.
         */
        protected LinkedList<GraphNode<?>> extractMovables(Iterator<VariableAction> iterator, VariableAction.CallMarker callMarker) {
            LinkedList<GraphNode<?>> res = new LinkedList<>();
            boolean input = true;
            while (iterator.hasNext()) {
                VariableAction va = iterator.next();
                if (va instanceof VariableAction.CallMarker newMarker) {
                    if (newMarker.isEnter()) {
                        // Recursively enter the next function and connect it. Consumes movables.
                        List<GraphNode<?>> moved = extractMovables(iterator, newMarker);
                        connectAppend(res, moved);
                    } else if (newMarker.getCall().equals(callMarker.getCall())) {
                        return res; // Process ended.
                    } else {
                        throw new IllegalStateException("Invalid pairing of call markers");
                    }
                } else if (va instanceof VariableAction.Movable movable) {
                    if (containsVertex(movable.getRealNode()))
                        continue; // Skip multi-action nodes
                    movable.move(ICFG.this);
                    // Check whether to insert call node (when we move from input to output)
                    if (input && !isActualIn(movable.getRealNode())) {
                        input = false;
                        insertCallerNode(res, CallNode.create(callMarker.getCall()), callMarker);
                        // If it is a call to void method, add a blank "return"
                        if (!(movable.getRealNode() instanceof CallNode.Return))
                            insertCallerNode(res, CallNode.Return.create(callMarker.getCall()), callMarker);
                        else {
                            addNonExecControlFlowArc(res.getLast(), movable.getRealNode());
                            res.add(movable.getRealNode());
                            continue;
                        }
                    }
                    // Connect node to previous nodes and add to resulting list
                    connectAppend(res, movable.getRealNode());
                }
            }
            throw new IllegalStateException("Closing call marker not found!");
        }

        /** Inserts a newly created CallNode or Return node into the graph and connects it to the sequence. */
        protected void insertCallerNode(List<GraphNode<?>> sequence, GraphNode<?> node, VariableAction.CallMarker callMarker) {
            addVertex(node);
            if (callMarker.getGraphNode().isImplicitInstruction())
                node.markAsImplicit();
            if (node instanceof CallNode.Return) {
                addNonExecControlFlowArc(sequence.getLast(), node);
                sequence.add(node);
            } else
                connectAppend(sequence, node);
        }

        /** Connects a node with the last element of the list (if not empty) and adds it to the list. */
        private void connectAppend(List<GraphNode<?>> list, GraphNode<?> node) {
            if (!list.isEmpty())
                addControlFlowArc(list.getLast(), node);
            list.add(node);
        }

        /** Connects two lists of nodes in the graph (if the first is not empty) and concatenates them. */
        private void connectAppend(List<GraphNode<?>> list, List<GraphNode<?>> list2) {
            if (!list.isEmpty())
                addControlFlowArc(list.getLast(), list2.getFirst());
            list.addAll(list2);
        }

        protected void joinCFGs() {
            for (CallGraph.Edge<?> call : callGraph.edgeSet()) {
                // Nodes to be paired up
                GraphNode<?> callNode  = vertexSet().stream()
                        .filter(n -> n instanceof CallNode)
                        .filter(n -> n.getAstNode().equals(call.getCall()))
                        .collect(toSingleton());
                GraphNode<?> returnNode = vertexSet().stream()
                        .filter(n -> n instanceof CallNode.Return)
                        .filter(n -> n.getAstNode().equals(call.getCall()))
                        .collect(toSingleton());
                GraphNode<?> enterNode = cfgMap.get(callGraph.getEdgeTarget(call).getDeclaration()).getRootNode();
                GraphNode<?> exitNode  = cfgMap.get(callGraph.getEdgeTarget(call).getDeclaration()).getExitNode();
                // Connections
                addControlFlowArc(callNode, enterNode);
                addControlFlowArc(exitNode, returnNode);
            }
        }

        protected static boolean isActualIn(GraphNode<?> node) {
            return node instanceof ActualIONode && ((ActualIONode) node).isInput();
        }

        private static boolean isExit(GraphNode<?> graphNode) {
            return graphNode instanceof MethodExitNode;
        }

        private boolean isEnter(GraphNode<?> graphNode) {
            return incomingEdgesOf(graphNode).isEmpty();
        }
    }
}