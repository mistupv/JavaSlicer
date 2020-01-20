package tfm.exec;

import com.github.javaparser.ast.Node;
import tfm.graphs.SDGGraph;
import tfm.visitors.sdg.SDGBuilder;

public class SDGLog extends GraphLog<SDGGraph> {

    @Override
    public void visit(Node node) {
        this.graph = new SDGGraph();
        SDGBuilder sdgBuilder = new SDGBuilder(this.graph);
        node.accept(sdgBuilder, null);
    }
}
