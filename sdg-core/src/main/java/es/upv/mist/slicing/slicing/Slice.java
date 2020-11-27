package es.upv.mist.slicing.slicing;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.visitor.CloneVisitor;
import es.upv.mist.slicing.nodes.GraphNode;

import java.util.*;

/** The representation of a slice, or a subset of a graph's nodes.
 *  A slice can be obtained from any {@link Sliceable} object, and converted
 *  to code with {@link #toAst()}. */
public class Slice {
    /** Nodes contained in this slice, mapped by id. */
    private final Map<Long, GraphNode<?>> map = new HashMap<>();
    /** The AST nodes contained in this slice. */
    private final List<Node> nodes = new LinkedList<>();

    /** Add a node to this slice. */
    public void add(GraphNode<?> node) {
        assert !map.containsKey(node.getId());
        map.put(node.getId(), node);
        nodes.add(node.getAstNode());
    }

    /** Add multiple nodes to this slice. */
    public void addAll(Collection<GraphNode<?>> nodes) {
        nodes.forEach(this::add);
    }

    /** Whether the slice contains the given node. */
    public boolean contains(GraphNode<?> node) {
        return map.containsKey(node.getId());
    }

    /** Whether the slice contains the given AST node. */
    public boolean contains(Node node) {
        return nodes.stream().anyMatch(n -> n == node);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Slice && map.equals(((Slice) obj).map);
    }

    /** Obtain the nodes from this slice. */
    public Set<GraphNode<?>> getGraphNodes() {
        return Set.copyOf(map.values());
    }

    /** Organize all nodes pertaining to this slice in one or more CompilationUnits. CompilationUnits
     *  themselves need not be part of the slice to be included if any of their components are present. */
    public NodeList<CompilationUnit> toAst() {
        Map<CompilationUnit, Set<Node>> cuMap = new IdentityHashMap<>();
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
            if (entry.getKey().getStorage().isPresent())
                clone.setStorage(entry.getKey().getStorage().get().getPath(),
                        entry.getKey().getStorage().get().getEncoding());
            clone.accept(sliceVisitor, entry.getValue());
            cus.add(clone);
        }
        return cus;
    }
}
