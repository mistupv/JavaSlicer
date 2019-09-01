package tfm.nodes;

import com.github.javaparser.ast.Node;
import edg.graphlib.Arrow;
import org.checkerframework.checker.nullness.qual.NonNull;
import tfm.arcs.data.ArcData;

import java.util.Collection;
import java.util.Set;

public class SDGNode<N extends Node> extends GraphNode<N> {

    public <N1 extends GraphNode<N>> SDGNode(N1 node) {
        super(node);
    }

    public SDGNode(int id, String representation, N node) {
        super(id, representation, node);
    }
    public SDGNode(int id, String representation, @NonNull N node, Collection<? extends Arrow<String, ArcData>> incomingArcs, Collection<? extends Arrow<String, ArcData>> outgoingArcs, Set<String> declaredVariables, Set<String> definedVariables, Set<String> usedVariables) {
        super(id, representation, node, incomingArcs, outgoingArcs, declaredVariables, definedVariables, usedVariables);
    }

    public String toString() {
        return String.format("SDGNode{id: %s, data: %s, ");
    }
}
