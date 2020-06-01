package tfm.cli;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import tfm.graphs.sdg.SDG;
import tfm.nodes.GraphNode;
import tfm.utils.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Optional;

public class MethodResolver {

    private static class Args {
        String file;
        String method;
    }

    public static void main(String[] inputArgs) throws FileNotFoundException {
        Args args = parseArgs(inputArgs);

        CompilationUnit cu = JavaParser.parse(new File(args.file));

        SDG sdg = new SDG();
        sdg.build(new NodeList<>(cu));

        VoidVisitorAdapter<Void> visitor = new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodCallExpr n, Void arg) {
                TypeSolver solver = new JavaParserTypeSolver(args.file.substring(0, args.file.lastIndexOf('/')));

                Logger.log("-- Trying to solve method " + n.getNameAsString() + " --");

                Optional<MethodDeclaration> optionalResolvedMethod;

                try {
                    optionalResolvedMethod = getMethodCallWithJavaParserSymbolSolver(n, solver, new ReflectionTypeSolver());
                } catch (UnsolvedSymbolException e) {
                    optionalResolvedMethod = Optional.empty();
                }

                if (!optionalResolvedMethod.isPresent()) {
                    Logger.format("Not found: %s", n);
                    return;
                }

                Logger.format("Found: %s", n.getNameAsString());
                Logger.log(optionalResolvedMethod.get().getSignature().asString());

                Logger.log("-- Trying to match with a node from SDG --");
                Optional<GraphNode<MethodDeclaration>> methodDeclarationNode = optionalResolvedMethod.flatMap(sdg::findNodeByASTNode);

                if (!methodDeclarationNode.isPresent()) {
                    Logger.log("Failed to find node in SDG");
                    return;
                }

                Logger.format("SDG node: %s", methodDeclarationNode.get());

            }
        };

        cu.accept(visitor, null);
    }

    private static Args parseArgs(String[] args) {
        Args res = new Args();

        Logger.log(Arrays.asList(args));

        try {
            res.file = args[0];
            // res.method = args[2];
        } catch (Exception e) {
            Logger.log("Incorrect syntax: java MethodResolver.class <file> <methodName>");
            System.exit(1);
        }

        return res;
    }

    private static Optional<MethodDeclaration> getMethodCallWithJavaParserSymbolSolver(MethodCallExpr methodCallExpr, TypeSolver... solvers) {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver(solvers);

        SymbolReference<ResolvedMethodDeclaration> solver = JavaParserFacade.get(combinedTypeSolver).solve(methodCallExpr);

        return solver.isSolved() ? solver.getCorrespondingDeclaration().toAst() : Optional.empty();
    }
}
