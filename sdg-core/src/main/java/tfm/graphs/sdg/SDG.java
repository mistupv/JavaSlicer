package tfm.graphs.sdg;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.arcs.sdg.CallArc;
import tfm.arcs.sdg.ParameterInOutArc;
import tfm.arcs.sdg.SummaryArc;
import tfm.graphs.Buildable;
import tfm.graphs.CallGraph;
import tfm.graphs.Graph;
import tfm.graphs.cfg.CFG;
import tfm.graphs.cfg.CFGBuilder;
import tfm.graphs.pdg.PDG;
import tfm.nodes.GraphNode;
import tfm.nodes.SyntheticNode;
import tfm.nodes.VariableAction;
import tfm.nodes.io.ActualIONode;
import tfm.nodes.io.CallNode;
import tfm.slicing.*;
import tfm.utils.ASTUtils;

import java.util.*;

/**
 * The <b>System Dependence Graph</b> represents the statements of a program in
 * a graph, connecting statements according to their {@link tfm.arcs.pdg.ControlDependencyArc control},
 * {@link tfm.arcs.pdg.DataDependencyArc data} and {@link tfm.arcs.sdg.InterproceduralArc interprocedural}
 * relationships. You can build one manually or use the {@link Builder SDGBuilder}.
 * The variations of the SDG are represented as child types.
 * <ol>
 *      <li>Build a graph: {@link #build(NodeList)}</li>
 *      <li>Slice a graph: {@link #slice(SlicingCriterion)}</li>
 *      <li>Obtain the sliced Java: {@link Slice#toAst()}</li>
 * </ol>
 */
public class SDG extends Graph implements Sliceable, Buildable<NodeList<CompilationUnit>> {
    protected final Map<CallableDeclaration<?>, CFG> cfgMap = new IdentityHashMap<>();

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
            buildCFGs(nodeList);                             // 1
            CallGraph callGraph = createCallGraph(nodeList); // 2
            dataFlowAnalysis(callGraph);                     // 3
            buildAndCopyPDGs();                              // 4
            connectCalls(callGraph);                         // 5
            createSummaryArcs(callGraph);                    // 6
        }

        /** Build a CFG per declaration found in the list of compilation units. */
        protected void buildCFGs(NodeList<CompilationUnit> nodeList) {
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
