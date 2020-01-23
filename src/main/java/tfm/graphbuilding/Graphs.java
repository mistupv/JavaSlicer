package tfm.graphbuilding;

import tfm.graphs.CFG;
import tfm.graphs.PDG;
import tfm.graphs.SDG;

public class Graphs {

    public static final GraphOptions<CFG> CFG = new CFGOptions();
    public static final GraphOptions<PDG> PDG = new PDGOptions();
    public static final GraphOptions<SDG> SDG = new SDGOptions();

}