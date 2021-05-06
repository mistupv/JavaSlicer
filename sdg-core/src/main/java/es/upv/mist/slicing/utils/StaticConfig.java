package es.upv.mist.slicing.utils;

import java.io.IOException;
import java.util.Properties;

public class StaticConfig {
    public static final int K_LIMIT;

    static {
        int kLimit;
        try {
            Properties p = new Properties();
            p.load(StaticConfig.class.getResourceAsStream("sdg.properties"));
            kLimit = Integer.parseInt((String) p.get("kLimit"));
        } catch (IOException e) {
            e.printStackTrace();
            kLimit = 10;
        }
        K_LIMIT = kLimit;
    }
}
