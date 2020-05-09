package tfm.graphs.sdg;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.SourceFileInfoExtractor;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.SymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import tfm.arcs.Arc;
import tfm.arcs.pdg.DataDependencyArc;
import tfm.graphs.cfg.CFG;
import tfm.nodes.GraphNode;
import tfm.nodes.TypeNodeFactory;
import tfm.nodes.type.NodeType;
import tfm.utils.Context;
import tfm.utils.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

class MethodCallReplacerVisitor extends VoidVisitorAdapter<Context> {

    private SDG sdg;
    private GraphNode<?> methodCallNode;

    public MethodCallReplacerVisitor(SDG sdg) {
        this.sdg = sdg;
    }

    private void searchAndSetMethodCallNode(Node node) {
        Optional<GraphNode<Node>> optionalNode = sdg.findNodeByASTNode(node);
        assert optionalNode.isPresent();
        methodCallNode = optionalNode.get();

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

//        // Parse first method call expressions as arguments
//        arguments.stream()
//                .filter(Expression::isMethodCallExpr)
//                .forEach(expression -> expression.accept(this, context));

        Logger.log("MethodCallReplacerVisitor", context);

        Optional<GraphNode<MethodDeclaration>> optionalNethodDeclarationNode = getMethodDeclarationNodeWithJavaParser(methodCallExpr);

        if (!optionalNethodDeclarationNode.isPresent()) {
            Logger.format("Not found: '%s'. Discarding", methodCallExpr);
            return;
        }

        GraphNode<MethodDeclaration> methodDeclarationNode = optionalNethodDeclarationNode.get();
        MethodDeclaration methodDeclaration = methodDeclarationNode.getAstNode();

        Optional<CFG> optionalCFG = sdg.getMethodCFG(methodDeclaration);
        assert optionalCFG.isPresent();
        CFG methodCFG = optionalCFG.get();

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

            GraphNode<ExpressionStmt> argumentInNode = sdg.addNode(inExprStmt.toString(), inExprStmt, TypeNodeFactory.fromType(NodeType.VARIABLE_IN));

            sdg.addControlDependencyArc(methodCallNode, argumentInNode);

            // Handle data dependency: Remove arc from method call node and add it to IN node

            List<DataDependencyArc> inDataDependencies = sdg.incomingEdgesOf(methodCallNode).stream()
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
                    .filter(node -> node.getNodeType() == NodeType.VARIABLE_IN && node.getInstruction().contains(parameter.getNameAsString() + "_in"))
                    .findFirst();

            if (optionalParameterInNode.isPresent()) {
                sdg.addParameterInOutArc(argumentInNode, optionalParameterInNode.get());
            } else {
                Logger.log("MethodCallReplacerVisitor", "WARNING: IN declaration node for argument " + argument + " not found.");
                Logger.log("MethodCallReplacerVisitor", String.format("Context: %s, Method: %s, Call: %s", context.getCurrentMethod().get().getNameAsString(), methodDeclaration.getSignature().asString(), methodCallExpr));
            }

            // Out expression
            VariableDeclarationExpr outVariableDeclarationExpr = new VariableDeclarationExpr(
                    new VariableDeclarator(
                            parameter.getType(),
                            parameter.getNameAsString(),
                            new NameExpr(parameter.getNameAsString() + "_out")
                    )
            );

            ExpressionStmt outExprStmt = new ExpressionStmt(outVariableDeclarationExpr);

            GraphNode<ExpressionStmt> argumentOutNode = sdg.addNode(outExprStmt.toString(), outExprStmt, TypeNodeFactory.fromType(NodeType.VARIABLE_OUT));

            sdg.addControlDependencyArc(methodCallNode, argumentOutNode);

            // Now, find the corresponding method declaration's out node and link argument node with it

            Optional<GraphNode<ExpressionStmt>> optionalParameterOutNode = sdg.outgoingEdgesOf(methodDeclarationNode).stream()
                    .map(arc -> (GraphNode<ExpressionStmt>) sdg.getEdgeTarget(arc))
                    .filter(node -> node.getNodeType() == NodeType.VARIABLE_OUT && node.getInstruction().contains(parameter.getNameAsString() + "_out"))
                    .findFirst();

            if (optionalParameterOutNode.isPresent()) {
                sdg.addParameterInOutArc(optionalParameterOutNode.get(), argumentOutNode);
            } else {
                Logger.log("MethodCallReplacerVisitor", "WARNING: OUT declaration node for argument " + argument + " not found.");
                Logger.log("MethodCallReplacerVisitor", String.format("Context: %s, Method: %s, Call: %s", context.getCurrentMethod().get().getNameAsString(), methodDeclaration.getSignature().asString(), methodCallExpr));
            }
        }

        // todo make call
        Logger.log("MethodCallReplacerVisitor", String.format("%s | Method '%s' called", methodCallExpr, methodDeclaration.getNameAsString()));
    }

    private Optional<GraphNode<MethodDeclaration>> getMethodDeclarationNodeWithJavaParser(MethodCallExpr methodCallExpr) {
        TypeSolver typeSolver = new ReflectionTypeSolver();

        try {
            SymbolReference<ResolvedMethodDeclaration> solver = JavaParserFacade.get(typeSolver).solve(methodCallExpr);

            return solver.isSolved()
                    ? solver.getCorrespondingDeclaration().toAst()
                        .flatMap(methodDeclaration -> sdg.findNodeByASTNode(methodDeclaration))
                    : Optional.empty();
        } catch (UnsolvedSymbolException e) {
            return Optional.empty();
        }
    }

    @Override
    public void visit(MethodDeclaration n, Context arg) {
        arg.setCurrentMethod(n);

        super.visit(n, arg);
    }
}
