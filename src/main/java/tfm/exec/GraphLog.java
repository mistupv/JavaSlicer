package tfm.exec;

import tfm.graphs.Graph;
import tfm.utils.FileUtil;
import tfm.utils.Logger;

import java.io.*;

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
            graph.getDOTExporter().exportGraph(graph, stringWriter);
            stringWriter.append('\n');
            Logger.log(stringWriter.toString());
        }
    }

    public void generateImages() throws IOException {
        generateImages("graph");
    }

    public void generateImages(String imageName) throws IOException {
        generateImages(imageName, Format.PNG);
    }

    public void generateImages(String imageName, Format format) throws IOException {
        this.imageName = imageName + "-" + graph.getClass().getName();
        this.format = format;
        generated = true;
        File tmpDot = File.createTempFile("graph-source-", ".dot");

        // Graph -> DOT -> file
        try (Writer w = new FileWriter(tmpDot)) {
            graph.getDOTExporter().exportGraph(graph, w);
        }
        // Execute dot
        ProcessBuilder pb = new ProcessBuilder("dot",
            tmpDot.getAbsolutePath(), "-T" + format.getExt(),
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

    public void openVisualRepresentation() throws IOException {
        if (!generated) generateImages();
        FileUtil.open(getImageFile());
    }

    protected File getImageFile() {
        return new File("./out/" + imageName + "." + format.getExt());
    }
}
