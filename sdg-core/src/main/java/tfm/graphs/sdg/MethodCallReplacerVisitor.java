package tfm.graphs.sdg;

import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import tfm.arcs.Arc;
import tfm.arcs.pdg.DataDependencyArc;
import tfm.graphs.cfg.CFGBuilder;
import tfm.nodes.*;
import tfm.nodes.type.NodeType;
import tfm.utils.Context;
import tfm.utils.Logger;

import java.util.*;
import java.util.stream.Collectors;

public class MethodCallReplacerVisitor extends VoidVisitorAdapter<Context> {

    protected final SDG sdg;
    /** The current location, as a stack of statements (e.g. do -> if -> exprStmt). */
    protected Deque<GraphNode<?>> stmtStack = new LinkedList<>();

    public MethodCallReplacerVisitor(SDG sdg) {
        this.sdg = sdg;
    }

    @Override
    public void visit(DoStmt n, Context arg) {
        stmtStack.push(sdg.findNodeByASTNode(n).orElseThrow());
        super.visit(n, arg);
        stmtStack.pop();
    }

    @Override
    public void visit(ForEachStmt n, Context arg) {
        stmtStack.push(sdg.findNodeByASTNode(n).orElseThrow());
        super.visit(n, arg);
        stmtStack.pop();
    }

    @Override
    public void visit(ForStmt n, Context arg) {
        stmtStack.push(sdg.findNodeByASTNode(n).orElseThrow());
        super.visit(n, arg);
        stmtStack.pop();
    }

    @Override
    public void visit(IfStmt n, Context arg) {
        stmtStack.push(sdg.findNodeByASTNode(n).orElseThrow());
        super.visit(n, arg);
        stmtStack.pop();
    }

    @Override
    public void visit(SwitchStmt n, Context arg) {
        stmtStack.push(sdg.findNodeByASTNode(n).orElseThrow());
        super.visit(n, arg);
        stmtStack.pop();
    }

    @Override
    public void visit(WhileStmt n, Context arg) {
        stmtStack.push(sdg.findNodeByASTNode(n).orElseThrow());
        super.visit(n, arg);
        stmtStack.pop();
    }

    @Override
    public void visit(ReturnStmt n, Context arg) {
        stmtStack.push(sdg.findNodeByASTNode(n).orElseThrow());
        super.visit(n, arg);
        stmtStack.pop();
    }

    @Override
    public void visit(ExpressionStmt n, Context arg) {
        stmtStack.push(sdg.findNodeByASTNode(n).orElseThrow());
        super.visit(n, arg);
        stmtStack.pop();
    }

    @Override
    public void visit(MethodCallExpr methodCallExpr, Context context) {
        GraphNode<MethodDeclaration> methodDeclarationNode;
        try {
            methodDeclarationNode = methodCallExpr.resolve().toAst()
                    .flatMap(sdg::findNodeByASTNode)
                    .orElseThrow(() -> new UnsolvedSymbolException(""));
        } catch (UnsolvedSymbolException e) {
            Logger.format("Method declaration not found: '%s'. Discarding", methodCallExpr);
            return;
        }

        NodeList<Expression> arguments = methodCallExpr.getArguments();
        NodeList<Parameter> parameters = methodDeclarationNode.getAstNode().getParameters();

        // Create and connect the CALL node
        CallNode methodCallNode = new CallNode(methodCallExpr);
        sdg.addNode(methodCallNode);
        sdg.addControlDependencyArc(stmtStack.peek(), methodCallNode);
        sdg.addCallArc(methodCallNode, methodDeclarationNode);


        for (int i = 0; i < parameters.size(); i++) {
            Parameter parameter = parameters.get(i);
            Expression argument;
            if (!parameter.isVarArgs()) {
                argument = arguments.get(i);
            } else {
                NodeList<Expression> varArgs = new NodeList<>(arguments.subList(i, arguments.size()));
                argument = new ArrayCreationExpr(
                        parameter.getType(),
                        new NodeList<>(new ArrayCreationLevel(varArgs.size())),
                        new ArrayInitializerExpr(varArgs)
                );
                i = parameters.size();
            }

            createActualIn(methodDeclarationNode, methodCallNode, parameter, argument, context);
            createActualOut(methodDeclarationNode, methodCallNode, parameter, argument, context);
        }

        // Add the 'output' node to the call and connect to the METHOD_OUTPUT node (there should be only one -- if any)
        sdg.outgoingEdgesOf(methodDeclarationNode).stream()
                .filter(arc -> sdg.getEdgeTarget(arc).getNodeType() == NodeType.METHOD_OUTPUT)
                .map(sdg::getEdgeTarget)
                .forEach(node -> processMethodOutputNode(node, methodCallNode));
    }

    protected void createActualIn(GraphNode<MethodDeclaration> declaration, GraphNode<MethodCallExpr> call, Parameter parameter, Expression argument, Context context) {
        ActualIONode argumentInNode = ActualIONode.createActualIn(call.getAstNode(), parameter, argument);
        sdg.addNode(argumentInNode);
        sdg.addControlDependencyArc(call, argumentInNode);

        // Handle data dependency: Remove arc from method call node and add it to IN node
        List<DataDependencyArc> arcsToRemove = sdg.incomingEdgesOf(stmtStack.peek()).stream()
                .filter(Arc::isDataDependencyArc)
                .map(Arc::asDataDependencyArc)
                .filter(arc -> arc.getTarget().isContainedIn(argument))
                .collect(Collectors.toList());
        arcsToRemove.forEach(arc -> moveArc(arc, argumentInNode, true));

        // Now, find the corresponding method declaration's in node and link argument node with it
        Optional<FormalIONode> optionalParameterInNode = sdg.outgoingEdgesOf(declaration).stream()
                .map(sdg::getEdgeTarget)
                .filter(FormalIONode.class::isInstance)
                .map(FormalIONode.class::cast)
                .filter(argumentInNode::matchesFormalIO)
                .findFirst();

        if (optionalParameterInNode.isPresent()) {
            sdg.addParameterInOutArc(argumentInNode, optionalParameterInNode.get());
        } else {
            Logger.log(getClass().getSimpleName(), "WARNING: IN declaration node for argument " + argument + " not found.");
            Logger.log(getClass().getSimpleName(), String.format("Context: %s, Method: %s, Call: %s",
                    context.getCurrentMethod().orElseThrow().getNameAsString(),
                    declaration.getAstNode().getSignature().asString(),
                    call.getAstNode()));
        }
    }

