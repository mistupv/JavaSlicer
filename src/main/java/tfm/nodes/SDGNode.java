package tfm.nodes;

import com.github.javaparser.ast.stmt.Statement;
import org.checkerframework.checker.nullness.qual.NonNull;

public class SDGNode extends Node {

    public <N extends Node> SDGNode(N node) {
        super(node);
    }

    public SDGNode(int id, String representation, @NonNull Statement statement) {
        super(id, representation, statement);
    }

    public String toString() {
        return String.format("SDGNode{id: %s, data: %s, ");
    }
}
