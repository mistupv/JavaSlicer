package tfm.exec;

import com.github.javaparser.ast.Node;
import tfm.graphs.SDG;
import tfm.visitors.sdg.SDGBuilder;

public class SDGLog extends GraphLog<SDG> {

    @Override
    public void visit(Node node) {
        this.graph = new SDG();
        SDGBuilder sdgBuilder = new SDGBuilder(this.graph);
        node.accept(sdgBuilder, null);
    }
}
