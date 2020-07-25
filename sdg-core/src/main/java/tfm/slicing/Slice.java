package tfm.slicing;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
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

    public Set<GraphNode<?>> getGraphNodes() {
        return Set.copyOf(map.values());
    }

    /**
     * Organize all nodes pertaining to this slice in one or more CompilationUnits.
     * CompilationUnits themselves need not be part of the slice to be included if any of their
     * components are present.
     */
    public NodeList<CompilationUnit> toAst() {
        Map<CompilationUnit, Set<Node>> cuMap = new HashMap<>();
        // Add each node to the corresponding bucket of the map
        // Nodes may not belong to a compilation unit (fictional nodes), and they are skipped for the slice.
        for (Node node : nodes) {
            Optional<CompilationUnit> cu = node.findCompilationUnit();
            if (cu.isEmpty()) continue;
            cuMap.putIfAbsent(cu.get(), new HashSet<>());
            cuMap.get(cu.get()).add(node);
        }
        // Traverse the AST of each compilation unit, creating a copy and
        // removing any element not present in the slice.
        NodeList<CompilationUnit> cus = new NodeList<>();
        SlicePruneVisitor sliceVisitor = new SlicePruneVisitor();
        CloneVisitor cloneVisitor = new CloneVisitor();
        for (Map.Entry<CompilationUnit, Set<Node>> entry : cuMap.entrySet()) {
            CompilationUnit clone = (CompilationUnit) entry.getKey().accept(cloneVisitor, null);
            assert entry.getKey().getStorage().isPresent();
            clone.setStorage(entry.getKey().getStorage().get().getPath());
            clone.accept(sliceVisitor, entry.getValue());
            cus.add(clone);
        }
        return cus;
    }

    @Deprecated
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
        clone.accept(new SlicePruneVisitor(), nodes);
        return ((MethodDeclaration) clone);
    }
}
