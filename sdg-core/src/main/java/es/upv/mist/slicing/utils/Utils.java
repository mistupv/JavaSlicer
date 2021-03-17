package es.upv.mist.slicing.utils;

import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;

import java.util.*;

/** General utilities. */
public class Utils {
    private Utils() {
        throw new UnsupportedOperationException("This is a static, utility class");
    }

    /** Get and remove one element from a set. This can be used with the
     * {@link java.util.LinkedHashSet} to implement a queue without repetition. */
    public static <E> E setPop(Set<E> set) {
        Iterator<E> it = set.iterator();
        E e = it.next();
        it.remove();
        return e;
    }

    public static Map<String, Attribute> dotLabel(String label) {
        Map<String, Attribute> map = new HashMap<>();
        if (label != null)
            map.put("label", DefaultAttribute.createAttribute(label));
        return map;
    }

    /** Find the matching element in the set and return it.  */
    public static <E> E setGet(Set<E> set, E object) {
        for (E element : set)
            if (element.hashCode() == object.hashCode() && Objects.equals(element, object))
                return element;
        throw new NoSuchElementException("Could not locate " + object + " in set.");
    }
}
