package tfm.exec;

import com.github.javaparser.ast.Node;
import tfm.graphs.CFGGraph;
import tfm.visitors.cfg.CFGBuilder;

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
