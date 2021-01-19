package es.upv.mist.slicing.utils;

import java.util.*;

/**
 * A customized abstract HashSet, which allows for customization of the equality
 * method used to compare objects. To create a child class, just implement the
 * abstract methods.
 */
public abstract class CustomEqualityHashSet<T> extends AbstractSet<T> {
    /** The underlying collection of objects, each list acts as bucket for colliding hash codes. */
    protected final Map<Integer, List<T>> map;

    protected int size = 0;

    public CustomEqualityHashSet() {
        map = new HashMap<>();
    }

    public CustomEqualityHashSet(int initialCapacity) {
        map = new HashMap<>(initialCapacity);
    }

    public CustomEqualityHashSet(int initialCapacity, float loadFactor) {
        map = new HashMap<>(initialCapacity, loadFactor);
    }

    /** Compares two objects to determine equality. The objects are assumed to be of type {@link T}. */
    protected abstract boolean objEquals(T a, Object b);

    /** Checks whether the argument is of a compatible class with the set. A
     *  default implementation could check against {@link Object}. */
    protected abstract boolean objInstanceOf(Object o);

    @Override
    public boolean add(T t) {
        Objects.requireNonNull(t);
        List<T> bucket = map.computeIfAbsent(t.hashCode(), i -> new LinkedList<>());
        if (contains(t, bucket))
            return false;
        bucket.add(t);
        size++;
        return true;
    }

    @Override
    public boolean remove(Object o) {
        if (!objInstanceOf(o))
            return false;
        List<T> bucket = map.get(o.hashCode());
        if (bucket == null)
            return false;
        Iterator<T> it = bucket.iterator();
        while (it.hasNext()) {
            T t = it.next();
            if (objEquals(t, o)) {
                it.remove();
                size--;
                return true;
            }
        }
        return false; // matching element not found
    }

    @Override
    public boolean contains(Object o) {
        List<T> bucket = map.get(o.hashCode());
        if (bucket == null)
            return false;
        return contains(o, bucket);
    }

    /** Whether the given bucket contains the given object. */
    protected boolean contains(Object o, List<T> bucket) {
        if (!objInstanceOf(o))
            return false;
        for (T element : bucket)
            if (objEquals(element, o))
                return true;
        return false;
    }

    @Override
    public void clear() {
        map.clear();
        size = 0;
    }

    @Override
    public Iterator<T> iterator() {
        Iterator<T> it = map.values().stream().flatMap(Collection::stream).iterator();
        return new Iterator<>() {
            T last = null;

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public T next() {
                return last = it.next();
            }

            @Override
            public void remove() {
                if (last != null)
                    CustomEqualityHashSet.this.remove(last);
            }
        };
    }

    @Override
    public int size() {
        return size;
    }
}
