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

import java.util.Deque;
import java.util.LinkedList;

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
public class CallGraph extends DirectedPseudograph<CallableDeclaration<?>, CallGraph.Edge<?>> implements Buildable<NodeList<CompilationUnit>> {
    private boolean built = false;

    public CallGraph() {
        super(null, null, false);
    }

    @Override
    public void build(NodeList<CompilationUnit> arg) {
        buildVertices(arg);
        buildEdges(arg);
        built = true;
    }

    @Override
    public boolean isBuilt() {
        return built;
    }

    protected void buildVertices(NodeList<CompilationUnit> arg) {
        arg.accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(MethodDeclaration n, Void arg) {
                addVertex(n);
                super.visit(n, arg);
            }

            @Override
            public void visit(ConstructorDeclaration n, Void arg) {
                addVertex(n);
                super.visit(n, arg);
            }
        }, null);
    }

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
                n.resolve().toAst().ifPresent(decl -> addEdge(declStack.peek(), decl, new Edge<>(n)));
                super.visit(n, arg);
            }

            @Override
            public void visit(ObjectCreationExpr n, Void arg) {
                n.resolve().toAst().ifPresent(decl -> addEdge(declStack.peek(), decl, new Edge<>(n)));
                super.visit(n, arg);
            }

            @Override
            public void visit(ExplicitConstructorInvocationStmt n, Void arg) {
                n.resolve().toAst().ifPresent(decl -> addEdge(declStack.peek(), decl, new Edge<>(n)));
                super.visit(n, arg);
            }
        }, null);
    }

    public DOTExporter<CallableDeclaration<?>, Edge<?>> getDOTExporter() {
        int[] id = new int[]{0};
        return new DOTExporter<>(
                decl -> id[0]++ + "",
                decl -> decl.getDeclarationAsString(false, false, false),
                e -> e.getCallExpr().toString()
        );
    }

    public static class Edge<T extends Resolvable<? extends ResolvedMethodLikeDeclaration>> extends DefaultEdge {
        protected T callExpr;

        public Edge(T callExpr) {
            this.callExpr = callExpr;
        }

        public T getCallExpr() {
            return callExpr;
        }
    }
}
