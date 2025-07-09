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
import es.upv.mist.slicing.graphs.augmented.ACFG;
import es.upv.mist.slicing.graphs.cfg.CFG;
import es.upv.mist.slicing.graphs.scrs.CallSCR;
import es.upv.mist.slicing.graphs.scrs.CallSCRGraph;
import es.upv.mist.slicing.graphs.scrs.IntraSCR;
import es.upv.mist.slicing.graphs.scrs.IntraSCRGraph;
import es.upv.mist.slicing.graphs.sdg.InterproceduralDefinitionFinder;
import es.upv.mist.slicing.graphs.sdg.InterproceduralUsageFinder;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.VariableAction;
import es.upv.mist.slicing.nodes.io.ActualIONode;
import es.upv.mist.slicing.nodes.io.CallNode;
import es.upv.mist.slicing.nodes.io.MethodExitNode;
import es.upv.mist.slicing.utils.ASTUtils;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.alg.util.Triple;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.DepthFirstIterator;

import java.util.*;

import static es.upv.mist.slicing.util.SingletonCollector.toSingleton;

/**
 * An interprocedural CFG, whose component CFGs are built as ACFGs.
 */
public class ICFG extends es.upv.mist.slicing.graphs.Graph implements Buildable<NodeList<CompilationUnit>> {

    protected final Map<CallableDeclaration<?>, CFG> cfgMap = ASTUtils.newIdentityHashMap();
    protected boolean built = false;

    public ControlFlowArc addControlFlowArc(GraphNode<?> from, GraphNode<?> to) {
        ControlFlowArc a = new ControlFlowArc();
        addEdge(from, to, a);
        return a;
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
        /** A map to locate interprocedural {@link ControlFlowArc}s that correspond to a given {@link CallGraph}'s edge. */
        protected final Map<CallGraph.Edge<?>, List<ControlFlowArc>> callGraphEdge2ICFGArcMap = new HashMap<>();
        /** The strongly connected components of the {@link CallGraph}. */
        protected CallSCRGraph cSCRs;
        /** A map to locate the set of {@link #intraSCRs} nodes that correspond (transitively) to a given {@link #cSCRs} node. */
        protected final Map<CallSCR, Set<IntraSCR>> transitiveMap = new HashMap<>();
        /** The strongly connected components of the {@link ICFG}, computed while ignoring
         *  interprocedural edges that connect {@link #cSCRs} nodes. <br>
         *  Fulfils steps 2-3 of the Nanda-Ramesh topological numbers' algorithm. */
        protected IntraSCRGraph intraSCRs;
        /** Non-Recursive interprocedural Arcs from {@link  #intraSCRs} */
        protected Set<Triple<GraphNode<?>, GraphNode<?>, ControlFlowArc>> interprocNonRecArcs = new HashSet<>();
        /** A map to locate the correspondent call node from a return node */
        protected final Map<IntraSCR, IntraSCR> returnToCorrespondentCallSiteMap = new HashMap<>();
        /** counter for topologicalNumbers */
        protected int topologicalNumber = 0;

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
            computeCallSCRs();
            computeIntraSCRs();
            buildISCR();
            generateTopologicalNumbers();
        }

        private void generateTopologicalNumbers() {
            // get exit node
            IntraSCR exitNode = null;
            for (IntraSCR cSCRNode : intraSCRs.vertexSet()) {
                if (intraSCRs.outDegreeOf(cSCRNode) == 0) {
                    exitNode = cSCRNode;
                    break;
                }
            }
            //callGetTopologicalNumber for exitNode (predecessors are visited inside)
            getTopologicalNumber(exitNode, new HashSet<>(), new Stack<>(), new HashMap<>());
        }

