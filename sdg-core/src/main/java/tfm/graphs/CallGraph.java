package tfm.graphs;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.Resolvable;
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedPseudograph;
import org.jgrapht.io.DOTExporter;
import tfm.graphs.cfg.CFG;
import tfm.nodes.GraphNode;
import tfm.utils.ASTUtils;
import tfm.utils.NodeNotFoundException;

import java.util.*;

/**
 * A directed graph which displays the available method declarations as nodes and their
 * invocations as edges (caller to callee).
 * <br/>
 * Method declarations include both {@link ConstructorDeclaration constructors}
 * and {@link MethodDeclaration method declarations}.
 * In the future, {@link InitializerDeclaration static initializer blocks} and field initializers will be included.
 * <br/>
 * Method calls include only direct method calls, from {@link MethodCallExpr normal call},
 * to {@link ObjectCreationExpr object creation} and {@link ExplicitConstructorInvocationStmt
 * explicit constructor invokation} ({@code this()}, {@code super()}).
 */
public class CallGraph extends DirectedPseudograph<CallGraph.Vertex, CallGraph.Edge<?>> implements Buildable<NodeList<CompilationUnit>> {
    private final Map<CallableDeclaration<?>, CFG> cfgMap;
    private final Map<CallableDeclaration<?>, Vertex> vertexDeclarationMap = new IdentityHashMap<>();

    private boolean built = false;

    public CallGraph(Map<CallableDeclaration<?>, CFG> cfgMap) {
        super(null, null, false);
        this.cfgMap = cfgMap;
    }

    /** Resolve a call to its declaration, by using the call AST nodes stored on the edges. */
    public CallableDeclaration<?> getCallTarget(Resolvable<? extends ResolvedMethodLikeDeclaration> call) {
        return edgeSet().stream()
                .filter(e -> e.getCall() == call)
                .map(this::getEdgeTarget)
                .map(Vertex::getDeclaration)
                .map(CallableDeclaration.class::cast)
                .findFirst()
                .orElse(null);
    }

    @Override
    public void build(NodeList<CompilationUnit> arg) {
        if (isBuilt())
            return;
        buildVertices(arg);
        buildEdges(arg);
        built = true;
    }

    @Override
    public boolean isBuilt() {
        return built;
    }

    /** Find the method and constructor declarations (vertices) in the given list of compilation units. */
    protected void buildVertices(NodeList<CompilationUnit> arg) {
        arg.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodDeclaration n, Void arg) {
                addDeclaration(n);
                super.visit(n, arg);
            }

            @Override
            public void visit(ConstructorDeclaration n, Void arg) {
                addDeclaration(n);
                super.visit(n, arg);
            }
        }, null);
    }

    protected void addDeclaration(CallableDeclaration<?> n) {
        Vertex v = new Vertex(n);
        vertexDeclarationMap.put(n, v);
        addVertex(v);
    }

    protected boolean addEdge(CallableDeclaration<?> source, CallableDeclaration<?> target, Resolvable<? extends ResolvedMethodLikeDeclaration> call) {
        Edge<?> edge = new Edge<>(call, findGraphNode(call, source));
        return addEdge(vertexDeclarationMap.get(source), vertexDeclarationMap.get(target), edge);
    }

    /** Find the calls to methods and constructors (edges) in the given list of compilation units. */
    protected void buildEdges(NodeList<CompilationUnit> arg) {
        arg.accept(new VoidVisitorAdapter<Void>() {
            private final Deque<CallableDeclaration<?>> declStack = new LinkedList<>();

            // ============ Method declarations ===========
            // There are some locations not considered, which may lead to an error in the stack.
            // 1. Method calls in non-static field initializations are assigned to all constructors of that class
            // 2. Method calls in static field initializations are assigned to the static block of that class

            @Override
            public void visit(MethodDeclaration n, Void arg) {
                declStack.push(n);
                super.visit(n, arg);
                declStack.pop();
            }

            @Override
            public void visit(ConstructorDeclaration n, Void arg) {
                declStack.push(n);
                super.visit(n, arg);
                declStack.pop();
            }

            // =============== Method calls ===============
            @Override
            public void visit(MethodCallExpr n, Void arg) {
                n.resolve().toAst().ifPresent(decl -> addEdge(declStack.peek(), decl, n));
                super.visit(n, arg);
            }

            @Override
            public void visit(ObjectCreationExpr n, Void arg) {
                n.resolve().toAst().ifPresent(decl -> addEdge(declStack.peek(), decl, n));
                super.visit(n, arg);
            }

            @Override
            public void visit(ExplicitConstructorInvocationStmt n, Void arg) {
                n.resolve().toAst().ifPresent(decl -> addEdge(declStack.peek(), decl, n));
                super.visit(n, arg);
            }
        }, null);
    }

    /** Locates the node in the collection of CFGs that contains the given call. */
    protected GraphNode<?> findGraphNode(Resolvable<? extends ResolvedMethodLikeDeclaration> n, CallableDeclaration<?> declaration) {
        for (GraphNode<?> node : cfgMap.get(declaration).vertexSet())
            if (node.containsCall(n))
                return node;
        throw new NodeNotFoundException("call " + n + " could not be located!");
    }

    /** Creates a graph-appropriate DOT exporter. */
    public DOTExporter<CallableDeclaration<?>, Edge<?>> getDOTExporter() {
        int[] id = new int[]{0};
        return new DOTExporter<>(
                decl -> id[0]++ + "",
                decl -> decl.getDeclarationAsString(false, false, false),
                e -> e.getCall().toString()
        );
    }

    /** A vertex containing the declaration it represents. It only exists because
     *  JGraphT relies heavily on equals comparison, which may not be correct in declarations. */
    public static class Vertex {
        protected final CallableDeclaration<?> declaration;

        public Vertex(CallableDeclaration<?> declaration) {
            assert declaration instanceof ConstructorDeclaration || declaration instanceof MethodDeclaration;
            this.declaration = declaration;
        }

        /** The declaration represented by this node. */
        public CallableDeclaration<?> getDeclaration() {
            return declaration;
        }

        @Override
        public int hashCode() {
            return Objects.hash(declaration, declaration.getRange());
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Vertex && ASTUtils.equalsWithRangeInCU(((Vertex) obj).declaration, declaration);
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }

    /** An edge containing the call it represents, and the graph node that contains it. */
    public static class Edge<T extends Resolvable<? extends ResolvedMethodLikeDeclaration>> extends DefaultEdge {
        protected final T call;
        protected final GraphNode<?> graphNode;

        public Edge(T call, GraphNode<?> graphNode) {
            assert call instanceof MethodCallExpr || call instanceof ObjectCreationExpr || call instanceof ExplicitConstructorInvocationStmt;
            this.call = call;
            this.graphNode = graphNode;
        }

        /** The call represented by this edge. Using the {@link CallGraph} it can be effortlessly resolved. */
        public T getCall() {
            return call;
        }

        /** The graph node that contains the call represented by this edge. */
        public GraphNode<?> getGraphNode() {
            return graphNode;
        }

        @Override
        public String toString() {
            return String.format("%s -%d-> %s",
                    ((CallableDeclaration<?>) getSource()).getDeclarationAsString(false, false, false),
                    graphNode.getId(),
                    ((CallableDeclaration<?>) getTarget()).getDeclarationAsString(false, false, false));
        }
    }
}
