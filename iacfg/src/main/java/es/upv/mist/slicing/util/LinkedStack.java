package es.upv.mist.slicing.util;

import java.util.Objects;

/** A stack implemented with a single linked list
 *  that does not modify its internals, but return
 *  pointers to new stacks (or states of the stack)
 *  to implement stack copying efficiently.
 * @param <E> The type of element contained in the stack
 */
public class LinkedStack<E> {
    private final E element;
    private final LinkedStack<E> next;

    /**
     * Creates a new, empty stack.
     */
    public LinkedStack() {
        this(null, null);
    }

    private LinkedStack(E element, LinkedStack<E> next) {
        this.element = element;
        this.next = next;
    }

    /**
     * Adds a new element to the stack. Should be used as: <code>stack = stack.push(element)</code>,
     * as it does not modify this object, but create a new one.
     * @param element The element to be added.
     * @return The new state of the stack. Should always be used, otherwise this method has no effect.
     */
    public LinkedStack<E> push(E element) {
        Objects.requireNonNull(element);
        return new LinkedStack<>(element, this);
    }

    /**
     * Removes the top element from this stack. Should be used as: <code>stack = stack.pop()</code>.
     * To see the element that has been popped, the user should run {@link #peek()} before this method.
     * @return The new state of the stack, with one fewer element. Should always be used,
     * otherwise this method has no effect.
     */
    public LinkedStack<E> pop() {
        return next;
    }

    /** Obtains the element at the top of the stack. */
    public E peek() {
        return element;
    }

    /** Whether this stack is empty. */
    public boolean isEmpty() {
        return element == null && next == null;
    }
}
