package tfm.exec;

import com.github.javaparser.ast.Node;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import tfm.graphs.Graph;
import tfm.utils.FileUtil;
import tfm.utils.Logger;

import java.io.File;
import java.io.IOException;

public abstract class GraphLog<G extends Graph> {

    static final String CFG = "cfg";
    static final String PDG = "pdg";
    static final String SDG = "sdg";

    G graph;

    protected String imageName;
    protected Format format;
    protected boolean generated = false;

    public GraphLog() {
        this(null);
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

    public void generateImages() throws IOException {
        generateImages("graph");
    }

    public void generateImages(String imageName) throws IOException {
        generateImages(imageName, Format.PNG);
    }

    public void generateImages(String imageName, Format format) throws IOException {
        this.imageName = imageName;
        this.format = format;
        generated = true;
        Graphviz.fromString(graph.toGraphvizRepresentation())
                .render(format)
                .toFile(getImageFile());
    }

    public void openVisualRepresentation() throws IOException {
        if (!generated) generateImages();
        FileUtil.open(getImageFile());
    }

    protected File getImageFile() {
        return new File("./out/" + imageName + "." + format.name());
    }
}
