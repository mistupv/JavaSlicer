package tfm.exec;

import com.github.javaparser.ast.Node;
import tfm.graphs.Graph;
import tfm.utils.Logger;

import java.io.IOException;

public abstract class GraphLog<G extends Graph<?>> {

    static final String CFG = "cfg";
    static final String PDG = "pdg";
    static final String SDG = "sdg";

    G graph;

    protected String pngName;

    public GraphLog() {

    }

    public GraphLog(G graph) {
        this.graph = graph;
    }

    public abstract void visit(Node node);


    public void log() throws IOException {
        Logger.log(
                "****************************\n" +
                "*           GRAPH          *\n" +
                "****************************"
        );
        Logger.log(graph);
        Logger.log(
                "****************************\n" +
                "*         GRAPHVIZ         *\n" +
                "****************************"
        );
        Logger.log(graph.toGraphvizRepresentation());
        Logger.log();
    }

    public abstract void generatePNGs() throws IOException;

    public abstract void generatePNGs(String pngName) throws IOException;

    public abstract void openVisualRepresentation() throws IOException;
}
