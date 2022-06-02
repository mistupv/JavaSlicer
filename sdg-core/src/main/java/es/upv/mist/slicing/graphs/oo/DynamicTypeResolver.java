package es.upv.mist.slicing.graphs.oo;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import es.upv.mist.slicing.graphs.CallGraph;
import es.upv.mist.slicing.graphs.ClassGraph;
import es.upv.mist.slicing.graphs.cfg.CFG;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.VariableAction;
import es.upv.mist.slicing.nodes.io.ActualIONode;
import es.upv.mist.slicing.nodes.io.FormalIONode;
import es.upv.mist.slicing.utils.ASTUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** A dynamic type solver that complements JavaParser's {@code resolve()} method. */
public class DynamicTypeResolver {
    protected final Map<CallableDeclaration<?>, CFG> cfgMap;
    protected final ClassGraph classGraph;
    protected final CallGraph callGraph;

    public DynamicTypeResolver(Map<CallableDeclaration<?>, CFG> cfgMap, ClassGraph classGraph, CallGraph callGraph) {
        this.cfgMap = cfgMap;
        this.classGraph = classGraph;
        this.callGraph = callGraph;
    }

    /** Obtains the possible dynamic types of the given expression, which is contained within a GraphNode.
     *  Only expressions of a reference type are allowed (e.g. objects, arrays, but not primitives). */
    public Set<ResolvedType> resolve(Expression expression, GraphNode<?> container) {
        assert expression.calculateResolvedType().isReference(): "The expression must be of reference type (no primitives).";
        return resolveStreamed(expression, container).collect(Collectors.toSet());
    }

    /** Directs each kind of expression to the appropriate resolve method. */
    protected Stream<ResolvedType> resolveStreamed(Expression expression, GraphNode<?> container) {
        if (expression.isMethodCallExpr())
            return resolveMethodCallExpr(expression.asMethodCallExpr());
        if (expression.isNameExpr() || expression.isFieldAccessExpr()) // May be field, local variable or parameter
            return resolveVariable(expression, container);
        if (expression.isArrayAccessExpr() ||
                expression.isThisExpr())
            return anyTypeOf(expression);
        if (expression.isCastExpr())
            return resolveCast(expression.asCastExpr(), container);
        if (expression.isEnclosedExpr())
            return resolveStreamed(expression.asEnclosedExpr().getInner(), container);
        if (expression.isObjectCreationExpr() ||
                expression.isArrayCreationExpr())
            return Stream.of(expression.calculateResolvedType());
        throw new IllegalArgumentException("The given expression is not an object-compatible one.");
    }

    /** Checks the possible values of all ReturnStmt of this call's target methods. */
    protected Stream<ResolvedType> resolveMethodCallExpr(MethodCallExpr methodCallExpr) {
        assert !methodCallExpr.calculateResolvedType().isVoid();
        return callGraph.getCallTargets(methodCallExpr)
                .filter(ASTUtils::hasBody) // abstract or interface methods must be skipped
                .map(cfgMap::get)
                .flatMap(cfg -> cfg.vertexSet().stream())
                .filter(node -> node.getAstNode() instanceof ReturnStmt)
                .flatMap(node -> resolveStreamed(((ReturnStmt) node.getAstNode()).getExpression().orElseThrow(), node));
    }

    /** Searches for the corresponding VariableAction object, then calls {@link #resolveVariableAction(VariableAction)}. */
    protected Stream<ResolvedType> resolveVariable(Expression expression, GraphNode<?> graphNode) {
        // TODO: implement a search like ExpressionObjectTreeFinder.
        return anyTypeOf(expression);
    }

    /**
     * Looks up the last definitions according to the CFG, then resolves the last assignment to it.
     * If the last definition was not in this method (in the case of parameters or fields), it
     * uses auxiliary methods to locate the last interprocedural definition.
     * Otherwise, the last expression(s) assigned to it is found and recursively resolved.
     */
    protected Stream<ResolvedType> resolveVariableAction(VariableAction va) {
        CFG cfg = cfgMap.get(findCallableDeclarationFromGraphNode(va.getGraphNode()));
        return cfg.findLastDefinitionsFrom(va).stream()
                .flatMap(def -> {
                    if (def.asDefinition().getExpression() == null) {
                        if (def instanceof VariableAction.Movable && ((VariableAction.Movable) def).getRealNode() instanceof FormalIONode)
                            return resolveFormalIn((FormalIONode) ((VariableAction.Movable) def).getRealNode());
                        throw new IllegalArgumentException("Definition was not movable and hadn't an expression.");
                    }
                    return resolveStreamed(def.asDefinition().getExpression(), def.getGraphNode());
                });
    }

    /** Locate the declaration (method or constructor) where the given node is located. */
    protected CallableDeclaration<?> findCallableDeclarationFromGraphNode(GraphNode<?> node) {
        return cfgMap.values().stream()
                .filter(cfg -> cfg.containsVertex(node))
                .map(CFG::getDeclaration)
                .findFirst().orElseThrow();
    }

    /** Looks up the expression assigned to all corresponding actual-in nodes and resolves it. */
    protected Stream<ResolvedType> resolveFormalIn(FormalIONode formalIn) {
        assert formalIn.isInput();
        return callGraph.callsTo(findCallableDeclarationFromGraphNode(formalIn))
                .map(this::findNodeInMapByAST)
                .map(GraphNode::getVariableActions)
                .flatMap(List::stream)
                .filter(VariableAction.Movable.class::isInstance)
                .map(VariableAction.Movable.class::cast)
                .flatMap(movable -> {
                    GraphNode<?> realNode = movable.getRealNode();
                    if (!(realNode instanceof ActualIONode))
                        return Stream.empty();
                    ActualIONode actualIn = (ActualIONode) realNode;
                    if (!actualIn.matchesFormalIO(formalIn))
                        return Stream.empty();
                    return resolveStreamed(actualIn.getArgument(), movable.getGraphNode());
                });
    }

    /** Locate the CFG graph node in which the argument is contained.
     *  Its time requirement is linear with the number of total nodes in */
    protected GraphNode<?> findNodeInMapByAST(Node astNode) {
        return cfgMap.values().stream()
                .map(cfg -> cfg.findNodeByASTNode(astNode))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst().orElseThrow();
    }

    /** Checks if the cast marks a more specific type and returns that one.
     *  Otherwise, it unwraps the cast expression and recursively resolves the type
     *  of the inner expression. */
    protected Stream<ResolvedType> resolveCast(CastExpr cast, GraphNode<?> container) {
        if (ASTUtils.isDownCast(cast))
            return Stream.of(cast.getType().resolve());
        return resolveStreamed(cast.getExpression(), container);
    }

    /** Returns all possible types that the given expression can be, by obtaining its static type
     *  and locating all subtypes in the class graph. */
    protected Stream<ResolvedType> anyTypeOf(Expression expression) {
        ResolvedClassDeclaration type = expression.calculateResolvedType().asReferenceType()
                .getTypeDeclaration().orElseThrow().asClass();
        return classGraph.subclassesOf(type).stream()
                .map(TypeDeclaration::resolve)
                .map(ASTUtils::resolvedTypeDeclarationToResolvedType);
    }
}
