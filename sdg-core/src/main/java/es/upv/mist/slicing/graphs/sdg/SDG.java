package es.upv.mist.slicing.graphs.sdg;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import es.upv.mist.slicing.arcs.pdg.ControlDependencyArc;
import es.upv.mist.slicing.arcs.pdg.DataDependencyArc;
import es.upv.mist.slicing.arcs.sdg.CallArc;
import es.upv.mist.slicing.arcs.sdg.InterproceduralArc;
import es.upv.mist.slicing.arcs.sdg.ParameterInOutArc;
import es.upv.mist.slicing.arcs.sdg.SummaryArc;
import es.upv.mist.slicing.graphs.Buildable;
import es.upv.mist.slicing.graphs.CallGraph;
import es.upv.mist.slicing.graphs.ClassGraph;
import es.upv.mist.slicing.graphs.Graph;
import es.upv.mist.slicing.graphs.cfg.CFG;
import es.upv.mist.slicing.graphs.cfg.CFGBuilder;
import es.upv.mist.slicing.graphs.pdg.PDG;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.SyntheticNode;
import es.upv.mist.slicing.nodes.VariableAction;
import es.upv.mist.slicing.nodes.io.ActualIONode;
import es.upv.mist.slicing.nodes.io.CallNode;
import es.upv.mist.slicing.slicing.*;
import es.upv.mist.slicing.utils.ASTUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The <b>System Dependence Graph</b> represents the statements of a program in
 * a graph, connecting statements according to their {@link ControlDependencyArc control},
 * {@link DataDependencyArc data} and {@link InterproceduralArc interprocedural}
 * relationships. You can build one manually or use the {@link Builder SDGBuilder}.
 * The variations of the SDG are represented as child types.
 * <ol>
 *      <li>Build a graph: {@link #build(NodeList)}</li>
 *      <li>Slice a graph: {@link #slice(SlicingCriterion)}</li>
 *      <li>Obtain the sliced Java: {@link Slice#toAst()}</li>
 * </ol>
 */
public class SDG extends Graph implements Sliceable, Buildable<NodeList<CompilationUnit>> {
    protected final Map<CallableDeclaration<?>, CFG> cfgMap = ASTUtils.newIdentityHashMap();

    protected boolean built = false;
    protected NodeList<CompilationUnit> compilationUnits;

    /** Obtain the list of compilation units used to create this graph. */
    public NodeList<CompilationUnit> getCompilationUnits() {
        return compilationUnits;
    }

    @Override
    public Slice slice(SlicingCriterion slicingCriterion) {
        Optional<GraphNode<?>> optSlicingNode = slicingCriterion.findNode(this);
        if (optSlicingNode.isEmpty())
            throw new IllegalArgumentException("Could not locate the slicing criterion in the SDG");
        return createSlicingAlgorithm().traverse(optSlicingNode.get());
    }

    protected SlicingAlgorithm createSlicingAlgorithm() {
        return new ClassicSlicingAlgorithm(this);
    }

    @Override
    public void build(NodeList<CompilationUnit> nodeList) {
        createBuilder().build(nodeList);
        compilationUnits = nodeList;
        built = true;
    }

    /** Create a new SDG builder. Child classes that wish to alter the creation of the graph
     * should create a new SDG builder and override this method. */
    protected Builder createBuilder() {
        return new Builder();
    }

    @Override
    public boolean isBuilt() {
        return built;
    }

    /** Obtain the CFGs that were generated in the process of creating this graph. */
    public Collection<CFG> getCFGs() {
        return cfgMap.values();
    }

    public void addCallArc(GraphNode<?> from, GraphNode<? extends CallableDeclaration<?>> to) {
        this.addEdge(from, to, new CallArc());
    }

    public void addParameterInOutArc(GraphNode<?> from, GraphNode<?> to) {
        this.addEdge(from, to, new ParameterInOutArc());
    }

    public void addSummaryArc(ActualIONode from, SyntheticNode<?> to) {
        this.addEdge(from, to, new SummaryArc());
    }

    /** Populates this SDG by building the corresponding CFGs, call graph, performing data flow analyses,
     *  building the PDGs, connecting the calls to declarations and computing the summary arcs.
     *  By default, it uses {@link PDG}s and {@link CFG}s. */
    public class Builder {
        public void build(NodeList<CompilationUnit> nodeList) {
            // See creation strategy at http://kaz2.dsic.upv.es:3000/Fzg46cQvT1GzHQG9hFnP1g#Using-data-flow-in-the-SDG
            ClassGraph classGraph = createClassGraph(nodeList); // TODO: Update order and creation strategy
            buildCFGs(nodeList, classGraph);                 // 1
            CallGraph callGraph = createCallGraph(nodeList); // 2
            dataFlowAnalysis(callGraph);                     // 3
            buildAndCopyPDGs();                              // 4
            connectCalls(callGraph);                         // 5
            createSummaryArcs(callGraph);                    // 6
        }

        /** Build a CFG per declaration found in the list of compilation units. */
        protected void buildCFGs(NodeList<CompilationUnit> nodeList, ClassGraph clg) {
            nodeList.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(MethodDeclaration n, Void arg) {
                    CFG cfg = createCFG();
                    cfg.build(n);
                    cfgMap.put(n, cfg);
                }

                @Override
                public void visit(ConstructorDeclaration n, Void arg) {
                    CFG cfg = createCFG();
                    cfg.build(n);
                    cfgMap.put(n, cfg);
                }
            }, null);
        }

        /** Create call graph from the list of compilation units. */
        protected CallGraph createCallGraph(NodeList<CompilationUnit> nodeList) {
            CallGraph callGraph = new CallGraph(cfgMap);
            callGraph.build(nodeList);
            return callGraph;
        }

        /** Create class graph from the list of compilation units. */
        protected ClassGraph createClassGraph(NodeList<CompilationUnit> nodeList){
            ClassGraph classGraph = new ClassGraph();
            classGraph.build(nodeList);
            return classGraph;
        }


        /** Perform interprocedural analyses to determine the actual, formal and call return nodes. */
        protected void dataFlowAnalysis(CallGraph callGraph) {
            new InterproceduralDefinitionFinder(callGraph, cfgMap).save(); // 3.1
            new InterproceduralUsageFinder(callGraph, cfgMap).save();      // 3.2
            insertCallOutput(callGraph);                                   // 3.3
        }

        /** Insert {@link CallNode.Return call return} nodes onto all appropriate calls. */
        protected void insertCallOutput(CallGraph callGraph) {
            for (CallGraph.Edge<?> edge : callGraph.edgeSet()) {
                if (ASTUtils.resolvableIsVoid(edge.getCall()))
                    continue;
                GraphNode<?> graphNode = edge.getGraphNode();
                // A node defines -output-
                var def = new VariableAction.Definition(new NameExpr(CFGBuilder.VARIABLE_NAME_OUTPUT), graphNode);
                var defMov = new VariableAction.Movable(def, CallNode.Return.create(edge.getCall()));
                graphNode.addActionsForCall(Set.of(defMov), edge.getCall(), false);
                // The container of the call uses -output-
                var use = new VariableAction.Usage(new NameExpr(CFGBuilder.VARIABLE_NAME_OUTPUT), graphNode);
                graphNode.addActionsAfterCall(Set.of(use), edge.getCall());
            }
        }

        /** Build a PDG per declaration, based on the CFGs built previously and enhanced by data analyses. */
        protected void buildAndCopyPDGs() {
            for (CFG cfg : cfgMap.values()) {
                // 4.1, 4.2, 4.3
                PDG pdg = createPDG(cfg);
                pdg.build(cfg.getDeclaration());
                // 4.4
                pdg.vertexSet().forEach(SDG.this::addVertex);
                pdg.edgeSet().forEach(arc -> addEdge(pdg.getEdgeSource(arc), pdg.getEdgeTarget(arc), arc));
            }
        }

        /** Add interprocedural arcs, connecting calls, their arguments and results to their corresponding declarations. */
        protected void connectCalls(CallGraph callGraph) {
            new CallConnector(SDG.this).connectAllCalls(callGraph);
        }

        /** Connect actual-in to actual-out nodes, summarizing the interprocedural arcs. */
        protected void createSummaryArcs(CallGraph callGraph) {
            new SummaryArcAnalyzer(SDG.this, callGraph).analyze();
        }

        /** Create a new CFG, of the appropriate type for the kind of SDG we're building. */
        protected CFG createCFG() {
            return new CFG();
        }

        /** Create a new PDG, of the appropriate type for the kind of SDG we're building. */
        protected PDG createPDG(CFG cfg) {
            return new PDG(cfg);
        }
    }
}
