package es.upv.mist.slicing.cli;

import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.graphs.sdg.SDG;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.slicing.Slice;
import es.upv.mist.slicing.slicing.SlicingCriterion;
import org.jgrapht.io.Attribute;
import org.jgrapht.io.DOTExporter;
import org.jgrapht.io.DefaultAttribute;

import java.util.HashMap;
import java.util.Map;

/** Utility to export a sliced SDG in dot and show the slices and slicing criterion. */
public class SlicedSDGLog extends SDGLog {
    protected final Slice slice;
    protected final GraphNode<?> sc;

    public SlicedSDGLog(SDG graph, Slice slice) {
        this(graph, slice, null);
    }

    public SlicedSDGLog(SDG graph, Slice slice, SlicingCriterion sc) {
        super(graph);
        this.slice = slice;
        this.sc = sc == null ? null : sc.findNode(graph).orElse(null);
    }

    @Override
    protected DOTExporter<GraphNode<?>, Arc> getDOTExporter(SDG graph) {
        return new DOTExporter<>(
                n -> String.valueOf(n.getId()),
                n -> {
                    String s = n.getId() + ": " + n.getLabel();
                    if (!n.getVariableActions().isEmpty())
                        s += "\n" + n.getVariableActions().stream().map(Object::toString).reduce((a, b) -> a + "," + b).orElse("--");
                    return s;
                },
                Arc::getLabel,
                this::vertexAttributes,
                Arc::getDotAttributes);
    }

    protected Map<String, Attribute> vertexAttributes(GraphNode<?> node) {
        Map<String, Attribute> map = new HashMap<>();
        if (slice.contains(node) && node.equals(sc))
            map.put("style", DefaultAttribute.createAttribute("filled,bold"));
        else if (slice.contains(node))
            map.put("style", DefaultAttribute.createAttribute("filled"));
        else if (node.equals(sc))
            map.put("style", DefaultAttribute.createAttribute("bold"));
        return map;
    }
}
