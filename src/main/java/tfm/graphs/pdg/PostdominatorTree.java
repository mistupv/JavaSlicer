package tfm.graphs.pdg;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.traverse.DepthFirstIterator;
import tfm.arcs.Arc;
import tfm.graphs.Graph;
import tfm.graphs.GraphWithRootNode;
import tfm.graphs.cfg.CFG;
import tfm.nodes.GraphNode;

import java.util.*;
import java.util.stream.Collectors;

public class PostdominatorTree extends DirectedTree<GraphNode<?>, DefaultEdge> {
    private Map<Arc, Route> routeMap;
//    private Map<GraphNode<?>, Set<Route>> servedByMap;

    public PostdominatorTree(CFG cfg) {
        super(null, DefaultEdge::new, false);
        // Vertices
        cfg.vertexSet().forEach(this::addVertex);
        // Edges
        Map<GraphNode<?>, GraphNode<?>> map = immediatePostdominatorTree(cfg);
        for (Map.Entry<GraphNode<?>, GraphNode<?>> entry : map.entrySet())
            addEdge(entry.getValue(), entry.getKey());
        // Set root
        for (GraphNode<?> node : vertexSet()) {
            if (inDegreeOf(node) == 0) {
                if (root == null)
                    root = node;
                else throw new IllegalStateException("Multiple roots found!");
            }
        }
        if (root == null)
            throw new IllegalStateException("No root found!");
        // Build route map and cache
        routeMap = constructRomanChariots(cfg);
//        servedByMap = constructCache();
    }

    private GraphNode<?> parentOf(GraphNode<?> node) {
        Set<DefaultEdge> edges = incomingEdgesOf(node);
        if (edges.size() > 1)
            throw new IllegalStateException("Node has multiple parents!");
        if (edges.isEmpty())
            throw new IllegalStateException("Node has no parents! Don't call this method on the root node!");
        return getEdgeSource(edges.iterator().next());
    }

    private Set<GraphNode<?>> childrenOf(GraphNode<?> node) {
        return outgoingEdgesOf(node).stream().map(this::getEdgeTarget).collect(Collectors.toSet());
    }

    /** The set of nodes controlled by the arc */
    protected Set<GraphNode<?>> cd(Graph graph, Arc arc) {
        GraphNode<?> u = graph.getEdgeSource(arc);
        GraphNode<?> v = graph.getEdgeTarget(arc);
        if (v.equals(parentOf(u)))
            return Collections.emptySet();
        if (!routeMap.containsKey(arc))
            throw new IllegalArgumentException("Arc is not valid!");
        return cd(routeMap.get(arc));
    }

    /** The nodes traversed by the route */
    private Set<GraphNode<?>> cd(Route r) {
        GraphNode<?> w = r.dest;
        Set<GraphNode<?>> cdSet = new HashSet<>();
        for (GraphNode<?> v = r.src; !v.equals(w) && !v.equals(root); v = parentOf(v))
            cdSet.add(v);
        return cdSet;
    }

    /** The set of nodes that is controlled by the node.
     * Equivalent to the routes that serve the node. */
    protected Set<GraphNode<?>> conds(CFG cfg, GraphNode<?> w) {
        Set<GraphNode<?>> res = new HashSet<>();
        for (Arc arc : cfg.outgoingEdgesOf(w))
            if (routeMap.containsKey(arc))
                res.addAll(cd(cfg, arc));
        return res;
    }

    private List<GraphNode<?>> inTopDownOrder() {
        Iterator<GraphNode<?>> it = new DepthFirstIterator<>(this, root);
        List<GraphNode<?>> list = new LinkedList<>();
        it.forEachRemaining(node -> {
            if (!list.contains(node))
                list.add(node);
        });
        return list;
    }

    public Set<GraphNode<?>> controlDependenciesOf(CFG cfg, GraphNode<?> node) {
        return conds(cfg, node);
    }

    private Map<Arc, Route> constructRomanChariots(CFG cfg) {
        Map<Arc, Route> routes = new HashMap<>();
        for (GraphNode<?> p : inTopDownOrder()) {
            for (GraphNode<?> u : childrenOf(p)) {
                for (Arc arc : cfg.outgoingEdgesOf(u)) {
                    GraphNode<?> v = cfg.getEdgeTarget(arc);
                    if (!v.equals(p)) {
                        // Append a `cd` set to end of routes
                        routes.put(arc, new Route(v, p));
                    }
                }
            }
        }
        return routes;
    }

