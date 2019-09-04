package tfm.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Utils {

    public static final String PROGRAMS_FOLDER = "src/main/java/tfm/programs/";

    public static <E> List<E> emptyList() {
        return new ArrayList<>(0);
    }

    public static <E> Set<E> emptySet() {
        return new HashSet<>(0);
    }
}
