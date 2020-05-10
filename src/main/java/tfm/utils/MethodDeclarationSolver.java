package tfm.utils;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.model.resolution.SymbolReference;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;

import java.util.*;

public class MethodDeclarationSolver {

    private static final MethodDeclarationSolver instance = new MethodDeclarationSolver();
    private static final List<TypeSolver> usedTypeSolvers = new ArrayList<>();

    private MethodDeclarationSolver() {

    }

    public static void addTypeSolvers(TypeSolver... typeSolvers) {
        usedTypeSolvers.addAll(Arrays.asList(typeSolvers));
    }

    public static MethodDeclarationSolver getInstance() {
        return instance;
    }

    public Optional<MethodDeclaration> findDeclarationFrom(MethodCallExpr methodCallExpr) {
        return this.findDeclarationFrom(methodCallExpr, usedTypeSolvers);
    }

    public Optional<MethodDeclaration> findDeclarationFrom(MethodCallExpr methodCallExpr, Collection<? extends TypeSolver> customTypeSolvers) {
        CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver(customTypeSolvers.toArray(new TypeSolver[0]));

        try {
            SymbolReference<ResolvedMethodDeclaration> solver = JavaParserFacade.get(combinedTypeSolver).solve(methodCallExpr);

            return solver.isSolved()
                    ? solver.getCorrespondingDeclaration().toAst()
                    : Optional.empty();
        } catch (UnsolvedSymbolException e) {
            return Optional.empty();
        }
    }
}
