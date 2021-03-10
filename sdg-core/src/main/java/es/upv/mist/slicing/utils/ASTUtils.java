package es.upv.mist.slicing.utils;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.model.typesystem.ReferenceTypeImpl;
import es.upv.mist.slicing.nodes.GraphNode;

import java.util.*;

/** JavaParser-related utility functions. */
public class ASTUtils {
    private ASTUtils() {
        throw new UnsupportedOperationException("This is a static, utility class");
    }

    public static boolean isContained(Node upper, Node contained) {
        Optional<Node> parent = contained.getParentNode();
        if (parent.isEmpty())
            return false;
        return equalsWithRangeInCU(upper, parent.get()) || isContained(upper, parent.get());
    }

    public static boolean switchHasDefaultCase(SwitchStmt stmt) {
        return switchGetDefaultCase(stmt) != null;
    }

    public static SwitchEntry switchGetDefaultCase(SwitchStmt stmt) {
        for (SwitchEntry entry : stmt.getEntries())
            if (entry.getLabels().isEmpty())
                return entry;
        return null;
    }

    public static boolean equalsInDeclaration(Node n1, Node n2) {
        if (n1 == n2)
            return true;
        CallableDeclaration<?> d1 = getDeclarationNode(n1);
        CallableDeclaration<?> d2 = getDeclarationNode(n2);
        return n1.equals(n2) && equalsWithRange(d1, d2);
    }

    public static boolean equalsWithRange(Node n1, Node n2) {
        if (n1 == null || n2 == null)
            return n1 == n2;
        return Objects.equals(n1.getRange(), n2.getRange()) && Objects.equals(n1, n2);
    }

    public static boolean equalsWithRangeInCU(Node n1, Node n2) {
        if (n1 == null || n2 == null)
            return n1 == n2;
        return n1.findCompilationUnit().equals(n2.findCompilationUnit())
                && equalsWithRange(n1, n2);
    }

    public static boolean resolvableIsVoid(Resolvable<? extends ResolvedMethodLikeDeclaration> call) {
        var resolved = call.resolve();
        if (resolved instanceof ResolvedMethodDeclaration)
            return ((ResolvedMethodDeclaration) resolved).getReturnType().isVoid();
        if (resolved instanceof ResolvedConstructorDeclaration)
            return false;
        throw new IllegalArgumentException("Call didn't resolve to either method or constructor!");
    }

    public static boolean resolvableIsPrimitive(Resolvable<? extends ResolvedMethodLikeDeclaration> call) {
        var resolved = call.resolve();
        if (resolved instanceof ResolvedMethodDeclaration)
            return ((ResolvedMethodDeclaration) resolved).getReturnType().isPrimitive();
        if (resolved instanceof ResolvedConstructorDeclaration)
            return false;
        throw new IllegalArgumentException("Call didn't resolve to either method or constructor!");
    }

    public static boolean declarationReturnIsObject(CallableDeclaration<?> declaration) {
        if (declaration.isMethodDeclaration())
            return declaration.asMethodDeclaration().getType().isClassOrInterfaceType();
        if (declaration.isConstructorDeclaration())
            return true;
        throw new IllegalArgumentException("Declaration wasn't method or constructor");
    }

    public static int getMatchingParameterIndex(CallableDeclaration<?> declaration, String paramName) {
        var parameters = declaration.getParameters();
        for (int i = 0; i < parameters.size(); i++)
            if (parameters.get(i).getNameAsString().equals(paramName))
                return i;
        throw new IllegalArgumentException("Expression resolved to a parameter, but could not be found!");
    }

    public static List<Expression> getResolvableArgs(Resolvable<? extends ResolvedMethodLikeDeclaration> call) {
        if (call instanceof MethodCallExpr)
            return ((MethodCallExpr) call).getArguments();
        if (call instanceof ObjectCreationExpr)
            return ((ObjectCreationExpr) call).getArguments();
        if (call instanceof ExplicitConstructorInvocationStmt)
            return ((ExplicitConstructorInvocationStmt) call).getArguments();
        throw new IllegalArgumentException("Call wasn't of a compatible type!");
    }

    public static BlockStmt getCallableBody(CallableDeclaration<?> callableDeclaration) {
        if (callableDeclaration instanceof MethodDeclaration)
            return ((MethodDeclaration) callableDeclaration).getBody().orElseThrow(() -> new IllegalStateException("Graph creation is not allowed for abstract or native methods!"));
        if (callableDeclaration instanceof ConstructorDeclaration)
            return ((ConstructorDeclaration) callableDeclaration).getBody();
        throw new IllegalStateException("The method must have a body!");
    }

    public static Optional<Expression> getResolvableScope(Resolvable<? extends ResolvedMethodLikeDeclaration> call) {
        if (call instanceof MethodCallExpr)
            return ((MethodCallExpr) call).getScope();
        if (call instanceof ObjectCreationExpr)
            return ((ObjectCreationExpr) call).getScope();
        if (call instanceof ExplicitConstructorInvocationStmt)
            return Optional.empty();
        throw new IllegalArgumentException("Call wasn't of a compatible type!");
    }

