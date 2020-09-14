package es.upv.mist.slicing.nodes.exceptionsensitive;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import es.upv.mist.slicing.nodes.SyntheticNode;

import java.util.LinkedList;

/** A node that represents the return from a call, either with or without thrown exceptions. */
public abstract class ReturnNode extends SyntheticNode<Node> {
    protected ReturnNode(String label, MethodCallExpr astNode) {
        super(label, astNode, new LinkedList<>());
    }

    protected ReturnNode(String label, ObjectCreationExpr astNode) {
        super(label, astNode, new LinkedList<>());
    }

    protected ReturnNode(String label, ExplicitConstructorInvocationStmt astNode) {
        super(label, astNode, new LinkedList<>());
    }
}
