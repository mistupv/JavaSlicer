package es.upv.mist.slicing.graphs.jsysdg;

import com.github.javaparser.ast.Node;
import es.upv.mist.slicing.nodes.SyntheticNode;

/**
 * A graph node that does not exist in the original program, but represents
 * implicit code constructs such as 'super()' at the start of a constructor.
 * @param <T> The type of the AST node contained in this graph node.
 */
public class ImplicitNode<T extends Node> extends SyntheticNode<T> {
    protected ImplicitNode(String instruction, T astNode) {
        super(instruction, astNode);
    }
}
