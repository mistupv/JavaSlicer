package tfm.slicing;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.CloneVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import tfm.nodes.GraphNode;

import java.util.*;
import java.util.stream.Collectors;

public class Slice {
    private final Map<Long, GraphNode<?>> map = new HashMap<>();
    private final Set<Node> nodes = new HashSet<>();

    public Slice() {}

    public void add(GraphNode<?> node) {
        assert !map.containsKey(node.getId());
        map.put(node.getId(), node);
        nodes.add(node.getAstNode());
    }

    public boolean contains(GraphNode<?> node) {
        return map.containsKey(node.getId());
    }

    public boolean contains(Node node) {
        return nodes.contains(node);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Slice && map.equals(((Slice) obj).map);
    }

    public Node getAst() {
        List<GraphNode<?>> methods = map.values().stream().filter(e -> e.getAstNode() instanceof MethodDeclaration).collect(Collectors.toList());
        if (methods.size() == 1) {
            Optional<Long> secondNode = map.keySet().stream()
                    .sorted(Long::compareTo).skip(1).findFirst();
            assert secondNode.isPresent();
            Node n = map.get(secondNode.get()).getAstNode();
            assert !(n instanceof MethodDeclaration);
            while (!(n instanceof MethodDeclaration) && n.getParentNode().isPresent())
                n = n.getParentNode().get();
            assert n instanceof MethodDeclaration;
            return getMethodAst(n);
        } else if (methods.size() > 1)
            throw new RuntimeException("Not implemented");
        throw new RuntimeException("No method found in the slice");
    }

    private MethodDeclaration getMethodAst(Node node) {
        Visitable clone = node.accept(new CloneVisitor(), null);
        assert clone instanceof MethodDeclaration;
        clone.accept(new SliceAstVisitor(), this);
        return ((MethodDeclaration) clone);
    }
}
