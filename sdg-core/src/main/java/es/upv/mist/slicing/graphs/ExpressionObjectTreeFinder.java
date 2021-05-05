package es.upv.mist.slicing.graphs;

import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedValueDeclaration;
import com.github.javaparser.utils.Pair;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.ObjectTree;
import es.upv.mist.slicing.nodes.VariableAction;
import es.upv.mist.slicing.utils.ASTUtils;

import java.util.LinkedList;
import java.util.List;

import static es.upv.mist.slicing.graphs.cfg.CFGBuilder.VARIABLE_NAME_OUTPUT;

/**
 * A helper class that locates and connects all object trees that can be the output of an expression.
 * <br/>
 * Examples:
 * <ul>
 *     <li>{@code a} results on that variable being linked.</li>
 *     <li>{@code a ? b : c} links both {@code b} and {@code c}.</li>
 *     <li>{@code a.b} links the subtree of {@code b}.</li>
 *     <li>{@code f()} links the output of the call.</li>
 * </ul>
 * These patterns can be mixed in any order.
 * <br/>
 * <h2>Practical usage</h2>
 * This class can be used to link assignments, declarations and other statements,
 * to link, for example, the expression in {@code return a ? b : c} to the -output-
 * variable action. This linkage must be performed in the same GraphNode, so the previous
 * return would have a DEF(-output-), and that action is the target of the links setup
 * by this class.
 * <h3>Assignments, declarations</h3>
 * Generate a new object and use the method {@link #handleAssignExpr(AssignExpr, VariableAction, String) handleAssignExpr()},
 * {@link #handleVariableDeclarator(VariableDeclarator, String) handleVariableDeclarator()} or {@link #handleArrayAssignExpr(AssignExpr) handleArrayAssignExpr()}.
 * <h3>Other statements</h3>
 * Generate a new object and use either {@link #locateAndMarkTransferenceToRoot(Expression, VariableAction)}, to link to
 * the given variable action or {@link #locateAndMarkTransferenceToRoot(Expression, int)} to extract the variable action
 * from the graph node used in the constructor. This is used for return and throw statements, and actual-in/out nodes.
 */
public class ExpressionObjectTreeFinder {

    /** The node that contains both the expression to be scanned, the source variable action and
     *  the desired variable action target. */
    protected final GraphNode<?> graphNode;

    /** Creates a new ExpressionObjectTreeFinder for the given GraphNode. */
    public ExpressionObjectTreeFinder(GraphNode<?> graphNode) {
        this.graphNode = graphNode;
    }

    /** Prepares the connection between the right-hand side of a variable declarator and the variable.
     *  The variable declarator must have an initializer, and the realName indicates the absolute (fields
     *  prefixed by 'this.') name of the variable being declared. */
    public void handleVariableDeclarator(VariableDeclarator variableDeclarator, String realName) {
        if (variableDeclarator.getInitializer().isEmpty())
            throw new IllegalArgumentException("The variableDeclarator must have an initializer!");
        VariableAction targetAction = locateVAVariableDeclarator(realName);
        ClassGraph.getInstance().generateObjectTreeForType(variableDeclarator.getType().resolve())
                .ifPresent(objectTree -> targetAction.getObjectTree().addAll(objectTree));
        locateExpressionResultTrees(variableDeclarator.getInitializer().get())
                .forEach(pair -> markTransference(pair, targetAction, ""));
    }

    /** Finds the variable action that corresponds to the definition of a variable
     *  in a VariableDeclarator with initializer. */
    protected VariableAction locateVAVariableDeclarator(String realName) {
        String root = realName.contains(".") ? ObjectTree.removeFields(realName) : realName;
        boolean foundDecl = false;
        VariableAction lastDef = null;
        for (VariableAction a : graphNode.getVariableActions()) {
            if (a.isDeclaration()) {
                if (a.getName().equals(realName))
                    foundDecl = true;
                else if (foundDecl)
                    return lastDef;
            } else if (a.isDefinition() && a.getName().equals(root)) {
                if (root.equals(realName) || a.hasPolyTreeMember(realName))
                    lastDef = a;
            }
        }
        if (lastDef == null)
            throw new IllegalStateException("Could not find DEF for variable declaration of " + realName);
        return lastDef;
    }

    /** Prepares the connection between the right-hand side of an assignment to the left-hand side.
     *  The caller must provide the variable action that represents the definition of the variable action.
     *  If the LHS of this assignment is an array access expression, the method
     *  {@link #handleArrayAssignExpr(AssignExpr)} should be used. */
    public void handleAssignExpr(AssignExpr assignExpr, VariableAction assignTarget, String targetMember) {
        ClassGraph.getInstance().generateObjectTreeForType(assignExpr.getTarget().calculateResolvedType())
                .ifPresent(fields -> assignTarget.getObjectTree().addAll(fields));
        locateExpressionResultTrees(assignExpr.getValue())
                .forEach(pair -> markTransference(pair, assignTarget, targetMember));
    }

    /** Prepares the connection between the right-hand side of the assignment and the GraphNode
     *  that represents the assignment, as currently array access expressions are treated as primitives. */
    public void handleArrayAssignExpr(AssignExpr assignExpr) {
        locateExpressionResultTrees(assignExpr.getValue())
                .forEach(pair -> pair.a.setPDGValueConnection(pair.b));
    }

