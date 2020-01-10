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
 */
public class Config {
    public static final int CFG = 0, ACFG = 1;
    public static int CFG_TYPE = CFG;
}
