package tfm.graphbuilding;

import tfm.graphs.CFGGraph;
import tfm.graphs.PDGGraph;
import tfm.graphs.SDGGraph;

public class Graphs {

    public static final GraphOptions<CFGGraph> CFG = new CFGOptions();
    public static final GraphOptions<PDGGraph> PDG = new PDGOptions();
    public static final GraphOptions<SDGGraph> SDG = new SDGOptions();

}