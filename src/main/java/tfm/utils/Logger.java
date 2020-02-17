package tfm.utils;

import java.util.Objects;

public class Logger {

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
        System.out.println(String.format("%s: %s", context, message));
    }

    public static void format(String message, Object... args) {
        log(String.format(message, args));
    }
}
