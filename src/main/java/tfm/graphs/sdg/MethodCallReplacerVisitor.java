package tfm.graphs.sdg;

import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.arcs.Arc;
import tfm.arcs.pdg.DataDependencyArc;
import tfm.graphs.cfg.CFG;
import tfm.nodes.GraphNode;
import tfm.nodes.TypeNodeFactory;
import tfm.nodes.type.NodeType;
import tfm.utils.Context;
import tfm.utils.Logger;
import tfm.utils.MethodDeclarationSolver;

import java.util.*;
import java.util.stream.Collectors;

class MethodCallReplacerVisitor extends VoidVisitorAdapter<Context> {

    private SDG sdg;
    private GraphNode<?> originalMethodCallNode;

    public MethodCallReplacerVisitor(SDG sdg) {
        this.sdg = sdg;
    }

    private void searchAndSetMethodCallNode(Node node) {
        Optional<GraphNode<Node>> optionalNode = sdg.findNodeByASTNode(node);
        assert optionalNode.isPresent();
        originalMethodCallNode = optionalNode.get();

    }

    @Override
    public void visit(DoStmt n, Context arg) {
        searchAndSetMethodCallNode(n);
        super.visit(n, arg);
    }

    @Override
    public void visit(ForEachStmt n, Context arg) {
        searchAndSetMethodCallNode(n);
        super.visit(n, arg);
    }

    @Override
    public void visit(ForStmt n, Context arg) {
        searchAndSetMethodCallNode(n);
        super.visit(n, arg);
    }

    @Override
    public void visit(IfStmt n, Context arg) {
        searchAndSetMethodCallNode(n);
        super.visit(n, arg);
    }

    @Override
    public void visit(SwitchStmt n, Context arg) {
        searchAndSetMethodCallNode(n);
        super.visit(n, arg);
    }

    @Override
    public void visit(WhileStmt n, Context arg) {
        searchAndSetMethodCallNode(n);
        super.visit(n, arg);
    }

    @Override
    public void visit(ReturnStmt n, Context arg) {
        searchAndSetMethodCallNode(n);
        super.visit(n, arg);
    }

    @Override
    public void visit(ExpressionStmt n, Context arg) {
        searchAndSetMethodCallNode(n);
        super.visit(n, arg);
    }

    @Override
    public void visit(MethodCallExpr methodCallExpr, Context context) {
        NodeList<Expression> arguments = methodCallExpr.getArguments();

        // Parse first method call expressions as arguments
//        arguments.stream()
//                .filter(Expression::isMethodCallExpr)
//                .forEach(expression -> expression.accept(this, context));

        Logger.log("MethodCallReplacerVisitor", context);

        Optional<GraphNode<MethodDeclaration>> optionalNethodDeclarationNode =
                MethodDeclarationSolver.getInstance()
                    .findDeclarationFrom(methodCallExpr)
                    .flatMap(methodDeclaration -> sdg.findNodeByASTNode(methodDeclaration));

        if (!optionalNethodDeclarationNode.isPresent()) {
            Logger.format("Not found: '%s'. Discarding", methodCallExpr);
            return;
        }

        GraphNode<MethodDeclaration> methodDeclarationNode = optionalNethodDeclarationNode.get();
        MethodDeclaration methodDeclaration = methodDeclarationNode.getAstNode();

        Optional<CFG> optionalCFG = sdg.getMethodCFG(methodDeclaration);
        assert optionalCFG.isPresent();
        CFG methodCFG = optionalCFG.get();

        GraphNode<MethodCallExpr> methodCallNode = sdg.addNode("CALL " + methodCallExpr.toString(), methodCallExpr, TypeNodeFactory.fromType(NodeType.METHOD_CALL));

        sdg.addControlDependencyArc(originalMethodCallNode, methodCallNode);
        sdg.addCallArc(methodCallNode, methodDeclarationNode);

        NodeList<Parameter> parameters = methodDeclarationNode.getAstNode().getParameters();

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
            }

