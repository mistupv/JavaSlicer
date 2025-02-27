package es.upv.mist.slicing;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.utils.CodeGenerationUtils;
import es.upv.mist.slicing.arcs.pdg.ControlDependencyArc;
import es.upv.mist.slicing.cli.GraphLog;
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
import es.upv.mist.slicing.utils.ASTUtils;
import es.upv.mist.slicing.utils.StaticTypeSolver;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class I_ACFG extends Graph {

    protected static final Map<CallableDeclaration<?>, CFG> acfgMap = ASTUtils.newIdentityHashMap();
    protected CallGraph callGraph;
    protected GraphNode<?> secuentialNode = null;

    public static void main(String[] args) throws ParseException, IOException {
        String ruta = "/src/main/java/es/upv/mist/slicing/tests/";
        String fichero = "Test.java";

        I_ACFG iAcfg = new I_ACFG();
        iAcfg.generarACFG(ruta, fichero);
    }

    public void generarACFG(String ruta, String fichero) throws IOException {
        NodeList<CompilationUnit> units = new NodeList<>();
        File file = new File(CodeGenerationUtils.mavenModuleRoot(I_ACFG.class)+ruta+fichero);

        StaticJavaParser.getConfiguration().setAttributeComments(false);
        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.INFO, "Configuring JavaParser");
        StaticTypeSolver.addTypeSolverJRE();

        try {
            units.add(StaticJavaParser.parse(file));
        } catch (FileNotFoundException e) {
            System.out.println("No se encontró el archivo");
        }

        createClassGraph(units);
        buildACFGs(units);
        createCallGraph(units);
        dataFlowAnalysis();
        copyACFGs();
        expandCalls();
        new GraphLog<>(this){}.generateImages("migrafo");
        /* acfgMap.forEach((declaration, cfg)-> {
            CFGLog cfgLog = new CFGLog(cfg);
            try {
                cfgLog.generateImages(declaration.getNameAsString());
            } catch (IOException e) {
                System.out.println("ERROR");
            }
        }); */
        System.out.println();
    }

    protected void buildACFGs(NodeList<CompilationUnit> nodeList) {
        nodeList.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodDeclaration n, Void arg) {
                boolean isInInterface = n.findAncestor(ClassOrInterfaceDeclaration.class)
                        .map(ClassOrInterfaceDeclaration::isInterface).orElse(false);
                if (n.isAbstract() || isInInterface)
                    return; // Allow abstract methods
                ACFG acfg = new ACFG();
                acfg.build(n);
                acfgMap.put(n, acfg);
                super.visit(n, arg);
            }

            @Override
            public void visit(ConstructorDeclaration n, Void arg) {
                boolean isInInterface = n.findAncestor(ClassOrInterfaceDeclaration.class)
                        .map(ClassOrInterfaceDeclaration::isInterface).orElse(false);
                if (n.isAbstract() || isInInterface)
                    return; // Allow abstract methods
                ACFG acfg = new ACFG();
                acfg.build(n);
                acfgMap.put(n, acfg);
                super.visit(n, arg);
            }
        }, null);
    }

    /** Create class graph from the list of compilation units. */
    protected void createClassGraph(NodeList<CompilationUnit> nodeList){
        ClassGraph.getNewInstance().build(nodeList);
    }

    /** Create call graph from the list of compilation units. */
    protected void createCallGraph(NodeList<CompilationUnit> nodeList) {
        callGraph = new CallGraph(acfgMap, ClassGraph.getInstance());
        callGraph.build(nodeList);
    }

    /** Perform interprocedural analyses to determine the actual and formal nodes. */
    protected void dataFlowAnalysis() {
        new InterproceduralDefinitionFinder(callGraph, acfgMap).save(); // 3.1
        new InterproceduralUsageFinder(callGraph, acfgMap).save();      // 3.2
    }

    /** Build a PDG per declaration, based on the CFGs built previously and enhanced by data analyses. */
    protected void copyACFGs() {
        for (CFG acfg : acfgMap.values()) {
            // for debugging
            // APDG pdg = new APDG((ACFG) acfg);
            // pdg.build(acfg.getDeclaration());

            acfg.vertexSet().forEach(this::addVertex);
            acfg.edgeSet().forEach(arc -> addEdge(acfg.getEdgeSource(arc), acfg.getEdgeTarget(arc), arc));
        }
    }

    protected void expandCalls() {
        // debug statement to print graph
        // new PDGLog(PDG.this).generateImages("pdg-debug")
        for (GraphNode<?> graphNode : Set.copyOf(vertexSet())) {
            secuentialNode = null;
            CallNode endCallNode = null;
            Deque<CallNode> callNodeStack = new LinkedList<>();
            for (VariableAction action : List.copyOf(graphNode.getVariableActions())) {
                if (action instanceof VariableAction.CallMarker) {
                    VariableAction.CallMarker variableAction = (VariableAction.CallMarker) action;
                    // Compute the call node, if entering the marker. Additionally, it places the node
                    // in the graph and makes it control-dependent on its container.
                    if (!variableAction.isEnter()) {
                        callNodeStack.pop();
                    } else {
                        CallNode callNode = CallNode.create(variableAction.getCall());
                        endCallNode = CallNode.create(variableAction.getCall());
                        if (graphNode.isImplicitInstruction())
                            callNode.markAsImplicit();
                        addVertex(callNode);
                        addControlDependencyArc(graphNode, callNode);
                        callNodeStack.push(callNode);
                    }
                } else if (action instanceof VariableAction.Movable) {
                    // Move the variable to its own node, add that node to the graph and connect it.
                    var movable = (VariableAction.Movable) action;
                    movable.move(this);
                    connectRealNode(graphNode, callNodeStack.peek(), movable.getRealNode());
                }
            }
            if(endCallNode != null && secuentialNode != null) {
                addVertex(endCallNode);
                addControlDependencyArc(secuentialNode, endCallNode);
            }
            assert callNodeStack.isEmpty();
        }
    }

    public void addControlDependencyArc(GraphNode<?> from, GraphNode<?> to) {
        this.addEdge(from, to, new ControlDependencyArc());
    }

    /** Connects the real node to the proper parent, control-dependent-wise. */
    protected void connectRealNode(GraphNode<?> graphNode, CallNode callNode, GraphNode<?> realNode) {
        if (realNode instanceof ActualIONode || realNode instanceof CallNode.Return) {
            assert callNode != null;
            if(secuentialNode != realNode) {
                addControlDependencyArc(secuentialNode == null ? callNode : secuentialNode, realNode);
                secuentialNode = realNode;
            }
        } else {
            addControlDependencyArc(graphNode, realNode);
        }
    }
}