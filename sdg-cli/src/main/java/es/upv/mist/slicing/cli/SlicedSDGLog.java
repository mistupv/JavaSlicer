package es.upv.mist.slicing.cli;

import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.graphs.sdg.SDG;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.slicing.Slice;
import es.upv.mist.slicing.slicing.SlicingCriterion;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/** Utility to export a sliced SDG in dot and show the slices and slicing criterion. */
public class SlicedSDGLog extends SDGLog {
    protected final Slice slice;
    protected final Set<GraphNode<?>> sc;

    public SlicedSDGLog(SDG graph, Slice slice) {
        this(graph, slice, null);
    }

    public SlicedSDGLog(SDG graph, Slice slice, SlicingCriterion sc) {
        super(graph);
        this.slice = slice;
        Set<GraphNode<?>> set = null;
        try {
            set = sc.findNode(graph);
        } catch (NullPointerException | NoSuchElementException ignored) {}
        this.sc = set;
    }

    @Override
    protected DOTExporter<GraphNode<?>, Arc> getDOTExporter(SDG graph) {
        DOTExporter<GraphNode<?>, Arc> dot = new DOTExporter<>();
        dot.setVertexIdProvider(n -> String.valueOf(n.getId()));
        dot.setVertexAttributeProvider(this::vertexAttributes);
        dot.setEdgeAttributeProvider(Arc::getDotAttributes);
        return dot;
    }

    protected Map<String, Attribute> vertexAttributes(GraphNode<?> node) {
        Map<String, Attribute> map = new HashMap<>();
        if (slice.contains(node) && sc.contains(node))
            map.put("style", DefaultAttribute.createAttribute("filled,bold"));
        else if (slice.contains(node) && node.isImplicitInstruction())
            map.put("style", DefaultAttribute.createAttribute("filled,dashed"));
        else if (slice.contains(node))
            map.put("style", DefaultAttribute.createAttribute("filled"));
        else if (sc.contains(node))
            map.put("style", DefaultAttribute.createAttribute("bold"));
        else if (node.isImplicitInstruction())
            map.put("style", DefaultAttribute.createAttribute("dashed"));
        map.put("label", DefaultAttribute.createAttribute(node.getLongLabel()));
        return map;
    }
}