            // In expression
            VariableDeclarationExpr inVariableDeclarationExpr = new VariableDeclarationExpr(
                    new VariableDeclarator(
                            parameter.getType(),
                            parameter.getNameAsString() + "_in",
                            new NameExpr(argument.toString())
                    )
            );

            ExpressionStmt inExprStmt = new ExpressionStmt(inVariableDeclarationExpr);

            GraphNode<ExpressionStmt> argumentInNode = sdg.addNode(inExprStmt.toString(), inExprStmt, TypeNodeFactory.fromType(NodeType.ACTUAL_IN));

            sdg.addControlDependencyArc(methodCallNode, argumentInNode);

            // Handle data dependency: Remove arc from method call node and add it to IN node

            List<DataDependencyArc> inDataDependencies = sdg.incomingEdgesOf(originalMethodCallNode).stream()
                .filter(arc -> arc.isDataDependencyArc() && Objects.equals(arc.getLabel(), argument.toString()))
                .map(Arc::asDataDependencyArc)
                .collect(Collectors.toList());

            for (DataDependencyArc arc : inDataDependencies) {
                GraphNode<?> dataDependencySource = sdg.getEdgeSource(arc);
                sdg.removeEdge(arc);
                sdg.addDataDependencyArc(dataDependencySource, argumentInNode, argument.toString());
            }

            // Now, find the corresponding method declaration's in node and link argument node with it

            Optional<GraphNode<ExpressionStmt>> optionalParameterInNode = sdg.outgoingEdgesOf(methodDeclarationNode).stream()
                    .map(arc -> (GraphNode<ExpressionStmt>) sdg.getEdgeTarget(arc))
                    .filter(node -> node.getNodeType() == NodeType.FORMAL_IN)
                    .filter(node -> node.getInstruction().contains(parameter.getNameAsString() + "_in"))
                    .findFirst();

            if (optionalParameterInNode.isPresent()) {
                sdg.addParameterInOutArc(argumentInNode, optionalParameterInNode.get());
            } else {
                Logger.log("MethodCallReplacerVisitor", "WARNING: IN declaration node for argument " + argument + " not found.");
                Logger.log("MethodCallReplacerVisitor", String.format("Context: %s, Method: %s, Call: %s", context.getCurrentMethod().get().getNameAsString(), methodDeclaration.getSignature().asString(), methodCallExpr));
            }

            // Out expression

            OutNodeVariableVisitor shouldHaveOutNodeVisitor = new OutNodeVariableVisitor();
            Set<String> variablesForOutNode = new HashSet<>();
            argument.accept(shouldHaveOutNodeVisitor, variablesForOutNode);

            // Here, variablesForOutNode may have 1 variable or more depending on the expression

            Logger.log("MethodCallReplacerVisitor", String.format("Variables for out node: %s", variablesForOutNode));
            if (variablesForOutNode.isEmpty()) {
                /*
                    If the argument is not a variable or it is not declared in the scope,
                    then there is no OUT node
                 */
                Logger.log("MethodCallReplacerVisitor", String.format("Expression '%s' should not have out node", argument.toString()));
                continue;
            } else if (variablesForOutNode.size() == 1) {
                String variable = variablesForOutNode.iterator().next();

                List<GraphNode<?>> declarations = sdg.findDeclarationsOfVariable(variable, originalMethodCallNode);

                Logger.log("MethodCallReplacerVisitor", String.format("Declarations of variable: '%s': %s", variable, declarations));

                if (declarations.isEmpty()) {
                    Logger.log("MethodCallReplacerVisitor", String.format("Expression '%s' should not have out node", argument.toString()));
                    continue;
                }
            } else {
                continue;
            }

            AssignExpr outVariableAssignExpr = new AssignExpr(
                argument,
                new NameExpr(parameter.getNameAsString() + "_out"),
                AssignExpr.Operator.ASSIGN
            );

