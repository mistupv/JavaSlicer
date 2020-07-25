package tfm.utils;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class FileUtil {
    public static void open(String path) throws IOException {
        open(new File(path));
    }

    public static void open(File file) throws IOException {
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
