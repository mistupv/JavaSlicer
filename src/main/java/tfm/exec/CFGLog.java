package tfm.exec;

import com.github.javaparser.ast.Node;
import tfm.graphs.CFG;
import tfm.visitors.cfg.CFGBuilder;

public class CFGLog extends GraphLog<CFG> {

    public CFGLog() {
        super();
    }

    public CFGLog(CFG graph) {
        super(graph);
    }

    @Override
    public void visit(Node node) {
        this.graph = new CFG();
        node.accept(new CFGBuilder(graph), null);
    }
}
