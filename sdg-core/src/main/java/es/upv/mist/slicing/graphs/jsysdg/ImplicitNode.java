package es.upv.mist.slicing.graphs.jsysdg;

import com.github.javaparser.ast.Node;
import es.upv.mist.slicing.nodes.SyntheticNode;

// TODO: Concretar más el tipo T del nodo (Node es muy general). Por ahora seria solo ExplicitConstructorInvocationStmt
public class ImplicitNode extends SyntheticNode<Node> {
    protected ImplicitNode(String instruction, Node astNode) {
        super(instruction, astNode);
    }
}
