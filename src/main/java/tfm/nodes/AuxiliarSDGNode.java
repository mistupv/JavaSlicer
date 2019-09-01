package tfm.nodes;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.EmptyStmt;

public class AuxiliarSDGNode extends SDGNode<EmptyStmt> {

    public <N1 extends GraphNode<EmptyStmt>> AuxiliarSDGNode(N1 node) {
        super(node);
    }

    public AuxiliarSDGNode(int id, String representation) {
        super(id, representation, new EmptyStmt());
    }

}
