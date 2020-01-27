package tfm.slicing;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithBody;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;

import java.util.stream.Collectors;

public class SliceAstVisitor extends ModifierVisitor<Slice> {
    @Override
    public Visitable visit(BreakStmt n, Slice arg) {
        return arg.contains(n) ? n : null;
    }

    @Override
    public Visitable visit(ContinueStmt n, Slice arg) {
        return arg.contains(n) ? n : null;
    }

    @Override
    public Visitable visit(DoStmt n, Slice arg) {
        boolean keep = arg.contains(n);
        super.visit(n, arg);
        fillBody(n);
        return keep ? n : null;
    }

    @Override
    public Visitable visit(ForEachStmt n, Slice arg) {
        boolean keep = arg.contains(n);
        super.visit(n, arg);
        fillBody(n);
        return keep ? n : null;
    }

    @Override
    public Visitable visit(ForStmt n, Slice arg) {
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
    public Visitable visit(WhileStmt n, Slice arg) {
        boolean keep = arg.contains(n);
        super.visit(n, arg);
        fillBody(n);
        return keep ? n : null;
    }

    @Override
    public Visitable visit(IfStmt n, Slice arg) {
        boolean keep = arg.contains(n);
        super.visit(n, arg);
        if (n.getThenStmt() == null)
            n.setThenStmt(new EmptyStmt());
        return keep ? n : null;
    }

    @Override
    public Visitable visit(LabeledStmt n, Slice arg) {
        super.visit(n, arg);
        return n.getStatement() != null ? n : null;
    }

    @Override
    public Visitable visit(ReturnStmt n, Slice arg) {
        return arg.contains(n) ? n : null;
    }

    @Override
    public Visitable visit(ThrowStmt n, Slice arg) {
        return arg.contains(n) ? n : null;
    }

    @Override
    public Visitable visit(SwitchEntryStmt n, Slice arg) {
        boolean keep = arg.contains(n);
        super.visit(n, arg);
        if (!n.getStatements().isEmpty())
            return n;
        return keep ? n : null;
    }

    @Override
    public Visitable visit(SwitchStmt n, Slice arg) {
        boolean keep = arg.contains(n);
        super.visit(n, arg);
        return keep ? n : null;
    }

    @Override
    public Visitable visit(ExpressionStmt n, Slice arg) {
        return arg.contains(n) ? n : null;
    }

    private void fillBody(Node n) {
        if (!(n instanceof NodeWithBody))
            return;
        NodeWithBody<?> nb = ((NodeWithBody<?>) n);
        if (nb.getBody() == null)
            nb.setBody(new EmptyStmt());
    }
}
