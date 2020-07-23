package tfm.graphs.exceptionsensitive;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import tfm.graphs.sdg.MethodCallReplacerVisitor;
import tfm.nodes.*;
import tfm.utils.Context;

import java.util.*;
import java.util.stream.Collectors;

public class ExceptionSensitiveMethodCallReplacerVisitor extends MethodCallReplacerVisitor {
    public ExceptionSensitiveMethodCallReplacerVisitor(ESSDG sdg) {
        super(sdg);
    }

    @Override
    public void visit(MethodCallExpr methodCallExpr, Context context) {
        if (methodCallExpr.resolve().getNumberOfSpecifiedExceptions() > 0)
            handleExceptionReturnArcs(methodCallExpr);
        super.visit(methodCallExpr, context);
    }

    /** Creates the following connections:
     * <ul>
     *     <li>{@link ExceptionExitNode} to {@link ExceptionReturnNode} with a ratio of (* to 1), n per method</li>
     *     <li>{@link NormalExitNode} to {@link NormalReturnNode} with a ratio of (1 to 1), 1 per method</li>
     * </ul>
     * @param call The method call to be connected to its method declaration.
     */
    protected void handleExceptionReturnArcs(MethodCallExpr call) {
        Set<SyntheticNode<?>> synthNodes = sdg.vertexSet().stream()
                .filter(SyntheticNode.class::isInstance)
                .map(n -> (SyntheticNode<?>) n)
                .collect(Collectors.toSet());
        ResolvedMethodDeclaration resolvedDecl = call.resolve();
        MethodDeclaration decl = resolvedDecl.toAst().orElseThrow();

        connectNormalNodes(synthNodes, call, decl);
        connectExceptionNodes(synthNodes, call, decl);
    }

    /** Connects normal exit nodes to their corresponding return node. */
    protected void connectNormalNodes(Set<SyntheticNode<?>> synthNodes, MethodCallExpr call, MethodDeclaration decl) {
        ReturnNode normalReturn = (ReturnNode) synthNodes.stream()
                .filter(NormalReturnNode.class::isInstance)
                .filter(n -> n.getAstNode() == call)
                .findAny().orElseThrow();
        ExitNode normalExit = (ExitNode) synthNodes.stream()
                .filter(NormalExitNode.class::isInstance)
                .filter(n -> n.getAstNode() == decl)
                .findAny().orElseThrow();
        ((ESSDG) sdg).addReturnArc(normalExit, normalReturn);
    }

    /**
     * Connects exception exit nodes to their corresponding return node,
     * taking into account that return nodes are generated from the 'throws' list
     * in the method declaration and exit nodes are generated from the exception sources
     * that appear in the method. This creates a mismatch that is solved in {@link #connectRemainingExceptionNodes(Map, Set)}
     */
    protected void connectExceptionNodes(Set<SyntheticNode<?>> synthNodes, MethodCallExpr call, MethodDeclaration decl) {
        Map<ResolvedType, ExceptionReturnNode> exceptionReturnMap = new HashMap<>();
        Set<ExceptionExitNode> eeNodes = synthNodes.stream()
                .filter(ExceptionExitNode.class::isInstance)
                .map(ExceptionExitNode.class::cast)
                .filter(n -> n.getAstNode() == decl)
                .collect(Collectors.toSet());
        for (ReferenceType rType : decl.getThrownExceptions()) {
            ResolvedType type = rType.resolve();
            ExceptionReturnNode exceptionReturn = synthNodes.stream()
                    .filter(ExceptionReturnNode.class::isInstance)
                    .map(ExceptionReturnNode.class::cast)
                    .filter(n -> n.getAstNode() == call)
                    .filter(n -> n.getExceptionType().equals(type))
                    .findAny().orElseThrow();
            ExceptionExitNode exceptionExit = eeNodes.stream()
                    .filter(n -> n.getExceptionType().equals(type))
                    .findAny().orElseThrow();
            eeNodes.remove(exceptionExit);
            exceptionReturnMap.put(type, exceptionReturn);
            ((ESSDG) sdg).addReturnArc(exceptionExit, exceptionReturn);
        }

        connectRemainingExceptionNodes(exceptionReturnMap, eeNodes);
    }

    /** Connects the remaining exception exit nodes to their closest exception return node. */
    protected void connectRemainingExceptionNodes(Map<ResolvedType, ExceptionReturnNode> exceptionReturnMap, Set<ExceptionExitNode> eeNodes) {
        boolean hasThrowable = resolvedTypeSetContains(exceptionReturnMap.keySet(), "java.lang.Throwable");
        boolean hasException = resolvedTypeSetContains(exceptionReturnMap.keySet(), "java.lang.Exception");

        eeFor: for (ExceptionExitNode ee : eeNodes) {
            List<ResolvedReferenceType> typeList = List.of(ee.getExceptionType().asReferenceType());
            while (!typeList.isEmpty()) {
                List<ResolvedReferenceType> newTypeList = new LinkedList<>();
                for (ResolvedReferenceType type : typeList) {
                    if (exceptionReturnMap.containsKey(type)) {
                        ((ESSDG) sdg).addReturnArc(ee, exceptionReturnMap.get(type));
                        continue eeFor;
                    }
                    // Skip RuntimeException, unless Throwable or Exception are present as ER nodes
                    if (type.getQualifiedName().equals("java.lang.RuntimeException") && !hasThrowable && !hasException)
                        continue;
                    // Skip Error, unless Throwable is present as EE node
                    if (type.getQualifiedName().equals("java.lang.Error") && !hasThrowable)
                        continue;
                    // Object has no ancestors, the search has ended
                    if (!type.getQualifiedName().equals("java.lang.Object"))
                        newTypeList.addAll(type.asReferenceType().getDirectAncestors());
                }
                typeList = newTypeList;
            }
        }
    }

    /** Utility method. Finds whether or not a type can be found in a set of resolved types.
     * The full qualified name should be used (e.g. 'java.lang.Object' for Object). */
    protected static boolean resolvedTypeSetContains(Set<ResolvedType> set, String qualifiedType) {
        return set.stream()
                .map(ResolvedType::asReferenceType)
                .map(ResolvedReferenceType::getQualifiedName)
                .anyMatch(qualifiedType::equals);
    }
}