    /**
     * Finds the variable action corresponding to the given index in the GraphNode used in the constructor
     * and then connects each valid tree found in the given expression to the former variable action.
     * Negative indices may be used, and will access variable actions from the end of the GraphNode's list,
     * starting at -1 for the tail of the list.
     */
    public void locateAndMarkTransferenceToRoot(Expression expr, int index) {
        List<VariableAction> list = graphNode.getVariableActions();
        locateAndMarkTransferenceToRoot(expr, list.get(index < 0 ? list.size() + index : index));
    }

    /** Connects each valid tree found in the given expression to the given variable action. */
    public void locateAndMarkTransferenceToRoot(Expression expression, VariableAction targetAction) {
        locateExpressionResultTrees(expression)
                .forEach(pair -> markTransference(pair, targetAction, ""));
    }

    /**
     * Finds object trees that correspond to the output of the given expression.
     * @param expression An expression that outputs an object.
     * @return A list of pairs, containing the variable actions and the member of such
     * actions where the link must be placed.
     */
    protected List<Pair<VariableAction, String>> locateExpressionResultTrees(Expression expression) {
        List<Pair<VariableAction, String>> list = new LinkedList<>();
        expression.accept(new VoidVisitorAdapter<String>() {
            @Override
            public void visit(ArrayAccessExpr n, String arg) {
                n.getName().accept(this, arg);
            }

            @Override
            public void visit(AssignExpr n, String arg) {
                n.getValue().accept(this, arg);
            }

            @Override
            public void visit(ConditionalExpr n, String arg) {
                n.getThenExpr().accept(this, arg);
                n.getElseExpr().accept(this, arg);
            }

            @Override
            public void visit(NameExpr n, String arg) {
                ResolvedValueDeclaration resolved = n.resolve();
                if (resolved.isType())
                    return;
                if (resolved.isField() && !resolved.asField().isStatic()) {
                    new FieldAccessExpr(new ThisExpr(), n.getNameAsString()).accept(this, arg);
                    return;
                }
                for (VariableAction action : graphNode.getVariableActions()) {
                    if (action.isUsage() && action.getName().equals(n.getNameAsString())) {
                        list.add(new Pair<>(action, arg));
                        return;
                    }
                }
                throw new IllegalStateException("Cannot find USE action for var " + n);
            }

            @Override
            public void visit(ThisExpr n, String arg) {
                for (VariableAction action : graphNode.getVariableActions()) {
                    if (action.isUsage() && action.getName().matches("^.*this$")) {
                        list.add(new Pair<>(action, arg));
                        return;
                    }
                }
                throw new IllegalStateException("Could not find USE(this)");
            }

            @Override
            public void visit(FieldAccessExpr n, String arg) {
                if (!arg.isEmpty())
                    arg = "." + arg;
                n.getScope().accept(this, n.getNameAsString() + arg);
            }

            @Override
            public void visit(ObjectCreationExpr n, String arg) {
                visitCall(n, arg);
            }

            @Override
            public void visit(MethodCallExpr n, String arg) {
                visitCall(n, arg);
            }

            protected void visitCall(Resolvable<? extends ResolvedMethodLikeDeclaration> call, String arg) {
                if (ASTUtils.shouldVisitArgumentsForMethodCalls(call))
                    return;
                VariableAction lastUseOut = null;
                for (VariableAction variableAction : graphNode.getVariableActions()) {
                    if (variableAction instanceof VariableAction.CallMarker) {
                        VariableAction.CallMarker marker = (VariableAction.CallMarker) variableAction;
                        if (ASTUtils.equalsWithRange(marker.getCall(), call) && !marker.isEnter()) {
                            assert lastUseOut != null;
                            list.add(new Pair<>(lastUseOut, arg));
                            return;
                        }
                    }
                    if (variableAction.isUsage() && variableAction.getName().equals(VARIABLE_NAME_OUTPUT)) {
                        lastUseOut = variableAction;
                    }
                }
                throw new IllegalStateException("Could not find USE(-output-) corresponding to call " + call);
            }

            @Override
            public void visit(ArrayCreationExpr n, String arg) {}

            @Override
            public void visit(ArrayInitializerExpr n, String arg) {}

            @Override
            public void visit(BinaryExpr n, String arg) {}

            @Override
            public void visit(ClassExpr n, String arg) {}

            @Override
            public void visit(InstanceOfExpr n, String arg) {}

            @Override
            public void visit(UnaryExpr n, String arg) {}

            @Override
            public void visit(LambdaExpr n, String arg) {}

            @Override
            public void visit(MethodReferenceExpr n, String arg) {}

            @Override
            public void visit(PatternExpr n, String arg) {}
        }, "");
        return list;
    }

    /** Prepares a tree connection, to be applied when the PDG is built. This class is used in the construction
     *  of the CFG, and the object trees are not yet placed, so the arcs cannot be generated yet. */
    protected void markTransference(Pair<VariableAction, String> sourcePair, VariableAction targetAction, String targetMember) {
        VariableAction sourceAction = sourcePair.a;
        String sourceMember = sourcePair.b;
        if (targetAction.hasObjectTree()) {
            boolean sourceTypesInClassGraph = sourceAction.getDynamicTypes().stream()
                    .anyMatch(ClassGraph.getInstance()::containsType);
            if (sourceTypesInClassGraph && !sourceAction.hasObjectTree())
                ObjectTree.copyTargetTreeToSource(sourceAction.getObjectTree(), targetAction.getObjectTree(), sourceMember, targetMember);
            sourceAction.setPDGTreeConnectionTo(targetAction, sourceMember, targetMember);
        } else {
            sourceAction.setPDGValueConnection(sourceMember);
        }
    }
}