    protected void createActualOut(GraphNode<MethodDeclaration> declaration, GraphNode<MethodCallExpr> call, Parameter parameter, Expression argument, Context context) {
        Set<String> variablesForOutNode = new HashSet<>();
        argument.accept(new OutNodeVariableVisitor(), variablesForOutNode);

        // Here, variablesForOutNode may have 1 variable or more depending on the expression
        if (variablesForOutNode.isEmpty()) {
            // If the argument is not a variable or it is not declared in the scope, then there is no OUT node
            Logger.log("MethodCallReplacerVisitor", String.format("Expression '%s' should not have out node", argument.toString()));
            return;
        } else if (variablesForOutNode.size() == 1) {
            String variable = variablesForOutNode.iterator().next();

            List<GraphNode<?>> declarations = sdg.findDeclarationsOfVariable(variable, stmtStack.peek());

            Logger.log("MethodCallReplacerVisitor", String.format("Declarations of variable: '%s': %s", variable, declarations));

            if (declarations.isEmpty()) {
                Logger.log("MethodCallReplacerVisitor", String.format("Expression '%s' should not have out node", argument.toString()));
                return;
            }
        } else {
            // Multiple variables (varargs, array) not considered!
            return;
        }

        ActualIONode argumentOutNode = ActualIONode.createActualOut(call.getAstNode(), parameter, argument);
        sdg.addNode(argumentOutNode);
        sdg.addControlDependencyArc(call, argumentOutNode);

        // Now, find the corresponding method call's out node and link argument node with it
        Optional<FormalIONode> optionalParameterOutNode = sdg.outgoingEdgesOf(declaration).stream()
                .map(sdg::getEdgeTarget)
                .filter(FormalIONode.class::isInstance)
                .map(FormalIONode.class::cast)
                .filter(argumentOutNode::matchesFormalIO)
                .findFirst();

        // Handle data dependency: copy arc from method call node and add it to OUT node
        List<DataDependencyArc> arcsToRemove = sdg.outgoingEdgesOf(stmtStack.peek()).stream()
                .filter(Arc::isDataDependencyArc)
                .map(DataDependencyArc.class::cast)
                .filter(arc -> arc.getSource().isContainedIn(argument))
                .collect(Collectors.toList());
        arcsToRemove.forEach(arc -> moveArc(arc, argumentOutNode, false));

        if (optionalParameterOutNode.isPresent()) {
            sdg.addParameterInOutArc(optionalParameterOutNode.get(), argumentOutNode);
        } else {
            Logger.log(getClass().getSimpleName(), "WARNING: OUT declaration node for argument " + argument + " not found.");
            Logger.log(getClass().getSimpleName(), String.format("Context: %s, Method: %s, Call: %s",
                    context.getCurrentMethod().orElseThrow().getNameAsString(),
                    declaration.getAstNode().getSignature().asString(),
                    call.getAstNode()));
        }
    }

    /**
     * @param moveTarget If true, the target will be changed to 'node', otherwise the source will be.
     */
    protected void moveArc(DataDependencyArc arc, ActualIONode node, boolean moveTarget) {
        VariableAction sourceAction = arc.getSource();
        VariableAction targetAction = arc.getTarget();
        if (moveTarget)
            sdg.addDataDependencyArc(sourceAction, targetAction.moveTo(node));
        else
            sdg.addDataDependencyArc(sourceAction.moveTo(node), targetAction);
        sdg.removeEdge(arc);
    }

    protected void processMethodOutputNode(GraphNode<?> methodOutputNode, GraphNode<MethodCallExpr> methodCallNode) {
        GraphNode<EmptyStmt> callReturnNode = sdg.addNode("call output", new EmptyStmt(),
                TypeNodeFactory.fromType(NodeType.METHOD_CALL_RETURN));
        VariableAction.Usage usage = stmtStack.peek().addUsedVariable(new NameExpr(CFGBuilder.VARIABLE_NAME_OUTPUT));
        VariableAction.Definition definition = callReturnNode.addDefinedVariable(new NameExpr(CFGBuilder.VARIABLE_NAME_OUTPUT));

        sdg.addControlDependencyArc(methodCallNode, callReturnNode);
        sdg.addDataDependencyArc(definition, usage);
        sdg.addParameterInOutArc(methodOutputNode, callReturnNode);
    }

    @Override
    public void visit(MethodDeclaration n, Context arg) {
        arg.setCurrentMethod(n);
        super.visit(n, arg);
        arg.setCurrentMethod(null);
    }

    @Override
    public void visit(ConstructorDeclaration n, Context arg) {
//        super.visit(n, arg); SKIPPED BECAUSE IT WAS SKIPPED IN CFG CREATION AND COUNTLESS OTHER VISITORS
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Context arg) {
        arg.setCurrentClass(n);
        super.visit(n, arg);
        arg.setCurrentClass(null);
    }

    @Override
    public void visit(CompilationUnit n, Context arg) {
        arg.setCurrentCU(n);
        super.visit(n, arg);
        arg.setCurrentCU(null);
    }
}
