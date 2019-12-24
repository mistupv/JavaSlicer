package tfm.exec;

import com.github.javaparser.ast.Node;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import tfm.graphs.CFGGraph;
import tfm.utils.FileUtil;
import tfm.visitors.cfg.CFGBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class CFGLog extends GraphLog<CFGGraph> {

    public CFGLog() {
        super();
    }

    public CFGLog(CFGGraph graph) {
        super(graph);
    }

    @Override
    public void visit(Node node) {
        this.graph = new CFGGraph();
        node.accept(new CFGBuilder(graph), null);
    }
}
