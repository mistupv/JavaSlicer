package es.upv.mist.slicing.util;

import java.util.stream.Collector;
import java.util.stream.Collectors;

public class SingletonCollector {
    public static <T> Collector<T, ?, T> toSingleton() {
        return Collectors.collectingAndThen(
                Collectors.toList(),
                list -> {
                    if (list.size() != 1) {
                        throw new IllegalStateException("Stream should contain exactly one element");
                    }
                    return list.get(0);
                }
        );
    }
}
