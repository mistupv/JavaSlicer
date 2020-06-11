package tfm.slicing;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithBody;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;

import java.util.Set;
import java.util.stream.Collectors;

public class SlicePruneVisitor extends ModifierVisitor<Set<Node>> {
    // ========== Utility methods ==========

    protected void fillBody(Node n) {
        if (!(n instanceof NodeWithBody))
            return;
        NodeWithBody<?> nb = ((NodeWithBody<?>) n);
        if (nb.getBody() == null)
            nb.setBody(new EmptyStmt());
    }

    // ========== File visitors ==========

    @Override
    public Visitable visit(CompilationUnit n, Set<Node> arg) {
        boolean keep = arg.contains(n);
        Visitable v = super.visit(n, arg);
        return keep ? v : null;
    }

    @Override
    public Visitable visit(ClassOrInterfaceDeclaration n, Set<Node> arg) {
        boolean keep = arg.contains(n);
        Visitable v = super.visit(n, arg);
        return keep ? v : null;
    }

    // ========== Class body visitors ==========

    @Override
    public Visitable visit(MethodDeclaration n, Set<Node> arg) {
        boolean keep = arg.contains(n);
        Visitable v = super.visit(n, arg);
        return keep ? v : null;
    }

    @Override
    public Visitable visit(ConstructorDeclaration n, Set<Node> arg) {
        boolean keep = arg.contains(n);
        Visitable v = super.visit(n, arg);
        return keep ? v : null;
    }

    @Override
    public Visitable visit(FieldDeclaration n, Set<Node> arg) {
        boolean keep = arg.contains(n);
        Visitable v = super.visit(n, arg);
        return keep ? v : null;
    }

// ========== Method body visitors ==========
    // 3 alternatives:
    //      a. Without relevant children and included if on the slice or not (e.g. ExpressionStmt)
    //      b. With relevant children and included if of the slice or not, children are discarded if not included (e.g. WhileStmt)
    //      c. With relevant children and included if any children is included OR if on the slice (e.g. SwitchEntryStmt, LabeledStmt)

    @Override
    public Visitable visit(BreakStmt n, Set<Node> arg) {
        return arg.contains(n) ? n : null;
    }

    @Override
    public Visitable visit(ContinueStmt n, Set<Node> arg) {
        return arg.contains(n) ? n : null;
    }

    @Override
    public Visitable visit(DoStmt n, Set<Node> arg) {
        boolean keep = arg.contains(n);
        super.visit(n, arg);
        fillBody(n);
        return keep ? n : null;
    }

    @Override
    public Visitable visit(ForEachStmt n, Set<Node> arg) {
        boolean keep = arg.contains(n);
        super.visit(n, arg);
        fillBody(n);
        return keep ? n : null;
    }

    @Override
    public Visitable visit(ForStmt n, Set<Node> arg) {
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
    public Visitable visit(WhileStmt n, Set<Node> arg) {
        boolean keep = arg.contains(n);
        super.visit(n, arg);
        fillBody(n);
        return keep ? n : null;
    }

    @Override
    public Visitable visit(IfStmt n, Set<Node> arg) {
        boolean keep = arg.contains(n);
        super.visit(n, arg);
        if (n.getThenStmt() == null)
            n.setThenStmt(new EmptyStmt());
        return keep ? n : null;
    }

    @Override
    public Visitable visit(LabeledStmt n, Set<Node> arg) {
        super.visit(n, arg);
        return n.getStatement() != null ? n : null;
    }

    @Override
    public Visitable visit(ReturnStmt n, Set<Node> arg) {
        return arg.contains(n) ? n : null;
    }

    @Override
    public Visitable visit(ThrowStmt n, Set<Node> arg) {
        return arg.contains(n) ? n : null;
    }

    @Override
    public Visitable visit(SwitchEntryStmt n, Set<Node> arg) {
        boolean keep = arg.contains(n);
        super.visit(n, arg);
        if (!n.getStatements().isEmpty())
            return n;
        return keep ? n : null;
    }

    @Override
    public Visitable visit(SwitchStmt n, Set<Node> arg) {
        boolean keep = arg.contains(n);
        super.visit(n, arg);
        return keep ? n : null;
    }

    @Override
    public Visitable visit(ExpressionStmt n, Set<Node> arg) {
        return arg.contains(n) ? n : null;
    }
}
