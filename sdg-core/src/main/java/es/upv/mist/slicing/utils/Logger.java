package es.upv.mist.slicing.utils;

import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/** A static logging utility that can be redirected to multiple {@link PrintStream}s. */
public class Logger {
    protected static final List<PrintStream> printStreams = new LinkedList<>();

    static {
        printStreams.add(System.out);
    }

    public static void registerPrintStream(PrintStream ps) {
        printStreams.add(Objects.requireNonNull(ps));
    }

    public static void clearPrintStreams() {
        printStreams.clear();
    }

    public static void log() {
        log("");
    }

    public static void log(Object object) {
        log(String.valueOf(object));
    }

    public static void log(String message) {
        log("", message);
    }

    public static void log(String context, Object object) {
        log(context, String.valueOf(object));
    }

    public static void log(String context, String message) {
        printStreams.forEach(out -> out.println(
                String.format("%s%s",
                        context.isEmpty() ? "" : String.format("[%s]: ", context),
                        message
                )
        ));
    }

    public static void format(String message, Object... args) {
        log(String.format(message, args));
    }

    private Logger() {
        throw new UnsupportedOperationException("This is a static, utility class");
    }
}
