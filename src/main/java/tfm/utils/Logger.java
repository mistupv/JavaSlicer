package tfm.utils;

import java.util.Objects;

public class Logger {

    public static void log() {
        log("");
    }

    public static void log(Object object) {
        log(Objects.toString(object));
    }

    public static void log(String message) {
        System.out.println(message);
    }

    public static void format(String message, Object... args) {
        System.out.printf(message, args);
    }
}
