package es.upv.mist.slicing.graphs.icfg;

import es.upv.mist.slicing.cli.DOTAttributes;
import es.upv.mist.slicing.graphs.scrs.IntraSCR;
import es.upv.mist.slicing.graphs.scrs.IntraSCRGraph;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.utils.Logger;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.nio.dot.DOTExporter;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class GraphLogIntraSCR<G extends IntraSCRGraph> {

    protected G graph;

    protected String imageName;
    protected String format;
    protected boolean generated = false;
    protected List<IntraSCR> marked = new ArrayList<>();
    protected File outputDir = new File("./out/");

    public GraphLogIntraSCR() {
        this(null);
    }

    public GraphLogIntraSCR(G graph) {
        this.graph = graph;
    }

    public GraphLogIntraSCR(G graph, List<IntraSCR> processedIntraSCRs) {
        this.marked = processedIntraSCRs;
        this.graph = graph;
    }

    public void setDirectory(File outputDir) {
        this.outputDir = outputDir;
    }

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
        try (StringWriter stringWriter = new StringWriter()) {
            getDOTExporter().exportGraph(graph, stringWriter);
            stringWriter.append('\n');
            Logger.log(stringWriter.toString());
        }
    }

    public void generateImages() throws IOException {
        generateImages("graph");
    }

    public void generateImages(String imageName) throws IOException {
        generateImages(imageName, "pdf");
    }

    public void generateImages(String imageName, String format) throws IOException {
        this.format = format;
        generated = true;
        File tmpDot = getDotFile(imageName);
        // Graph -> DOT -> file
        try (Writer w = new FileWriter(tmpDot)) {
            getDOTExporter().exportGraph(graph, w);
        }
        // Execute dot
        ProcessBuilder pb = new ProcessBuilder("dot",
                tmpDot.getAbsolutePath(), "-T" + format,
                "-o", getImageFile().getAbsolutePath());
        try {
            int result = pb.start().waitFor();
            if (result == 0)
                tmpDot.deleteOnExit();
            else
                Logger.log("Image generation failed, try running \"" + pb.toString() + "\" on your terminal.");
        } catch (InterruptedException e) {
            Logger.log("Image generation failed\n" + e.getMessage());
        }
    }

    public File getDotFile(String imageName) throws IOException {
        this.imageName = imageName + "-" + graph.getClass().getSimpleName();
        File tmpDot = File.createTempFile("graph-source-", ".dot");
        tmpDot.getParentFile().mkdirs();
        getImageFile().getParentFile().mkdirs();

        // Graph -> DOT -> file
        try (Writer w = new FileWriter(tmpDot)) {
            getDOTExporter().exportGraph(graph, w);
        }

        return tmpDot;
    }

    public void openVisualRepresentation() throws IOException {
        if (!generated) generateImages();
        openFileForUser(getImageFile());
    }

    protected static void openFileForUser(File file) throws IOException {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(file);
            return;
        }
        // Alternative manual opening of the file
        String os = System.getProperty("os.name").toLowerCase();
        String cmd = null;
        if (os.contains("win")) {
            cmd = "";
        } else if (os.contains("mac")) {
            cmd = "open";
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            cmd = "xdg-open";
        }

        if (cmd != null) {
            new ProcessBuilder(cmd, file.getAbsolutePath()).start();
        } else {
            Logger.format("Warning: cannot open file %s in your system (%s)",
                    file.getName(), os);
        }
    }

    public File getImageFile() {
        return new File(outputDir, imageName + "." + format);
    }

    protected DOTExporter<IntraSCR, DefaultEdge> getDOTExporter() {
        DOTExporter<IntraSCR, DefaultEdge> exporter = new DOTExporter<>();
        exporter.setVertexIdProvider(node -> String.valueOf(node.vertexSet().stream().map(GraphNode::getId).findFirst().orElse(-1L)));
        exporter.setVertexAttributeProvider(v -> vertexAttributes(v).build());
        exporter.setEdgeAttributeProvider(v -> edgeAttributes(v).build());
        return exporter;
    }

    protected DOTAttributes vertexAttributes(IntraSCR vertex) {
        DOTAttributes res = new DOTAttributes();
        if(!marked.isEmpty()){
            res.set("style", marked.contains(vertex) ? "normal" : "dashed");
        }
        res.set("label", "X" + vertex.getId() + "\n" + "TOP-" + vertex.getTopologicalNumber() + "\n" + vertex.vertexSet().stream()
                .map(graphNode -> "%04d: %s".formatted(graphNode.getId(), graphNode.getLabel()))
                .sorted()
                .collect(Collectors.joining("\n")));
        return res;
    }

    protected DOTAttributes edgeAttributes(DefaultEdge arc) {
        return new DOTAttributes();
    }
}
