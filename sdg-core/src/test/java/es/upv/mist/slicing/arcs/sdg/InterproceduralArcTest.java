package es.upv.mist.slicing.arcs.sdg;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.NodeList;
import es.upv.mist.slicing.graphs.jsysdg.JSysDG;
import es.upv.mist.slicing.graphs.sdg.SDG;
import es.upv.mist.slicing.slicing.SlicingCriterion;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.FileNotFoundException;

public class InterproceduralArcTest {
    @ParameterizedTest(name = "{0}")
    @MethodSource("es.upv.mist.slicing.SlicerTest#findAllFiles")
    public void interproceduralArcsMustBeInputXorOutputTest(File source, File target, SlicingCriterion sc) throws FileNotFoundException {
        if (!target.exists())
            return;
        SDG sdg;
        sdg = new JSysDG();
        sdg.build(new NodeList<>(StaticJavaParser.parse(source)));
        sdg.edgeSet().stream()
                .filter(InterproceduralArc.class::isInstance)
                .map(InterproceduralArc.class::cast)
                .forEach(arc -> {
                    assert arc.isInterproceduralInputArc() || arc.isInterproceduralOutputArc();
                    assert !arc.isInterproceduralInputArc() || !arc.isInterproceduralOutputArc();
                });
    }
}