        private <V> void getTopologicalNumber(IntraSCR node, Set<IntraSCR> proccessedIntraSCRs, Stack<IntraSCR> callStack, Map<IntraSCR, Set<IntraSCR>> callNodeProcessMap) {
            if(proccessedIntraSCRs.contains(node)) {
                return;
            }

            for(IntraSCR predecessor : Graphs.predecessorListOf(intraSCRs, node)) {
                if (isCallSCR(predecessor)) {
                    if (callStack.isEmpty() || !predecessor.equals(callStack.peek()))
                        continue; // Non-matching call nodes are ignored
                    callStack.pop();
                    getTopologicalNumber(predecessor, proccessedIntraSCRs, callStack, callNodeProcessMap);
                } else if(isReturnSCR(predecessor)) {
                    IntraSCR callNode = returnToCorrespondentCallSiteMap.get(predecessor);
                    callStack.push(callNode);
                    getTopologicalNumber(predecessor, proccessedIntraSCRs, callStack, callNodeProcessMap);
                    if(callNodeProcessMap.containsKey(callNode)) {
                        for (IntraSCR intraSCR : callNodeProcessMap.get(callNode)) {
                            proccessedIntraSCRs.remove(intraSCR);
                        }
                    }
                } else {
                    if(!callStack.isEmpty()) {
                        callNodeProcessMap.computeIfAbsent(callStack.peek(), k -> new HashSet<>()).add(predecessor);
                    }
                    getTopologicalNumber(predecessor, proccessedIntraSCRs, callStack, callNodeProcessMap);
                }
            }
            node.addTopologicalNumber(topologicalNumber++);

            proccessedIntraSCRs.add(node);
        }

        private static boolean isCallSCR(IntraSCR node) {
            return node.vertexSet().size() == 1 && node.vertexSet().iterator().next() instanceof CallNode;
        }

        private static boolean isReturnSCR(IntraSCR node) {
            return node.vertexSet().size() == 1 && node.vertexSet().iterator().next() instanceof CallNode.Return;
        }

        private void addEdgesToIntraSCRs(Set<Triple<GraphNode<?>, GraphNode<?>, ControlFlowArc>> deletedArcs) {
            for (Triple<GraphNode<?>, GraphNode<?>, ControlFlowArc> arc : deletedArcs) {
                IntraSCR srcRegion = intraSCRs.getRegion(arc.getFirst());
                IntraSCR tgtRegion = intraSCRs.getRegion(arc.getSecond());
                if (srcRegion.equals(tgtRegion))
                    srcRegion.addEdge(arc.getFirst(), arc.getSecond(), arc.getThird());
                else
                    intraSCRs.addEdge(srcRegion, tgtRegion);
            }
        }

        private void buildCacheTransitive() {
            Set<CallSCR> processedCSCRNode = new HashSet<>();

            // startFromMain
            // getMain vertex
            CallSCR mainNode = null;
            for (CallSCR cSCRNode : cSCRs.vertexSet()) {
                if (cSCRs.inDegreeOf(cSCRNode) == 0) {
                    mainNode = cSCRNode;
                    break;
                }
            }
            //callCacheTransitive for mainNode (successors are visited inside)
            cacheTransitive(mainNode, processedCSCRNode);
        }

        private void cacheTransitive(CallSCR cSCRNode, Set<CallSCR> processedCSCRs) {
            if (processedCSCRs.contains(cSCRNode)) {
                return;
            }
            processedCSCRs.add(cSCRNode);

            Set<IntraSCR> intraSCRNodes = getEnterNodesRelatedToProcess(cSCRNode);
            Set<IntraSCR> SCRNodes = new HashSet<>();
            getAllIntraSCRNodes(intraSCRNodes, SCRNodes);
            transitiveMap.computeIfAbsent(cSCRNode, k -> new HashSet<>()).addAll(SCRNodes);

            for (CallSCR successor : Graphs.successorListOf(cSCRs, cSCRNode)) {
                cacheTransitive(successor, processedCSCRs);
                transitiveMap.get(cSCRNode).addAll(transitiveMap.get(successor));
            }
        }

        private Set<IntraSCR> getEnterNodesRelatedToProcess(CallSCR cSCRNode) {
            Set<IntraSCR> intraSCRNodes = new HashSet<>();
            for (CallGraph.Vertex process : cSCRNode.vertexSet()) {
                for (IntraSCR intraSCR : intraSCRs.vertexSet()) {
                    if (intraSCRs.inDegreeOf(intraSCR) == 0)
                        for (GraphNode<?> graphNode : intraSCR.vertexSet()) {
                            if (graphNode.getAstNode() instanceof MethodDeclaration
                                    && ASTUtils.equalsWithRange(process.getDeclaration(), graphNode.getAstNode())) {
                                intraSCRNodes.add(intraSCR);
                            }
                        }
                }
            }
            return intraSCRNodes;
        }

        private void getAllIntraSCRNodes(Set<IntraSCR> intraSCRNodes, Set<IntraSCR> SCRNodes) {
            for (IntraSCR intraSCRNode : intraSCRNodes) {
                SCRNodes.add(intraSCRNode);
                Iterator<IntraSCR> iterator = new DepthFirstIterator<>(intraSCRs,intraSCRNode);
                while (iterator.hasNext()) {
                    SCRNodes.add(iterator.next());
                }
            }
        }

