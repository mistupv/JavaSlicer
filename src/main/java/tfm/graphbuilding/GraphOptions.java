package tfm.graphbuilding;

import com.github.javaparser.ast.Node;
import tfm.graphs.CFGGraph;
import tfm.graphs.Graph;
import tfm.graphs.PDGGraph;
import tfm.graphs.SDGGraph;
import tfm.visitors.cfg.CFGBuilder;
import tfm.visitors.pdg.PDGBuilder;
import tfm.visitors.sdg.SDGBuilder;

public abstract class GraphOptions<G extends Graph> {
    public abstract G empty();

    public G fromASTNode(Node node) {
        G emptyGraph = empty();

        buildGraphWithSpecificVisitor(emptyGraph, node);

        return emptyGraph;
    }

    protected abstract void buildGraphWithSpecificVisitor(G emptyGraph, Node node);
}

class CFGOptions extends GraphOptions<CFGGraph> {

    @Override
    public CFGGraph empty() {
        return new CFGGraph();
    }

    @Override
    protected void buildGraphWithSpecificVisitor(CFGGraph emptyGraph, Node node) {
        node.accept(new CFGBuilder(emptyGraph), null);
    }
}

class PDGOptions extends GraphOptions<PDGGraph> {

    @Override
    public PDGGraph empty() {
        return new PDGGraph();
    }

    @Override
    protected void buildGraphWithSpecificVisitor(PDGGraph emptyGraph, Node node) {
        node.accept(new PDGBuilder(emptyGraph), null);
    }
}

class SDGOptions extends GraphOptions<SDGGraph> {

    @Override
    public SDGGraph empty() {
        return new SDGGraph();
    }

    @Override
    protected void buildGraphWithSpecificVisitor(SDGGraph emptyGraph, Node node) {
        node.accept(new SDGBuilder(emptyGraph), null);
    }
}