            ExpressionStmt outExprStmt = new ExpressionStmt(outVariableAssignExpr);

            GraphNode<ExpressionStmt> argumentOutNode = sdg.addNode(outExprStmt.toString(), outExprStmt, TypeNodeFactory.fromType(NodeType.ACTUAL_OUT));

            sdg.addControlDependencyArc(methodCallNode, argumentOutNode);

            // Now, find the corresponding method call's out node and link argument node with it

            Optional<GraphNode<ExpressionStmt>> optionalParameterOutNode = sdg.outgoingEdgesOf(methodDeclarationNode).stream()
                    .map(arc -> (GraphNode<ExpressionStmt>) sdg.getEdgeTarget(arc))
                    .filter(node -> node.getNodeType() == NodeType.FORMAL_OUT)
                    .filter(node -> node.getInstruction().contains(parameter.getNameAsString() + "_out"))
                    .findFirst();

            // Handle data dependency: remove arc from method call node and add it to OUT node

            List<DataDependencyArc> outDataDependencies = sdg.outgoingEdgesOf(originalMethodCallNode).stream()
                    .filter(arc -> arc.isDataDependencyArc() && Objects.equals(arc.getLabel(), argument.toString()))
                    .map(Arc::asDataDependencyArc)
                    .collect(Collectors.toList());

            for (DataDependencyArc arc : outDataDependencies) {
                GraphNode<?> dataDependencyTarget = sdg.getEdgeTarget(arc);
                sdg.removeEdge(arc);
                sdg.addDataDependencyArc(argumentOutNode, dataDependencyTarget, argument.toString());
            }

            if (optionalParameterOutNode.isPresent()) {
                sdg.addParameterInOutArc(optionalParameterOutNode.get(), argumentOutNode);
            } else {
                Logger.log("MethodCallReplacerVisitor", "WARNING: OUT declaration node for argument " + argument + " not found.");
                Logger.log("MethodCallReplacerVisitor", String.format("Context: %s, Method: %s, Call: %s", context.getCurrentMethod().get().getNameAsString(), methodDeclaration.getSignature().asString(), methodCallExpr));
            }
        }

        // Add 'output' node of the call

        // First, check if method has an output node

        if (methodDeclaration.getType().isVoidType()) {
            return;
        }

        // If not void, find the output node

        Optional<GraphNode<EmptyStmt>> optionalDeclarationOutputNode = sdg.outgoingEdgesOf(methodDeclarationNode).stream()
                .filter(arc -> sdg.getEdgeTarget(arc).getNodeType() == NodeType.METHOD_OUTPUT)
                .map(arc -> (GraphNode<EmptyStmt>) sdg.getEdgeTarget(arc))
                .findFirst();

        if (!optionalDeclarationOutputNode.isPresent()) {
            // Method return type is void, do nothing
            return;
        }

        GraphNode<EmptyStmt> declarationOutputNode = optionalDeclarationOutputNode.get();

        // If method has output node, then create output call node and link them
        GraphNode<EmptyStmt> callReturnNode = sdg.addNode("output", new EmptyStmt(), TypeNodeFactory.fromType(NodeType.METHOD_CALL_RETURN));

        sdg.addControlDependencyArc(methodCallNode, callReturnNode);
        sdg.addDataDependencyArc(callReturnNode, originalMethodCallNode);
        sdg.addParameterInOutArc(declarationOutputNode, callReturnNode);

        Logger.log("MethodCallReplacerVisitor", String.format("%s | Method '%s' called", methodCallExpr, methodDeclaration.getNameAsString()));
    }

    private void argumentAsNameExpr(GraphNode<ExpressionStmt> methodCallNode) {

    }

    @Override
    public void visit(MethodDeclaration n, Context arg) {
        arg.setCurrentMethod(n);

        super.visit(n, arg);
    }
}
