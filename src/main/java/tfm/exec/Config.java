package tfm.exec;

/**
 * Configuration for the different graphs created in the process of program slicing.
 * <br/>
 * The <b>Control Flow Graph</b> has different variations, each with its own name or code.
 * <ul>
 *     <li>CFG</li>: the original proposal. It works well with statements, loops and branch instructions.
 *     <li>ACFG</li>: the <it>augmented</it> CFG; adds non-executable edges to represent unconditional
 *     jumps such as {@code break}, {@code return}, {@code switch} and much more.
 * </ul>
 * The <b>Program Dependence Graph</b> has variations in a similar fashion.
 * <ul>
 *     <li>PDG</li>: based on the CFG, it computes control and data dependence to connect the nodes.
 *     <li>APDG</li>: similar to the PDG, but based on the ACFG. The non-executable edges are ignored
 *     when computing data dependencies.
 * </ul>
 */
public class Config {
    public static final int CFG = 0, ACFG = 1;
    public static final int PDG = 0, APDG = 1;

    public static int CFG_TYPE = CFG;
    public static int PDG_TYPE = PDG;
}