        private void buildISCR() {
            List<IntraSCR> processedIntraSCRs = new ArrayList<>();
            // startFromMain
            // getMain vertex
            IntraSCR mainIntraSCR = null;
            List<IntraSCR> listIntraSCR = new ArrayList<>();
            for (IntraSCR intraSCR : intraSCRs.vertexSet()) {
                if(intraSCRs.incomingEdgesOf(intraSCR).isEmpty()) {
                    listIntraSCR.add(intraSCR);
                }
            }
            for (IntraSCR intraSCRNode : listIntraSCR) {
                for (GraphNode<?> graphNode : intraSCRNode.vertexSet()) {
                    MethodDeclaration declaration = (MethodDeclaration) graphNode.getAstNode();
                    if(declaration.isPublic() && declaration.isStatic() && declaration.getType().isVoidType()) {
                        mainIntraSCR = intraSCRNode;
                    }
                }
            }
            buildISCRGraph(mainIntraSCR, processedIntraSCRs);
            deleteCallReturnEdges();
            deleteMainExitEdge(mainIntraSCR);
            getMainProcess(mainIntraSCR);
        }

        private void getMainProcess(IntraSCR mainIntraSCR) {
            ConnectivityInspector<IntraSCR, DefaultEdge> inspector = new ConnectivityInspector<>(intraSCRs);
            Set<IntraSCR> mainProcess = inspector.connectedSetOf(mainIntraSCR);
            Set<IntraSCR> nodesToBeDeleted = new HashSet<>();
            for (IntraSCR intraSCR : intraSCRs.vertexSet()) {
                if(!mainProcess.contains(intraSCR)){
                    nodesToBeDeleted.add(intraSCR);
                }
            }
            for (IntraSCR node : nodesToBeDeleted) {
                intraSCRs.removeVertex(node);
            }
        }

        private void deleteMainExitEdge(IntraSCR mainIntraSCR) {
            IntraSCR exitNode = intraSCRs.outgoingEdgesOf(mainIntraSCR).stream()
                    .map(intraSCRs::getEdgeTarget)
                    .filter(target -> target.vertexSet().stream().anyMatch(node -> node instanceof MethodExitNode))
                    .findFirst().orElseThrow();

            intraSCRs.removeEdge(mainIntraSCR, exitNode);
        }

        private void deleteCallReturnEdges() {
            Set<DefaultEdge> toBeDeletedSet = new HashSet<>();
            for (DefaultEdge e : intraSCRs.edgeSet()) {
                IntraSCR source = intraSCRs.getEdgeSource(e);
                IntraSCR target = intraSCRs.getEdgeTarget(e);
                // Deleted only if source vertex is simple intraSCR
                // ToDo: check if target will always be simple (ask Carlos)
                if(source.vertexSet().size() == 1){
                    for(GraphNode<?> src : source.vertexSet()) {
                        for(GraphNode<?> tgt : target.vertexSet()) {
                            if(src instanceof CallNode && tgt instanceof CallNode.Return) {
                                returnToCorrespondentCallSiteMap.put(target, source);
                                toBeDeletedSet.add(e);
                            }
                        }
                    }
                }
            }
            for (DefaultEdge deletedEdge : toBeDeletedSet) {
                intraSCRs.removeEdge(deletedEdge);
            }
        }

        private static void mergeGraphs(IntraSCR target, IntraSCR src) {
            for (GraphNode<?> n : src.vertexSet())
                if (!target.addVertex(n))
                    throw new RuntimeException();
            for (Arc arc : src.edgeSet())
                if (!target.addEdge(src.getEdgeSource(arc), src.getEdgeTarget(arc), arc))
                    throw new RuntimeException();
        }