    public static Optional<? extends CallableDeclaration<?>> getResolvedAST(ResolvedMethodLikeDeclaration resolvedDeclaration) {
        if (resolvedDeclaration instanceof ResolvedMethodDeclaration)
            return ((ResolvedMethodDeclaration) resolvedDeclaration).toAst();
        if (resolvedDeclaration instanceof ResolvedConstructorDeclaration)
            return ((ResolvedConstructorDeclaration) resolvedDeclaration).toAst();
        throw new IllegalStateException("AST node of invalid type");
    }

    public static boolean shouldVisitArgumentsForMethodCalls(Resolvable<? extends ResolvedMethodLikeDeclaration> call) {
        return getResolvedAST(call.resolve()).isEmpty();
    }

    public static boolean shouldVisitArgumentsForMethodCalls(Resolvable<? extends ResolvedMethodLikeDeclaration> call, GraphNode<?> graphNode) {
        return shouldVisitArgumentsForMethodCalls(call) || graphNode == null;
    }

    public static boolean constructorHasExplicitConstructorInvocation(ConstructorDeclaration declaration) {
        return !getCallableBody(declaration).getStatements().isEmpty() &&
                getCallableBody(declaration).getStatements().getFirst().get() instanceof ExplicitConstructorInvocationStmt;
    }

    /**
     * Creates a new set that is suitable for JavaParser nodes. This
     * set behaves by comparing by identity (==) instead of equality (equals()).
     * Thus, multiple objects representing the same node will not be identified as
     * equal, and duplicates will be inserted. For this use-case, you may use
     * {@link NodeHashSet}.
     */
    public static <T> Set<T> newIdentityHashSet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }

    /**
     * Creates a new map that is suitable for JavaParser nodes as keys. This
     * map behaves by comparing by identity (==) instead of equality (equals()).
     * Thus, multiple objects representing the same node will not be identified as
     * equal, and duplicates will be inserted.
     */
    public static <K, V> Map<K, V> newIdentityHashMap() {
        return new IdentityHashMap<>();
    }

    /** Converts a type declaration into just a type. */
    public static ResolvedType resolvedTypeDeclarationToResolvedType(ResolvedReferenceTypeDeclaration decl) {
        return new ReferenceTypeImpl(decl, StaticTypeSolver.getTypeSolver());
    }

    /**
     * Whether a cast of reference type is a downcast; which means
     * that the type of the cast is strictly more specific than the expression's static type.
     * This method should only be called with cast expressions of reference type (no primitives).
     * Otherwise it will fail.
     * <br/>
     * Examples:
     * <ul>
     *     <li>{@code (Object) new String()}: false.</li>
     *     <li>{@code (String) new String()}: false</li>
     *     <li>{@code (String) object}: true.</li>
     * </ul>
     */
    public static boolean isDownCast(CastExpr castExpr) {
        ResolvedType castType = castExpr.getType().resolve();
        ResolvedType exprType = castExpr.getExpression().calculateResolvedType();
        if (castType.isReferenceType() && exprType.isReferenceType()) {
            if (castType.equals(exprType))
                return false;
            return castType.asReferenceType().getAllAncestors().contains(exprType.asReferenceType());
        }
        throw new IllegalArgumentException("This operation is only valid for reference type cast operations.");
    }

    /** Given an AST node, visit the parent until finding a ClassOrInterfaceDeclaration */
    public static ClassOrInterfaceDeclaration getClassNode(Node n){
        Node upperNode = n;
        while (!(upperNode instanceof ClassOrInterfaceDeclaration))
            upperNode = upperNode.getParentNode().orElseThrow();
        return (ClassOrInterfaceDeclaration) upperNode;
    }

    public static CallableDeclaration<?> getDeclarationNode(Node n) {
        assert n instanceof Statement || n instanceof Expression || n instanceof CallableDeclaration;
        Node upperNode = n;
        while (!(upperNode instanceof CallableDeclaration))
            upperNode = upperNode.getParentNode().orElseThrow();
        return (CallableDeclaration<?>) upperNode;
    }

    /** Generates the default initializer, given a field. In Java, reference types
     *  default to null, booleans to false and all other primitives to 0. */
    public static Expression initializerForField(FieldDeclaration field) {
        Type type = field.getVariables().getFirst().orElseThrow().getType();
        if (type.isReferenceType())
            return new NullLiteralExpr();
        if (type.isPrimitiveType()) {
            PrimitiveType primitive = type.asPrimitiveType();
            if (primitive.equals(PrimitiveType.booleanType()))
                return new BooleanLiteralExpr();
            else
                return new IntegerLiteralExpr();
        }
        throw new IllegalArgumentException("Invalid typing for a field");
    }

    /** Returns a List with FieldDeclarations and InitializerDeclarations static/dynamic items of the given class */
    public static List<BodyDeclaration<?>> getClassInit(ClassOrInterfaceDeclaration clazz, boolean isStatic) {
        List<BodyDeclaration<?>> classInit = new LinkedList<>();
        for (BodyDeclaration<?> member : clazz.getMembers()) {
            if (member.isFieldDeclaration() &&
                    member.asFieldDeclaration().isStatic() == isStatic)
                classInit.add(member);
            if (member.isInitializerDeclaration() &&
                    member.asInitializerDeclaration().isStatic() == isStatic)
                classInit.add(member);
        }
        return classInit;
    }
}
