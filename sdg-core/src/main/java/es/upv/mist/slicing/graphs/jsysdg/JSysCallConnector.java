package es.upv.mist.slicing.graphs.jsysdg;

import es.upv.mist.slicing.graphs.CallGraph;
import es.upv.mist.slicing.graphs.cfg.CFGBuilder;
import es.upv.mist.slicing.graphs.exceptionsensitive.ExceptionSensitiveCallConnector;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.VariableAction;
import es.upv.mist.slicing.nodes.io.ActualIONode;
import es.upv.mist.slicing.nodes.io.CallNode;
import es.upv.mist.slicing.nodes.io.FormalIONode;
import es.upv.mist.slicing.nodes.io.OutputNode;
import es.upv.mist.slicing.utils.ASTUtils;

import java.util.List;

public class JSysCallConnector extends ExceptionSensitiveCallConnector {
    public JSysCallConnector(JSysDG sdg) {
        super(sdg);
    }

    @Override
    public void connectAllCalls(CallGraph callGraph) {
        super.connectAllCalls(callGraph);
    }

    @Override
    protected void createActualInConnection(ActualIONode actualIn, FormalIONode formalIn) {
        super.createActualInConnection(actualIn, formalIn);
        if (formalIsObject(formalIn))
            connectObjectInterprocedurally(actualIn, formalIn);
    }

    @Override
    protected void createActualOutConnection(FormalIONode formalOut, ActualIONode actualOut) {
        super.createActualOutConnection(formalOut, actualOut);
        if (formalIsObject(formalOut))
            connectObjectInterprocedurally(formalOut, actualOut);
    }

    protected boolean formalIsObject(FormalIONode formalNode) {
        return formalNode.getVariableName().equals("this")
                || !formalNode.getAstNode().getParameterByName(formalNode.getVariableName())
                .orElseThrow().getType().isPrimitiveType();
    }

    protected void connectObjectInterprocedurally(GraphNode<?> source, GraphNode<?> target) {
        assert !target.getVariableActions().isEmpty();
        assert !source.getVariableActions().isEmpty();
        for (VariableAction targetVar : target.getVariableActions())
            source.getLastVariableAction().applySDGTreeConnection((JSysDG) sdg, targetVar);
    }

    @Override
    protected void createOutputReturnConnection(OutputNode<?> outputNode, CallNode.Return callReturnNode) {
        if (ASTUtils.declarationReturnIsObject(outputNode.getAstNode()))
            connectObjectOutput(outputNode, callReturnNode);
        else
            super.createOutputReturnConnection(outputNode, callReturnNode);
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
