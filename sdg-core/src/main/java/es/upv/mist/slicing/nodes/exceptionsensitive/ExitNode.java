package es.upv.mist.slicing.nodes.exceptionsensitive;

import com.github.javaparser.ast.body.CallableDeclaration;
import es.upv.mist.slicing.nodes.SyntheticNode;

import java.util.LinkedList;

/** A node that summarizes the normal or exceptional exits of a declaration. */
public abstract class ExitNode extends SyntheticNode<CallableDeclaration> {
    protected ExitNode(String label, CallableDeclaration<?> astNode) {
        super(label, astNode, new LinkedList<>());
    }
}
