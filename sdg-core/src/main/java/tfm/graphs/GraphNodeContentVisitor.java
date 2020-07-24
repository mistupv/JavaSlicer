package tfm.graphs;

import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import tfm.nodes.GraphNode;

/** A generic visitor that can be use as a basis to traverse the nodes inside
 * a given {@link GraphNode}. */
public class GraphNodeContentVisitor<A> extends VoidVisitorAdapter<A> {
    protected GraphNode<?> graphNode = null;

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
    public void visit(BlockStmt n, A arg) {}

    @Override
    public void visit(BreakStmt n, A arg) {
        n.getLabel().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(CatchClause n, A arg) {
        n.getParameter().accept(this, arg);
    }

    @Override
    public void visit(ConstructorDeclaration n, A arg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visit(ContinueStmt n, A arg) {
        n.getLabel().ifPresent(l -> l.accept(this, arg));
    }

    @Override
    public void visit(DoStmt n, A arg) {
        n.getCondition().accept(this, arg);
    }

    // TODO: this should not be part of any node, but in practice there are still
    //    synthetic nodes that rely on it instead of extending SyntheticNode.
    @Override
    public void visit(EmptyStmt n, A arg) {}

    @Override
    public void visit(EnumConstantDeclaration n, A arg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visit(EnumDeclaration n, A arg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visit(ExplicitConstructorInvocationStmt n, A arg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visit(ExpressionStmt n, A arg) {
        super.visit(n, arg);
    }

    @Override
    public void visit(FieldDeclaration n, A arg) {
        throw new UnsupportedOperationException();
    }

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
    public void visit(MethodDeclaration n, A arg) {}

    @Override
    public void visit(ReturnStmt n, A arg) {
        n.getExpression().ifPresent(e -> e.accept(this, arg));
    }

    @Override
    public void visit(SwitchEntryStmt n, A arg) {
        n.getLabel().ifPresent(l -> l.accept(this, arg));
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
    public void visit(TryStmt n, A arg) {}

    @Override
    public void visit(LocalClassDeclarationStmt n, A arg) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visit(WhileStmt n, A arg) {
        n.getCondition().accept(this, arg);
    }
}
