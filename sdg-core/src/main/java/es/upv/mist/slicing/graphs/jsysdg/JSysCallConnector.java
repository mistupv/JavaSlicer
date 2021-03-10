package es.upv.mist.slicing.graphs.jsysdg;

import com.github.javaparser.ast.body.CallableDeclaration;
import es.upv.mist.slicing.graphs.CallGraph;
import es.upv.mist.slicing.graphs.cfg.CFGBuilder;
import es.upv.mist.slicing.graphs.exceptionsensitive.ExceptionSensitiveCallConnector;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.VariableAction;
import es.upv.mist.slicing.nodes.io.ActualIONode;
import es.upv.mist.slicing.nodes.io.FormalIONode;
import es.upv.mist.slicing.nodes.io.OutputNode;
import es.upv.mist.slicing.utils.ASTUtils;

import java.util.List;
import java.util.function.Consumer;

public class JSysCallConnector extends ExceptionSensitiveCallConnector {
    public JSysCallConnector(JSysDG sdg) {
        super(sdg);
    }

    @Override
    public void connectAllCalls(CallGraph callGraph) {
        super.connectAllCalls(callGraph);
    }

    @Override
    protected void connectActualIn(GraphNode<? extends CallableDeclaration<?>> declaration, ActualIONode actualIn) {
        sdg.outgoingEdgesOf(declaration).stream()
                .map(sdg::getEdgeTarget)
                .filter(FormalIONode.class::isInstance)
                .map(FormalIONode.class::cast)
                .filter(actualIn::matchesFormalIO)
                .forEach(formalIn -> {
                    boolean primitive = !formalIn.getVariableName().equals("this")
                            && declaration.getAstNode().getParameterByName(formalIn.getVariableName())
                                    .orElseThrow().getType().isPrimitiveType();
                    if (primitive)
                        sdg.addParameterInOutArc(actualIn, formalIn);
                    else
                        connectObjectActualIn(actualIn, formalIn);
                });
    }

    protected void connectObjectActualIn(GraphNode<?> actualIn, GraphNode<?> formalIn) {
        List<VariableAction> actualList = actualIn.getVariableActions();
        List<VariableAction> formalList = formalIn.getVariableActions();
        assert formalList.size() == 1;
        VariableAction actualVar = actualList.get(actualList.size() - 1);
        VariableAction formalVar = formalList.get(0);
        actualVar.applySDGTreeConnection((JSysDG) sdg, formalVar);
    }

    @Override
    protected void connectOutput(GraphNode<? extends CallableDeclaration<?>> methodDeclaration, GraphNode<?> callReturnNode) {
        Consumer<GraphNode<?>> action;
        if (ASTUtils.declarationReturnIsObject(methodDeclaration.getAstNode()))
            action = node -> connectObjectOutput(node, callReturnNode);
        else
            action = node -> sdg.addParameterInOutArc(node, callReturnNode);
        sdg.outgoingEdgesOf(methodDeclaration).stream()
                .map(sdg::getEdgeTarget)
                .filter(OutputNode.class::isInstance)
                .forEach(action);
    }

    protected void connectObjectOutput(GraphNode<?> methodOutputNode, GraphNode<?> callReturnNode) {
        List<VariableAction> outputList = methodOutputNode.getVariableActions();
        assert outputList.size() == 1;
        assert outputList.get(0).isUsage() && outputList.get(0).getName().equals(CFGBuilder.VARIABLE_NAME_OUTPUT);
        List<VariableAction> returnList = callReturnNode.getVariableActions();
        assert returnList.size() == 1;
        assert returnList.get(0).isDefinition() && returnList.get(0).getName().equals(CFGBuilder.VARIABLE_NAME_OUTPUT);
        VariableAction source = outputList.get(0);
        VariableAction target = returnList.get(0);
        source.applySDGTreeConnection((JSysDG) sdg, target);
    }
}
