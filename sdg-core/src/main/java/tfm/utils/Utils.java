package tfm.utils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
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

    public static void openFileForUser(File file) throws IOException {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(file);
            return;
        }
        // Alternative manual opening of the file
        String os = System.getProperty("os.name").toLowerCase();
        String cmd = null;
        if (os.contains("win")) {
            cmd = "";
        } else if (os.contains("mac")) {
            cmd = "open";
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            cmd = "xdg-open";
        }

        if (cmd != null) {
            new ProcessBuilder(cmd, file.getAbsolutePath()).start();
        } else {
            Logger.format("Warning: cannot open file %s in your system (%s)",
                    file.getName(), os);
        }
    }
}
