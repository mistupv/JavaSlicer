package tfm.graphbuilding;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import tfm.graphs.CFG;
import tfm.graphs.Graph;
import tfm.graphs.PDG;
import tfm.graphs.SDG;

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
        if (node instanceof MethodDeclaration) {
            emptyGraph.build((MethodDeclaration) node);
        } else {
            for (Node n : node.getChildNodes())
                buildGraphWithSpecificVisitor(emptyGraph, n);
        }
    }
}

class PDGOptions extends GraphOptions<PDG> {

    @Override
    public PDG empty() {
        return new PDG();
    }

    @Override
    protected void buildGraphWithSpecificVisitor(PDG emptyGraph, Node node) {
        if (node instanceof MethodDeclaration) {
            emptyGraph.build((MethodDeclaration) node);
        } else {
            for (Node n : node.getChildNodes())
                buildGraphWithSpecificVisitor(emptyGraph, n);
        }
    }
}

class SDGOptions extends GraphOptions<SDG> {

    @Override
    public SDG empty() {
        return new SDG();
    }

    @Override
    protected void buildGraphWithSpecificVisitor(SDG emptyGraph, Node node) {
        if (node instanceof CompilationUnit)
            emptyGraph.build(new NodeList<>(((CompilationUnit) node)));
        else
            throw new IllegalStateException("The node needs to be a CompilationUnit");
    }
}