package tfm.graphbuilding;

import com.github.javaparser.ast.Node;
import tfm.graphs.CFG;
import tfm.graphs.Graph;
import tfm.graphs.PDG;
import tfm.graphs.SDG;
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

class CFGOptions extends GraphOptions<CFG> {

    @Override
    public CFG empty() {
        return new CFG();
    }

    @Override
    protected void buildGraphWithSpecificVisitor(CFG emptyGraph, Node node) {
        node.accept(new CFGBuilder(emptyGraph), null);
    }
}

class PDGOptions extends GraphOptions<PDG> {

    @Override
    public PDG empty() {
        return new PDG();
    }

    @Override
    protected void buildGraphWithSpecificVisitor(PDG emptyGraph, Node node) {
        node.accept(new PDGBuilder(emptyGraph), null);
    }
}

class SDGOptions extends GraphOptions<SDG> {

    @Override
    public SDG empty() {
        return new SDG();
    }

    @Override
    protected void buildGraphWithSpecificVisitor(SDG emptyGraph, Node node) {
        node.accept(new SDGBuilder(emptyGraph), null);
    }
}