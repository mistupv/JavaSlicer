package es.upv.mist.slicing.graphs.oo;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.resolution.types.ResolvedType;
import es.upv.mist.slicing.graphs.ClassGraph;
import es.upv.mist.slicing.graphs.cfg.CFG;
import es.upv.mist.slicing.nodes.VariableAction;
import es.upv.mist.slicing.utils.ASTUtils;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** A dynamic type solver that complements Javaparser's {@code resolve()} method. */
public class DynamicTypeResolver {
    protected final CFG cfg;
    protected final ClassGraph classGraph;

    public DynamicTypeResolver(CFG cfg, ClassGraph classGraph) {
        this.cfg = cfg;
        this.classGraph = classGraph;
    }

    /** Obtains the set of dynamic types that the variable passed as argument
     *  may take. <strong>The current implementation is intra-procedural!</strong> */
    public Set<ResolvedType> resolve(VariableAction action) {
        return resolveIntraProcedural(action);
    }

    /** Obtains a list of possible dynamic types for the given action, by
     *  searching for definitions intraprocedurally. */
    public Set<ResolvedType> resolveIntraProcedural(VariableAction action) {
        return cfg.findLastDefinitionsFrom(action).stream()
                .map(VariableAction::asDefinition)
                .filter(Objects::nonNull)
                .map(def -> {
                    Expression expr = unwrapExpr(def.getExpression());
                    if (expr.isObjectCreationExpr() || expr.isArrayCreationExpr()
                            || expr.isArrayInitializerExpr())
                        return Set.of(expr.calculateResolvedType());
                    else
                        return findAllSubtypes(def.getExpression().calculateResolvedType());
                })
                .reduce(new HashSet<>(), (a, b) -> { a.addAll(b); return a; });
    }

    /** Unwraps an enclosed expression, and other constructs that are type-transparent. */
    protected Expression unwrapExpr(Expression expr) {
        if (expr.isCastExpr())
            if (ASTUtils.isDownCast(expr.asCastExpr()))
                return expr;
            else
                return unwrapExpr(expr.asCastExpr().getExpression());
        if (expr.isEnclosedExpr())
            return unwrapExpr(expr.asEnclosedExpr().getInner());
        return expr;
    }

    /** Extracts from the call graph all the subtypes of the given argument.
     *  The input is included as part of the output. */
    protected Set<ResolvedType> findAllSubtypes(ResolvedType t) {
        return classGraph.subclassesOf(t.asReferenceType().getTypeDeclaration().orElseThrow().asClass()).stream()
                .map(ClassOrInterfaceDeclaration::resolve)
                .map(ASTUtils::resolvedTypeDeclarationToResolvedType)
                .collect(Collectors.toSet());
    }
}