    private static class Route {
        GraphNode<?> src;
        GraphNode<?> dest;

        public Route(GraphNode<?> src, GraphNode<?> dest) {
            this.src = src;
            this.dest = dest;
        }

        @Override
        public String toString() {
            return String.format("[%d, %d)", src.getId(), dest.getId());
        }
    }

    private Map<GraphNode<?>, Set<Route>> constructCache() {
        Map<GraphNode<?>, Set<Route>> map = new HashMap<>();
        for (Route r : routeMap.values()) {
            for (GraphNode<?> n : cd(r)) {
                if (!map.containsKey(n))
                    map.put(n, new HashSet<>());
                map.get(n).add(r);
            }
        }
        return map;
    }

    private static Map<GraphNode<?>, GraphNode<?>> immediatePostdominatorTree(CFG cfg) {
        Optional<GraphNode<?>> optExitNode = cfg.vertexSet().stream().filter(gn -> gn.getInstruction().equals("Exit")).findFirst();
        if (!optExitNode.isPresent())
            throw new IllegalStateException("CFG lacks exit node");
        GraphNode<?> exitNode = optExitNode.get();
        Map<GraphNode<?>, GraphNode<?>> postdoms = new HashMap<>();
        List<GraphNode<?>> postorderList = postorder(cfg);
        Map<GraphNode<?>, Integer> sortingMap = new HashMap<>();
        for (int i = 0; i < postorderList.size(); i++)
            sortingMap.put(postorderList.get(i), i);
        postorderList.remove(exitNode);
        postdoms.put(exitNode, exitNode);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (GraphNode<?> node : postorderList) {
                GraphNode<?> newIpostdom = null;
                for (Arc arc : cfg.outgoingEdgesOf(node)) {
                    GraphNode<?> successor = cfg.getEdgeTarget(arc);
                    if (newIpostdom == null && postdoms.containsKey(successor))
                        newIpostdom = successor;
                    if (newIpostdom != null && postdoms.containsKey(successor))
                        newIpostdom = reverseIntersect(successor, newIpostdom, postdoms, sortingMap);
                }
                if (postdoms.get(node) != newIpostdom) {
                    postdoms.put(node, newIpostdom);
                    changed = true;
                }
            }
        }
        postdoms.remove(exitNode);
        return postdoms;
    }

    private static GraphNode<?> reverseIntersect(final GraphNode<?> b1, final GraphNode<?> b2,
                                                 final Map<GraphNode<?>, GraphNode<?>> postdoms,
                                                 final Map<GraphNode<?>, Integer> sortingMap) {
        GraphNode<?> finger1 = b1;
        GraphNode<?> finger2 = b2;
        while (finger1 != finger2) {
            while (compare(finger1, finger2, sortingMap) > 0)
                finger1 = postdoms.get(finger1);
            while (compare(finger2, finger1, sortingMap) > 0)
                finger2 = postdoms.get(finger2);
        }
        return finger1;
    }

    private static int compare(GraphNode<?> b1, GraphNode<?> b2, Map<GraphNode<?>, Integer> sortingMap) {
        return Integer.compare(sortingMap.get(b1), sortingMap.get(b2));
    }

    private static List<GraphNode<?>> postorder(GraphWithRootNode<?> graph) {
        Optional<GraphNode<?>> rootNode = graph.getRootNode();
        if (!rootNode.isPresent())
            throw new IllegalStateException("CFG lacks root node");
        return postorder(graph, rootNode.get());
    }

    private static <V> List<V> postorder(org.jgrapht.Graph<V, ?> graph, V startVertex) {
        List<V> dfsOrder = new LinkedList<>();
        Iterator<V> it = new DepthFirstIterator<>(graph, startVertex);
        it.forEachRemaining(dfsOrder::add);

        List<V> postorder = new LinkedList<>();
        for (V v : dfsOrder)
            if (!postorder.contains(v))
                postorder.add(v);
        return postorder;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("PostdominatorTree");
        for (DefaultEdge edge : edgeSet())
            builder.append(getEdgeSource(edge).getId())
                    .append(" -> ")
                    .append(getEdgeTarget(edge).getId())
                    .append('\n');
        return builder.toString();
    }
}
