package tfm.exec;

import com.github.javaparser.ast.Node;
import org.jgrapht.io.ComponentNameProvider;
import org.jgrapht.io.DOTExporter;
import org.jgrapht.io.ExportException;
import org.jgrapht.io.GraphExporter;
import tfm.arcs.Arc;
import tfm.graphs.Graph;
import tfm.nodes.GraphNode;
import tfm.utils.FileUtil;
import tfm.utils.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public abstract class GraphLog<G extends Graph> {
    public enum Format {
        PNG("png"),
        PDF("pdf");

        private String ext;
        Format(String ext) {
            this.ext = ext;
        }

        public String getExt() {
            return ext;
        }
    }

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
        File tmpDot = File.createTempFile("graph-source-", ".dot");
        tmpDot.deleteOnExit();

        try (Writer w = new FileWriter(tmpDot)) {
//            w.write(graph.toGraphvizRepresentation());

            // JGraphT DOT export
            GraphExporter<GraphNode<?>, Arc> exporter = new DOTExporter<>(
                    component -> String.valueOf(component.getId()),
                    GraphNode::getInstruction,
                    component -> component.getVariable().orElse(""));

            exporter.exportGraph(graph, w);
        } catch (ExportException e) {
            e.printStackTrace();
        }
        ProcessBuilder pb = new ProcessBuilder("dot",
            tmpDot.getAbsolutePath(), "-T" + format.getExt(),
            "-o", getImageFile().getAbsolutePath());
        try {
            int result = pb.start().waitFor();
            if (result != 0) {
                Logger.log("Image generation failed");
            }
        } catch (InterruptedException e) {
            Logger.log("Image generation failed\n" + e.getMessage());
        }
    }

    public void openVisualRepresentation() throws IOException {
        if (!generated) generateImages();
        FileUtil.open(getImageFile());
    }

    protected File getImageFile() {
        return new File("./out/" + imageName + "." + format.getExt());
    }
}
