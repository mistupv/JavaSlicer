package es.upv.mist.slicing.cli;

import es.upv.mist.slicing.graphs.ClassGraph;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.dot.DOTExporter;

import java.util.Map;

public class ClassGraphLog {
    protected DOTExporter<ClassGraph.Vertex<?>, ClassGraph.ClassArc> getDOTExporter() {
        DOTExporter<ClassGraph.Vertex<?>, ClassGraph.ClassArc> dot = new DOTExporter<>();
        dot.setVertexAttributeProvider(this::attributes);
        dot.setEdgeAttributeProvider(this::attributes);
        return dot;
    }

    protected Map<String, Attribute> attributes(ClassGraph.Vertex<?> vertex) {
        DOTAttributes res = new DOTAttributes();
        res.set("label", vertex.toString());
        return res.build();
    }

    protected Map<String, Attribute> attributes(ClassGraph.ClassArc arc) {
        DOTAttributes res = new DOTAttributes();
        return res.build();
    }
}
