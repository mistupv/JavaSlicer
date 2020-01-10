package tfm.exec;

/**
 * Configuration for the different graphs created in the process of program slicing.
 * <br/>
 * The <b>Control Flow Graph</b> has different variations, each with its own name or code.
 * <ul>
 *     <li><b>CFG</b>: the original proposal. It works well with statements, loops and branch instructions.</li>
 *     <li><b>ACFG</b>: the <it>augmented</it> CFG; adds non-executable edges to represent unconditional
 *     jumps such as {@code break}, {@code return}, {@code switch} and much more.</li>
 * </ul>
 * The <b>Program Dependence Graph</b> has variations in a similar fashion.
 * <ul>
 *     <li><b>PDG</b>: based on the CFG, it computes control and data dependence to connect the nodes.</li>
 *     <li><b>APDG</b>: similar to the PDG, but based on the ACFG. The non-executable edges are ignored
 *     when computing data dependencies.</li>
 *     <li><b>PPDG</b>: combines the PDG and the APDG; (1) when computing control dependencies it uses the ACFG
 *     to find a node's successors, but the CFG for computing the postdominance, and (2) modifies the traversal
 *     of the graph, disallowing the traversal of edges that reach a pseudo-predicate (those that are the source
 *     of non-executable edges) if the pseudo-predicate is not the slicing criterion.</li>
 * </ul>
 */
public class Config {
    public static final int CFG = 0, ACFG = 1;
    public static final int PDG = 0, APDG = 1, PPDG = 2;

    public static int CFG_TYPE = CFG;
    public static int PDG_TYPE = PDG;
}