        private void buildISCRGraph(IntraSCR intraSCR, List<IntraSCR> processedIntraSCRs) {
            if (processedIntraSCRs.contains(intraSCR)) {
                return;
            }
            processedIntraSCRs.add(intraSCR);
            Set<GraphNode<?>> graphNodes = intraSCR.vertexSet();
            if (graphNodes.size() > 1) {
                //multi
                Set<IntraSCR> toBeMerged = new HashSet<>();
                for (GraphNode<?> graphNode : graphNodes) {
                    if (graphNode instanceof CallNode) {
                        for (Triple<GraphNode<?>, GraphNode<?>, ControlFlowArc> arc : interprocNonRecArcs) {
                            if (graphNode.equals(arc.getFirst())) {
                                @SuppressWarnings("unchecked")
                                GraphNode<CallableDeclaration<?>> enterNode = (GraphNode<CallableDeclaration<?>>) arc.getSecond();
                                Set<IntraSCR> procIntraSCRset = getProcIntraSCRset(enterNode);
                                toBeMerged.addAll(procIntraSCRset);
                                IntraSCR iSCRtarget = intraSCRs.vertexSet().stream()
                                        .filter(iSCR -> iSCR.containsVertex(enterNode))
                                        .findFirst().orElseThrow();
                                intraSCRs.removeEdge(intraSCR, iSCRtarget);
                                break;
                            }
                        }
                    } else if(graphNode instanceof CallNode.Return){
                        for (Triple<GraphNode<?>, GraphNode<?>, ControlFlowArc> arc : interprocNonRecArcs) {
                            if (graphNode.equals(arc.getSecond())) {
                                @SuppressWarnings("unchecked")
                                GraphNode<CallableDeclaration<?>> exitNode = (GraphNode<CallableDeclaration<?>>) arc.getFirst();
                                IntraSCR iSCRSource = intraSCRs.vertexSet().stream()
                                        .filter(iSCR -> iSCR.containsVertex(exitNode))
                                        .findFirst().orElseThrow();
                                intraSCRs.removeEdge(iSCRSource, intraSCR);
                                break;
                            }
                        }
                    }
                }
                for (IntraSCR graph : toBeMerged)
                    mergeGraphs(intraSCR, graph);
            } else if (graphNodes.size() == 1) {
                //single
                GraphNode<?> graphNode = graphNodes.iterator().next();
                if (graphNode instanceof CallNode) {
                    for (Triple<GraphNode<?>, GraphNode<?>, ControlFlowArc> arc : interprocNonRecArcs) {
                        if (graphNode.equals(arc.getFirst())) {
                            @SuppressWarnings("unchecked")
                            GraphNode<CallableDeclaration<?>> enterNode = (GraphNode<CallableDeclaration<?>>) arc.getSecond();
                            IntraSCR enterIntraSCR = getEnterIntraSCR(enterNode);
                            buildISCRGraph(enterIntraSCR, processedIntraSCRs);
                        }
                    }
                }
            } else {
                throw new IllegalStateException("The intraSCRs is empty");
            }

            for (IntraSCR successor : Graphs.successorListOf(intraSCRs, intraSCR)) {
                buildISCRGraph(successor, processedIntraSCRs);
            }
        }

        private IntraSCR getEnterIntraSCR(GraphNode<CallableDeclaration<?>> enterNode) {
            return getProcIntraSCRset(enterNode).stream()
                    .filter(scr -> scr.containsVertex(enterNode))
                    .findFirst()
                    .orElseThrow();
        }

        private Set<IntraSCR> getProcIntraSCRset(GraphNode<CallableDeclaration<?>> node) {
            CallGraph.Vertex procVertex = new CallGraph.Vertex(node.getAstNode());
            CallSCR procRegion = cSCRs.getRegion(procVertex);
            return transitiveMap.get(procRegion);
        }

