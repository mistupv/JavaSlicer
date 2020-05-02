package tfm.graphs.sdg;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
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
        List<Expression> arguments = methodCallExpr.getArguments();

//        // Parse first method call expressions as arguments
//        arguments.stream()
//                .filter(Expression::isMethodCallExpr)
//                .forEach(expression -> expression.accept(this, context));

        Logger.log("MethodCallReplacerVisitor", context);

        Optional<GraphNode<MethodDeclaration>> optionalNethodDeclarationNode = getMethodDeclarationNodeWithJavaParser(methodCallExpr);

        if (!optionalNethodDeclarationNode.isPresent()) {
            Logger.format("Not found: '%s'. Discarding");
            return;
        }

        GraphNode<MethodDeclaration> calledMethodNode = optionalNethodDeclarationNode.get();

        MethodDeclaration methodCalled = calledMethodNode.getAstNode();

        sdg.addCallArc(methodCallNode, calledMethodNode);

        NodeList<Parameter> parameters = calledMethodNode.getAstNode().getParameters();

        for (int i = 0; i < parameters.size(); i++) {
            Parameter parameter = parameters.get(i);
            Expression argument = arguments.get(i);

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


        }

        // todo make call
        Logger.log("MethodCallReplacerVisitor", String.format("%s | Method '%s' called", methodCallExpr, methodCalled.getNameAsString()));
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
}
