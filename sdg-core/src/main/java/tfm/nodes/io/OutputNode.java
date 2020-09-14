package tfm.nodes.io;

import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import tfm.nodes.SyntheticNode;

import java.util.LinkedList;

/** A node that represents the value returned from a call. */
public class OutputNode<T extends CallableDeclaration<T>> extends SyntheticNode<T> {
    public OutputNode(T astNode) {
        super("method output", astNode, new LinkedList<>());
    }

    public static OutputNode<?> create(CallableDeclaration<?> declaration) {
        if (declaration instanceof MethodDeclaration)
            return new OutputNode<>((MethodDeclaration) declaration);
        else if (declaration instanceof ConstructorDeclaration)
            return new OutputNode<>((ConstructorDeclaration) declaration);
        throw new IllegalArgumentException("Callable declaration was of an unknown type");
    }
}