        protected void computeIntraSCRs() {
            // 1. Copiar el ICFG a un nuevo grafo sin arcos duplicados. (opcional)
            var simpleICFG = deleteDuplicatedEdges(ICFG.this);
            // 2. Borrar del ICFG todos los arcos que aparecen en cSCRs
            if (callGraph.vertexSet().size() * callGraph.vertexSet().size() < callGraph.edgeSet().size())
                for (CallGraph.Edge<?> e : callGraph.edgeSet()) {
                    if (cSCRs.getRegion(callGraph.getEdgeSource(e)) != cSCRs.getRegion(callGraph.getEdgeTarget(e))) {
                        int callsDeleted = 0;
                        for (ControlFlowArc arc : callGraphEdge2ICFGArcMap.get(e)) {
                            if (simpleICFG.removeEdge(arc)) {
                                interprocNonRecArcs.add(new Triple<>(simpleICFG.getEdgeSource(arc), simpleICFG.getEdgeTarget(arc), arc));
                                callsDeleted++;
                            }
                        }
                        if (callsDeleted < 2)
                            throw new IllegalStateException("The creation of intraSCRs did not delete the minimum amount of call/return arcs");
                        if (callsDeleted % 2 == 1)
                            throw new IllegalStateException("The creation of intraSCRs missed a call or a return arc");

                    }
                }
            else
                for (DefaultEdge e : cSCRs.edgeSet()) {
                    int callsDeleted = 0;
                    for (CallGraph.Vertex src : cSCRs.getEdgeSource(e).vertexSet()) {
                        for (CallGraph.Vertex tgt : cSCRs.getEdgeTarget(e).vertexSet()) {
                            // 2a. Buscar el arco(s) correspondiente en el callgraph
                            for (CallGraph.Edge<?> callEdge : callGraph.getAllEdges(src, tgt)) {
                                // 2b. Buscar el arco(s) correspondientes en el simpleICFG y borrarlos
                                for (ControlFlowArc controlFlowArc : callGraphEdge2ICFGArcMap.get(callEdge)) {
                                    if (simpleICFG.removeEdge(controlFlowArc)) {
                                        interprocNonRecArcs.add(new Triple<>(simpleICFG.getEdgeSource(controlFlowArc), simpleICFG.getEdgeTarget(controlFlowArc), controlFlowArc));
                                        callsDeleted++;
                                    }
                                }
                            }
                        }
                    }
                    if (callsDeleted < 2)
                        throw new IllegalStateException("The creation of intraSCRs did not delete the minimum amount of call/return arcs");
                    if (callsDeleted % 2 == 1)
                        throw new IllegalStateException("The creation of intraSCRs missed a call or a return arc");
                }
            // 4. Generar los SCR del simpleICFG, para producir los intraSCRs
            intraSCRs = new IntraSCRGraph(simpleICFG);
            // buildCache algorithm
            buildCacheTransitive();
            // Only now can interprocedural edges be re-added to intraSCRs, cache building required
            addEdgesToIntraSCRs(interprocNonRecArcs);
        }

        protected void computeCallSCRs() {
            cSCRs = new CallSCRGraph(deleteDuplicatedEdges(callGraph));
        }

        public static <V, E> DefaultDirectedGraph<V, E> deleteDuplicatedEdges(Graph<V, E> baseGraph) {
            DefaultDirectedGraph<V, E> simpleGraph = new DefaultDirectedGraph<>(null, null, false);

            for (V v : baseGraph.vertexSet())
                simpleGraph.addVertex(v);
            for (E edge : baseGraph.edgeSet()) {
                V src = baseGraph.getEdgeSource(edge);
                V tgt = baseGraph.getEdgeTarget(edge);
                if (!simpleGraph.containsEdge(src, tgt))
                    simpleGraph.addEdge(src, tgt, edge);
            }

            return simpleGraph;
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
                    LinkedList<GraphNode<?>> callSequence = extractMovables(iterator, marker);
                    if (!extracted.isEmpty() && !ICFG.this.containsEdge(extracted.getLast(), callSequence.getFirst()))
                        addControlFlowArc(extracted.getLast(), callSequence.getFirst());
                    extracted.addAll(callSequence);
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
         * @return The non-empty list of nodes, inserted into the graph and connected to each other.
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
                        if (input) {
                            // Either we have only seen arguments or we have seen no elements (res is empty).
                            insertCallerNode(res, CallNode.create(callMarker.getCall()), callMarker);
                            // If we haven't seen a CallNode.Return node, the call is void: blank `return`.
                            insertCallerNode(res, CallNode.Return.create(callMarker.getCall()), callMarker);
                        }
                        assert !res.isEmpty();
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
                        addNonExecControlFlowArc(res.getLast(), movable.getRealNode());
                        res.add(movable.getRealNode());
                        continue;
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
                GraphNode<?> callNode = vertexSet().stream()
                        .filter(n -> n instanceof CallNode)
                        .filter(n -> ASTUtils.equalsWithRange(n.getAstNode(), call.getCall()))
                        .collect(toSingleton());
                GraphNode<?> returnNode = vertexSet().stream()
                        .filter(n -> n instanceof CallNode.Return)
                        .filter(n -> ASTUtils.equalsWithRange(n.getAstNode(), call.getCall()))
                        .collect(toSingleton());
                GraphNode<?> enterNode = cfgMap.get(callGraph.getEdgeTarget(call).getDeclaration()).getRootNode();
                GraphNode<?> exitNode = cfgMap.get(callGraph.getEdgeTarget(call).getDeclaration()).getExitNode();
                // Connections
                ControlFlowArc call2enter = addControlFlowArc(callNode, enterNode);
                ControlFlowArc exit2return = addControlFlowArc(exitNode, returnNode);
                callGraphEdge2ICFGArcMap.put(call, List.of(call2enter, exit2return));
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