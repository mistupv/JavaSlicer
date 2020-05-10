package tfm.utils;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import tfm.nodes.GraphNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SymbolSolverWrapper {

    private static final SymbolSolverWrapper instance = new SymbolSolverWrapper();
    private static final List<TypeSolver> typeSolvers = new ArrayList<>();

    private SymbolSolverWrapper() {

    }

    public static void addTypeSolver(TypeSolver typeSolver) {
        typeSolvers.add(typeSolver);
    }

    public static SymbolSolverWrapper getInstance() {
        return instance;
    }

    private <N extends Node> Optional<N> findNodeFrom(MethodCallExpr methodCallExpr) {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();

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
