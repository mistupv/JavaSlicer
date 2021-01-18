package es.upv.mist.slicing.utils;

import com.github.javaparser.ast.Node;

/**
 * A HashSet that implements an appropriate equals method ({@link ASTUtils#equalsWithRangeInCU(Node, Node)}).
 * For a set that relies on object identity (==), use {@link ASTUtils#newIdentityHashSet()}.
 * @param <T> The specific subclass of Node.
 */
public class NodeHashSet<T extends Node> extends CustomEqualityHashSet<T> {
    public NodeHashSet() {
        super();
    }

    public NodeHashSet(int initialCapacity) {
        super(initialCapacity);
    }

    public NodeHashSet(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    @Override
    protected boolean objEquals(T a, Object b) {
        return ASTUtils.equalsWithRangeInCU(a, (Node) b);
    }

    @Override
    protected boolean objInstanceOf(Object o) {
        return o instanceof Node;
    }
}
