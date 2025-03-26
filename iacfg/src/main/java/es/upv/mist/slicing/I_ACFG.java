package es.upv.mist.slicing;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.utils.CodeGenerationUtils;
import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.arcs.cfg.ControlFlowArc;
import es.upv.mist.slicing.cli.DOTAttributes;
import es.upv.mist.slicing.cli.GraphLog;
import es.upv.mist.slicing.graphs.CallGraph;
import es.upv.mist.slicing.graphs.ClassGraph;
import es.upv.mist.slicing.graphs.Graph;
import es.upv.mist.slicing.graphs.augmented.ACFG;
import es.upv.mist.slicing.graphs.cfg.CFG;
import es.upv.mist.slicing.graphs.sdg.InterproceduralDefinitionFinder;
import es.upv.mist.slicing.graphs.sdg.InterproceduralUsageFinder;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.SyntheticNode;
import es.upv.mist.slicing.nodes.VariableAction;
import es.upv.mist.slicing.nodes.io.CallNode;
import es.upv.mist.slicing.nodes.io.MethodExitNode;
import es.upv.mist.slicing.utils.ASTUtils;
import es.upv.mist.slicing.utils.StaticTypeSolver;
import org.apache.commons.cli.ParseException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static es.upv.mist.slicing.util.SingletonCollector.toSingleton;

public class I_ACFG extends Graph {

    protected static final Map<CallableDeclaration<?>, CFG> acfgMap = ASTUtils.newIdentityHashMap();
    protected CallGraph callGraph;

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
        joinACFGs();
        new GraphLog<>(this){
            @Override
            protected DOTAttributes edgeAttributes(Arc arc) {
                DOTAttributes att = super.edgeAttributes(arc);
                if (arc.isNonExecutableControlFlowArc())
                    att.add("style", "dashed");
                return att;
            }
        }.generateImages("migrafo");
        System.out.println("Grafo generado...");
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
            acfg.vertexSet().forEach(this::addVertex);
            acfg.edgeSet().forEach(arc -> addEdge(acfg.getEdgeSource(arc), acfg.getEdgeTarget(arc), arc));
        }
    }

    protected void expandCalls() {
        for (GraphNode<?> graphNode : Set.copyOf(vertexSet())) {
            GraphNode<?> lastMovableNode = null;
            Deque<GraphNode<?>> firstNodeStack = new LinkedList<>();
            boolean firstMovable = true;
            Deque<CallNode> callNodeStack = new LinkedList<>();
            VariableAction lastAction = graphNode.getVariableActions().isEmpty() ? null : graphNode.getLastVariableAction();
            VariableAction firstAction = graphNode.getVariableActions().isEmpty() ? null : graphNode.getVariableActions().get(0);
            Set<GraphNode<?>> movableNodes = new HashSet<>();
            for (VariableAction action : List.copyOf(graphNode.getVariableActions())) {
                if (action instanceof VariableAction.CallMarker) {
                    VariableAction.CallMarker variableAction = (VariableAction.CallMarker) action;
                    // Compute the call node, if entering the marker. Additionally, it places the node
                    // in the graph and makes it control-dependent on its container.
                    if (!variableAction.isEnter()) {
                        CallNode callNode = callNodeStack.peek();
                        if (!containsVertex(callNode)) {
                            addVertex(callNode);
                            if (lastMovableNode != null)
                                addControlFlowArc(lastMovableNode, callNode);
                            lastMovableNode = callNode;
                        }
                        callNodeStack.pop();
                    } else {
                        CallNode callNode = CallNode.create(variableAction.getCall());
                        if (graphNode.isImplicitInstruction())
                            callNode.markAsImplicit();
                        callNodeStack.push(callNode);
                    }
                } else if (action instanceof VariableAction.Movable) {
                    if(!isEnter(graphNode) && !isExit(graphNode)) {
                        movableNodes.add(graphNode);
                    }
                    // Move the variable to its own node, add that node to the graph and connect it.
                    var movable = (VariableAction.Movable) action;
                    movable.move(this);
                    SyntheticNode<?> realNode = movable.getRealNode();
                    if (realNode instanceof CallNode.Return) {
                        CallNode callNode = callNodeStack.peek();
                        addVertex(callNode);
                        addControlFlowArc(lastMovableNode, callNode);
                        if(movableNodes.contains(graphNode)) {
                            addControlFlowArc(realNode, graphNode);
                            movableNodes.remove(graphNode);
                        }
                    } else if(isExit(graphNode) && firstMovable) {
                        Set.copyOf(this.incomingEdgesOf(graphNode).stream()
                                .filter(Arc::isExecutableControlFlowArc)
                                .collect(Collectors.toSet()))
                                .forEach(arc -> {
                                    GraphNode<?> source = getEdgeSource(arc);
                                    removeEdge(arc);
                                    addEdge(source, realNode, arc);
                                });
                        addControlFlowArc(realNode, graphNode);
                    } else {
                        if(firstAction.equals(action) && isEnter(graphNode)){
                            firstNodeStack.add(movable.getRealNode());
                        }
                        if(lastAction != null && lastAction.equals(action)){
                            Set.copyOf(this.outgoingEdgesOf(graphNode).stream()
                                            .filter(Arc::isExecutableControlFlowArc)
                                            .collect(Collectors.toSet()))
                                    .forEach(arc -> {
                                        GraphNode<?> target = getEdgeTarget(arc);
                                        removeEdge(arc);
                                        addEdge(realNode, target, arc);
                                    });

                            // obtener primer nodo
                            if(!firstNodeStack.isEmpty()){
                                addControlFlowArc(graphNode, firstNodeStack.pop());
                            }
                        }

                        if (movableNodes.contains(graphNode)) {
                            Set.copyOf(this.incomingEdgesOf(graphNode).stream()
                                            .filter(Arc::isExecutableControlFlowArc)
                                            .collect(Collectors.toSet()))
                                    .forEach(arc -> {
                                        GraphNode<?> source = getEdgeSource(arc);
                                        removeEdge(arc);
                                        addEdge(source, realNode, arc);
                                    });
                        }

                        if(!firstMovable) {
                            connectRealNode(graphNode, lastMovableNode, realNode);
                        }
                    }
                    firstMovable = false;
                    lastMovableNode = realNode;
                }
            }
            assert callNodeStack.isEmpty();
        }
    }


    protected void joinACFGs() {
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
            GraphNode<?> enterNode = acfgMap.get(callGraph.getEdgeTarget(call).getDeclaration()).getRootNode();
            GraphNode<?> exitNode  = acfgMap.get(callGraph.getEdgeTarget(call).getDeclaration()).getExitNode();
            // Connections
            addControlFlowArc(callNode, enterNode);
            addControlFlowArc(exitNode, returnNode);
            // TODO: move this arc creation to expandCalls
            addEdge(callNode, returnNode, new ControlFlowArc.NonExecutable());
        }
    }

    private static boolean isExit(GraphNode<?> graphNode) {
        return graphNode instanceof MethodExitNode;
    }

    private boolean isEnter(GraphNode<?> graphNode) {
        return this.incomingEdgesOf(graphNode).isEmpty();
    }

    public void addControlFlowArc(GraphNode<?> from, GraphNode<?> to) {
        this.addEdge(from, to, new ControlFlowArc());
    }

    /** Connects the real node to the proper parent, control-dependent-wise. */
    protected void connectRealNode(GraphNode<?> graphNode, GraphNode<?> lastNode, GraphNode<?> realNode) {
        if(lastNode != null && !lastNode.equals(realNode)) {
            addControlFlowArc(lastNode, realNode);
        }
    }
}