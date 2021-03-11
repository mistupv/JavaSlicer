package es.upv.mist.slicing.graphs.sdg;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import es.upv.mist.slicing.arcs.sdg.CallArc;
import es.upv.mist.slicing.arcs.sdg.ParameterInOutArc;
import es.upv.mist.slicing.graphs.CallGraph;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.io.ActualIONode;
import es.upv.mist.slicing.nodes.io.CallNode;
import es.upv.mist.slicing.nodes.io.FormalIONode;
import es.upv.mist.slicing.nodes.io.OutputNode;
import es.upv.mist.slicing.utils.Logger;

/** Adds interprocedural arcs between the 'PDG components' of an SDG.
 * Arcs generated include {@link ParameterInOutArc parameter input/output} and
 * {@link CallArc call} arcs. */
public class CallConnector {
    protected final SDG sdg;

    public CallConnector(SDG sdg) {
        this.sdg = sdg;
    }

    /** Connects all calls found in the given call graph, placing the arcs in the SDG. */
    public void connectAllCalls(CallGraph callGraph) {
        sdg.vertexSet().stream()
                .filter(CallNode.class::isInstance)
                .map(CallNode.class::cast)
                .forEach(node -> connectCall(node, callGraph));
    }

    /** Connects a given call to all possible matching declarations. */
    @SuppressWarnings("unchecked")
    protected void connectCall(CallNode callNode, CallGraph callGraph) {
        var callExpr = (Resolvable<? extends ResolvedMethodLikeDeclaration>) callNode.getAstNode();
        callGraph.getCallTargets(callExpr)
                .map(sdg::findNodeByASTNode)
                .filter(opt -> {
                    if (opt.isEmpty())
                        Logger.format("Method declaration not found: '%s'. Discarding", callExpr);
                    return opt.isPresent();
                })
                .map(opt -> opt.orElseThrow(IllegalArgumentException::new))
                .forEach(node -> connectCall(callNode, node));
    }

    /** Connects a given call to its declaration, via call and in/out arcs. */
    protected void connectCall(CallNode callNode, GraphNode<? extends CallableDeclaration<?>> declarationNode) {
        // Connect the call and declaration nodes
        sdg.addCallArc(callNode, declarationNode);

        // Locate and connect all ACTUAL nodes
        sdg.outgoingEdgesOf(callNode).stream()
                .map(sdg::getEdgeTarget)
                .filter(ActualIONode.class::isInstance)
                .map(ActualIONode.class::cast)
                .forEach(actualNode -> {
                    if (actualNode.isInput())
                        connectActualIn(declarationNode, actualNode);
                    else if (actualNode.isOutput())
                        connectActualOut(declarationNode, actualNode);
                });

        // Locate and connect the -output- node
        sdg.outgoingEdgesOf(callNode).stream()
                .map(sdg::getEdgeTarget)
                .filter(CallNode.Return.class::isInstance)
                .map(CallNode.Return.class::cast)
                .forEach(n -> connectOutput(declarationNode, n));
    }

    /** Connects an actual-in node to its formal-in counterpart. */
    protected void connectActualIn(GraphNode<? extends CallableDeclaration<?>> declaration, ActualIONode actualIn) {
        sdg.outgoingEdgesOf(declaration).stream()
                .map(sdg::getEdgeTarget)
                .filter(FormalIONode.class::isInstance)
                .map(FormalIONode.class::cast)
                .filter(actualIn::matchesFormalIO)
                .forEach(formalIn -> createActualInConnection(actualIn, formalIn));
    }

    protected void createActualInConnection(ActualIONode actualIn, FormalIONode formalIn) {
        sdg.addParameterInOutArc(actualIn, formalIn);
    }

    /** Connects an actual-out node to its formal-out counterpart. Arc in reverse direction. */
    protected void connectActualOut(GraphNode<? extends CallableDeclaration<?>> declaration, ActualIONode actualOut) {
        sdg.outgoingEdgesOf(declaration).stream()
                .map(sdg::getEdgeTarget)
                .filter(FormalIONode.class::isInstance)
                .map(FormalIONode.class::cast)
                .filter(actualOut::matchesFormalIO)
                .forEach(formalOut -> createActualOutConnection(formalOut, actualOut));
    }

    protected void createActualOutConnection(FormalIONode formalOut, ActualIONode actualOut) {
        sdg.addParameterInOutArc(formalOut, actualOut);
    }

    /** Connects a method call return node to its method output counterpart. Arc in reverse direction. */
    protected void connectOutput(GraphNode<? extends CallableDeclaration<?>> methodDeclaration, CallNode.Return callReturnNode) {
        sdg.outgoingEdgesOf(methodDeclaration).stream()
                .map(sdg::getEdgeTarget)
                .filter(OutputNode.class::isInstance)
                .map(OutputNode.class::cast)
                .forEach(n -> createOutputReturnConnection(n, callReturnNode));
    }

    protected void createOutputReturnConnection(OutputNode<?> outputNode, CallNode.Return callReturnNode) {
        sdg.addParameterInOutArc(outputNode, callReturnNode);
    }
}
