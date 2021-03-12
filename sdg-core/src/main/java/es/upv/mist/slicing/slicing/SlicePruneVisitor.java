package es.upv.mist.slicing.slicing;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithBody;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import es.upv.mist.slicing.utils.NodeHashSet;

import java.util.stream.Collectors;

/** Given an AST tree and a slice, removes or prunes all nodes that are not
 *  included in the slice. Some nodes are included if they are present in the slice
 *  or of any of their children are (as is the case with {@link CompilationUnit}s). */
public class SlicePruneVisitor extends ModifierVisitor<NodeHashSet<Node>> {
    // ========== Utility methods ==========

    /** Place a valid placeholder in this node's body, if there is none. */
    protected void fillBody(NodeWithBody<?> n) {
        if (n.getBody() == null)
            n.setBody(new EmptyStmt());
    }

    // ========== File visitors ==========

    @Override
    public Visitable visit(CompilationUnit n, NodeHashSet<Node> arg) {
        boolean keep = arg.contains(n);
        Visitable v = super.visit(n, arg);
        return keep || !((Node) v).getChildNodes().isEmpty() ? v : null;
    }

    @Override
    public Visitable visit(ClassOrInterfaceDeclaration n, NodeHashSet<Node> arg) {
        boolean keep = arg.contains(n);
        Visitable v = super.visit(n, arg);
        boolean containsDeclarations = ((Node) v).getChildNodes().stream().anyMatch(BodyDeclaration.class::isInstance);
        return keep || containsDeclarations ? v : null;
    }

    // ========== Class body visitors ==========

    @Override
    public Visitable visit(MethodDeclaration n, NodeHashSet<Node> arg) {
        boolean keep = arg.contains(n);
        Visitable v = super.visit(n, arg);
        return keep ? v : null;
    }

    @Override
    public Visitable visit(ConstructorDeclaration n, NodeHashSet<Node> arg) {
        boolean keep = arg.contains(n);
        Visitable v = super.visit(n, arg);
        return keep ? v : null;
    }

    @Override
    public Visitable visit(FieldDeclaration n, NodeHashSet<Node> arg) {
        return arg.contains(n) ? n : null;
    }

// ========== Method body visitors ==========
    // 3 alternatives:
    //      a. Without relevant children and included if on the slice or not (e.g. ExpressionStmt)
    //      b. With relevant children and included if of the slice or not, children are discarded if not included (e.g. WhileStmt)
    //      c. With relevant children and included if any children is included OR if on the slice (e.g. SwitchEntryStmt, LabeledStmt)

    @Override
    public Visitable visit(BreakStmt n, NodeHashSet<Node> arg) {
        return arg.contains(n) ? n : null;
    }

    @Override
    public Visitable visit(ContinueStmt n, NodeHashSet<Node> arg) {
        return arg.contains(n) ? n : null;
    }

    @Override
    public Visitable visit(DoStmt n, NodeHashSet<Node> arg) {
        boolean keep = arg.contains(n);
        super.visit(n, arg);
        fillBody(n);
        return keep ? n : null;
    }

    @Override
    public Visitable visit(ForEachStmt n, NodeHashSet<Node> arg) {
        boolean keep = arg.contains(n);
        super.visit(n, arg);
        fillBody(n);
        return keep ? n : null;
    }

    @Override
    public Visitable visit(ForStmt n, NodeHashSet<Node> arg) {
        boolean keep = arg.contains(n);
        super.visit(n, arg);
        n.setInitialization(new NodeList<>(n.getInitialization().stream()
                .filter(arg::contains).collect(Collectors.toList())));
        n.setUpdate(new NodeList<>(n.getUpdate().stream()
                .filter(arg::contains).collect(Collectors.toList())));
        fillBody(n);
        if (keep)
            return n;
        if (n.getInitialization().isEmpty() && n.getUpdate().isEmpty())
            return null;
        return new ForStmt(n.getInitialization(), new BooleanLiteralExpr(false),
                n.getUpdate(), n.getBody());
    }

    @Override
    public Visitable visit(WhileStmt n, NodeHashSet<Node> arg) {
        boolean keep = arg.contains(n);
        super.visit(n, arg);
        fillBody(n);
        return keep ? n : null;
    }

    @Override
    public Visitable visit(IfStmt n, NodeHashSet<Node> arg) {
        boolean keep = arg.contains(n);
        super.visit(n, arg);
        if (n.getThenStmt() == null)
            n.setThenStmt(new EmptyStmt());
        return keep ? n : null;
    }

    @Override
    public Visitable visit(LabeledStmt n, NodeHashSet<Node> arg) {
        super.visit(n, arg);
        return n.getStatement() != null ? n : null;
    }

    @Override
    public Visitable visit(ReturnStmt n, NodeHashSet<Node> arg) {
        return arg.contains(n) ? n : null;
    }

    @Override
    public Visitable visit(ThrowStmt n, NodeHashSet<Node> arg) {
        return arg.contains(n) ? n : null;
    }

    @Override
    public Visitable visit(SwitchEntry n, NodeHashSet<Node> arg) {
        boolean keep = arg.contains(n);
        super.visit(n, arg);
        if (!n.getStatements().isEmpty())
            return n;
        return keep ? n : null;
    }

    @Override
    public Visitable visit(SwitchStmt n, NodeHashSet<Node> arg) {
        boolean keep = arg.contains(n);
        super.visit(n, arg);
        return keep ? n : null;
    }

    @Override
    public Visitable visit(ExpressionStmt n, NodeHashSet<Node> arg) {
        return arg.contains(n) ? n : null;
    }

    @Override
    public Visitable visit(ExplicitConstructorInvocationStmt n, NodeHashSet<Node> arg) {
        return arg.contains(n) ? n : null;
    }

    @Override
    public Visitable visit(TryStmt n, NodeHashSet<Node> arg) {
        boolean keep = arg.contains(n);
        super.visit(n, arg);
        return keep ? n : null;
    }

    @Override
    public Visitable visit(CatchClause n, NodeHashSet<Node> arg) {
        boolean keep = arg.contains(n);
        super.visit(n, arg);
        return keep ? n : null;
    }
}
