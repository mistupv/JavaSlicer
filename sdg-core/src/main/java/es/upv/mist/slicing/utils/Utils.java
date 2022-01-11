package es.upv.mist.slicing.utils;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

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

    /** Find the matching element in the set and return it.  */
    public static <E> E setGet(Set<E> set, E object) {
        for (E element : set)
            if (element.hashCode() == object.hashCode() && Objects.equals(element, object))
                return element;
        throw new NoSuchElementException("Could not locate " + object + " in set.");
    }

    public static String arrayJoin(String[] array, String separator) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            if (i > 0) builder.append(separator);
            builder.append(array[i]);
        }
        return builder.toString();
    }

    public static <E> boolean arrayPrefix(E[] array, E[] prefix) {
        if (prefix.length == 0)
            return true;
        if (array.length < prefix.length)
            return false;
        for (int i = 0; i < prefix.length; i++)
            if (!Objects.equals(array[i], prefix[i]))
                return false;
        return true;
    }
}
