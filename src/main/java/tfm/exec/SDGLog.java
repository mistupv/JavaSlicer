package tfm.exec;

import com.github.javaparser.ast.Node;
import tfm.graphs.SDGGraph;
import tfm.visitors.SDGVisitor;

import java.io.IOException;

public class SDGLog extends GraphLog<SDGGraph, SDGVisitor> {

    @Override
    void visit(Node node) {
        SDGGraph sdgGraph = new SDGGraph();

        SDGVisitor sdgVisitor = new SDGVisitor(sdgGraph);

        node.accept(sdgVisitor, null);
    }

    @Override
    void generatePNGs() throws IOException {

    }

    @Override
    void openVisualRepresentation() throws IOException {

    }
}
