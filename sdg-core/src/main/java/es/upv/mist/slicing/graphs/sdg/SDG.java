package es.upv.mist.slicing.graphs.sdg;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.LambdaExpr;
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
import es.upv.mist.slicing.graphs.pdg.PDG;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.SyntheticNode;
import es.upv.mist.slicing.slicing.*;
import es.upv.mist.slicing.utils.ASTUtils;

import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
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
        Set<GraphNode<?>> slicingCriterionNodes;
        try {
            slicingCriterionNodes = slicingCriterion.findNode(this);
            assert !slicingCriterionNodes.isEmpty();
        } catch (NoSuchElementException e) {
            throw new IllegalArgumentException("Could not locate the slicing criterion " + slicingCriterion);
        }
        return createSlicingAlgorithm().traverse(slicingCriterionNodes);
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

    /** @see CFG#isPredicate(GraphNode) */
    public boolean isPredicate(GraphNode<?> node) {
        if (node instanceof SyntheticNode)
            return false;
        for (CFG cfg : cfgMap.values())
            if (cfg.containsVertex(node))
                return cfg.isPredicate(node);
        throw new IllegalArgumentException("Node " + node.getId() + "'s associated CFG cannot be found!");
    }

    public void addCallArc(GraphNode<?> from, GraphNode<? extends CallableDeclaration<?>> to) {
        this.addEdge(from, to, new CallArc());
    }

    public void addParameterInOutArc(GraphNode<?> from, GraphNode<?> to) {
        this.addEdge(from, to, new ParameterInOutArc());
    }

    public void addSummaryArc(SyntheticNode<?> from, SyntheticNode<?> to) {
        this.addEdge(from, to, new SummaryArc());
    }

    /** Populates this SDG by building the corresponding CFGs, call graph, performing data flow analyses,
     *  building the PDGs, connecting the calls to declarations and computing the summary arcs.
     *  By default, it uses {@link PDG}s and {@link CFG}s. */
    public class Builder {
        protected CallGraph callGraph;

        public void build(NodeList<CompilationUnit> nodeList) {
            // See creation strategy at http://kaz2.dsic.upv.es:3000/Fzg46cQvT1GzHQG9hFnP1g#Using-data-flow-in-the-SDG
            // This ordering cannot be altered, as each step requires elements from the previous one.
            createClassGraph(nodeList); // 0
            buildCFGs(nodeList);        // 1
            createCallGraph(nodeList);  // 2
            dataFlowAnalysis();         // 3
            buildAndCopyPDGs();         // 4
            connectCalls();             // 5
            createSummaryArcs();        // 6
        }

        /** Build a CFG per declaration found in the list of compilation units. */
        protected void buildCFGs(NodeList<CompilationUnit> nodeList) {
            nodeList.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(MethodDeclaration n, Void arg) {
                    CFG cfg = createCFG();
                    buildCFG(n, cfg);
                    cfgMap.put(n, cfg);
                    super.visit(n, arg);
                }

                @Override
                public void visit(ConstructorDeclaration n, Void arg) {
                    CFG cfg = createCFG();
                    buildCFG(n, cfg);
                    cfgMap.put(n, cfg);
                    super.visit(n, arg);
                }

                @Override
                public void visit(LambdaExpr n, Void arg) {
                    super.visit(n, arg);
                }
            }, null);
        }

        /** Given a single empty CFG and a declaration, build the CFG. */
        protected void buildCFG(CallableDeclaration<?> declaration, CFG cfg) {
            cfg.build(declaration);
        }

        /** Create call graph from the list of compilation units. */
        protected void createCallGraph(NodeList<CompilationUnit> nodeList) {
            callGraph = new CallGraph(cfgMap, ClassGraph.getInstance());
            callGraph.build(nodeList);
        }

        /** Create class graph from the list of compilation units. */
        protected void createClassGraph(NodeList<CompilationUnit> nodeList){
            ClassGraph.getNewInstance().build(nodeList);
        }


        /** Perform interprocedural analyses to determine the actual and formal nodes. */
        protected void dataFlowAnalysis() {
            new InterproceduralDefinitionFinder(callGraph, cfgMap).save(); // 3.1
            new InterproceduralUsageFinder(callGraph, cfgMap).save();      // 3.2
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
        protected void connectCalls() {
            new CallConnector(SDG.this).connectAllCalls(callGraph);
        }

        /** Connect actual-in to actual-out nodes, summarizing the interprocedural arcs. */
        protected void createSummaryArcs() {
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
