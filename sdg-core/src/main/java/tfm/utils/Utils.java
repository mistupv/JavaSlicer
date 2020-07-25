package tfm.utils;

import java.util.*;

public class Utils {

    public static final String PROGRAMS_FOLDER = "src/test/res/programs/";

    public static <E> List<E> emptyList() {
        return new ArrayList<>(0);
    }

    public static <E> Set<E> emptySet() {
        return new HashSet<>(0);
    }

    public static <E> E setPop(Set<E> set) {
        Iterator<E> it = set.iterator();
        E e = it.next();
        it.remove();
        return e;
    }
}
