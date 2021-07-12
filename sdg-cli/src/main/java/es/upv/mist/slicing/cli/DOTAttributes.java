package es.upv.mist.slicing.cli;

import org.jgrapht.nio.Attribute;

import java.util.*;

import static org.jgrapht.nio.DefaultAttribute.createAttribute;

public class DOTAttributes {
    protected Map<String, Set<String>> map = new HashMap<>();

    /** Set the value of a property to the given value. */
    public void set(String key, String value) {
        Set<String> set = new HashSet<>();
        set.add(value);
        map.put(key, set);
    }

    /** Add the given value to the list of values of the given property. */
    public void add(String key, String value) {
        map.computeIfAbsent(key, k -> new HashSet<>())
                .add(value);
    }

    /** Generate the map of attributes required by DOTExporter. */
    public Map<String, Attribute> build() {
        Map<String, Attribute> map = new HashMap<>();
        for (var entry : this.map.entrySet()) {
            if (entry.getValue() == null)
                continue;
            Optional<String> string = entry.getValue().stream().reduce((a, b) -> a + "," + b);
            string.ifPresent(s -> map.put(entry.getKey(), createAttribute(s)));
        }
        return map;
    }
}
