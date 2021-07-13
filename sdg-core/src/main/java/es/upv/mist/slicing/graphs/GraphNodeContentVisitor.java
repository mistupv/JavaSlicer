package es.upv.mist.slicing.graphs;

import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import es.upv.mist.slicing.nodes.GraphNode;

/** A generic visitor that can be use as a basis to traverse the nodes inside a given {@link GraphNode}.
 *  @see #startVisit(GraphNode) */
public class GraphNodeContentVisitor<A> extends VoidVisitorAdapter<A> {
    protected GraphNode<?> graphNode = null;

    /**
     * An entry-point to this visitor. The use of this class via
     * {@link com.github.javaparser.ast.Node#accept(VoidVisitor, Object) Node.accept(VoidVisitor, Object)}
     * is not supported and may result in a {@link NullPointerException} being thrown
     */
    public final void startVisit(GraphNode<?> graphNode, A arg) {
        this.graphNode = graphNode;
        graphNode.getAstNode().accept(this, arg);
        this.graphNode = null;
    }

    public void startVisit(GraphNode<?> graphNode) {
        startVisit(graphNode, null);
    }

    @Override
    public void visit(AssertStmt n, A arg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visit(BlockStmt n, A arg) {
        // A node representing a block has no relevant elements to be visited.
    }

    @Override
    public void visit(BreakStmt n, A arg) {
        n.getLabel().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(CatchClause n, A arg) {
        n.getParameter().accept(this, arg);
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, A arg) {
        // A node representing a class or interface declaration has no relevant elements to be visited.
    }

    @Override
    public void visit(ConstructorDeclaration n, A arg) {
        // A node representing a constructor declaration has no relevant elements to be visited.
    }

    @Override
    public void visit(ContinueStmt n, A arg) {
        n.getLabel().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(DoStmt n, A arg) {
        n.getCondition().accept(this, arg);
    }

    @Override
    public void visit(EmptyStmt n, A arg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visit(EnumConstantDeclaration n, A arg) {
        n.getArguments().accept(this, arg);
    }

    @Override
    public void visit(EnumDeclaration n, A arg) {
        // This node should not contain other elements
    }

    @Override
    public void visit(ExplicitConstructorInvocationStmt n, A arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(ExpressionStmt n, A arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(FieldDeclaration n, A arg) { super.visit(n, arg); }

    @Override
    public void visit(ForEachStmt n, A arg) {
        n.getIterable().accept(this, arg);
        n.getVariable().accept(this, arg);
    }

    @Override
    public void visit(ForStmt n, A arg) {
        n.getCompare().ifPresent(c -> c.accept(this, arg));
    }

    @Override
    public void visit(IfStmt n, A arg) {
        n.getCondition().accept(this, arg);
    }

    @Override
    public void visit(LabeledStmt n, A arg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visit(MethodDeclaration n, A arg) {
        // A node representing a method declaration has no relevant elements to be visited.
    }

    @Override
    public void visit(ReturnStmt n, A arg) {
        n.getExpression().ifPresent(e -> e.accept(this, arg));
    }

    @Override
    public void visit(SwitchEntry n, A arg) {
        n.getLabels().forEach(l -> l.accept(this, arg));
    }

    @Override
    public void visit(SwitchStmt n, A arg) {
        n.getSelector().accept(this, arg);
    }

    @Override
    public void visit(SynchronizedStmt n, A arg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visit(ThrowStmt n, A arg) {
        n.getExpression().accept(this, arg);
    }

    @Override
    public void visit(TryStmt n, A arg) {
        // A node representing a try statement has no relevant elements to be visited.
    }

    @Override
    public void visit(LocalClassDeclarationStmt n, A arg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visit(WhileStmt n, A arg) {
        n.getCondition().accept(this, arg);
    }
}
