package es.upv.mist.slicing.graphs;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.utils.ASTUtils;
import es.upv.mist.slicing.utils.Utils;
import org.jgrapht.graph.DirectedPseudograph;
import org.jgrapht.nio.dot.DOTExporter;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClassGraph extends DirectedPseudograph<ClassGraph.Vertex, ClassGraph.ClassArc> implements Buildable<NodeList<CompilationUnit>> {

    /** The key of the vertex map needs to be a String because extendedTypes represent extended classes
     * as ClassOrInterfaceType objects while class declarations define classes as ClassOrInterfaceDeclaration
     * objects and there is no relationship to match them */
    private final Map<String, ClassGraph.Vertex> vertexDeclarationMap = new HashMap<>();

    private boolean built = false;

    public ClassGraph() {
        super(null, null, false);
    }

    /** Locates the vertex that represents a given class or interface declaration.
     *  The vertex must exist, or an exception will be thrown. */
    protected Vertex findVertex(ResolvedClassDeclaration declaration) {
        Optional<Vertex> vertex = vertexSet().stream()
                .filter(v -> v.declaration instanceof ClassOrInterfaceDeclaration)
                .filter(v -> v.declaration.asClassOrInterfaceDeclaration().resolve().asClass().equals(declaration))
                .findFirst();
        return vertex.orElseThrow();
    }

    public Set<ClassOrInterfaceDeclaration> subclassesOf(ResolvedClassDeclaration clazz) {
        return subclassesStreamOf(findVertex(clazz))
                .map(ClassOrInterfaceDeclaration.class::cast)
                .collect(Collectors.toSet());
    }

    protected Stream<Vertex> subclassesStreamOf(Vertex classVertex) {
        return Stream.concat(Stream.of(classVertex), outgoingEdgesOf(classVertex).stream()
                .filter(ClassArc.Extends.class::isInstance)
                .map(this::getEdgeTarget)
                .flatMap(this::subclassesStreamOf));
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

    /** Find the class declarations, the field declaration, and method and constructor declarations (vertices)
     * in the given list of compilation units. */
    protected void buildVertices(NodeList<CompilationUnit> arg) {
        arg.accept(new VoidVisitorAdapter<Void>() {
            private final Deque<ClassOrInterfaceDeclaration> classStack = new LinkedList<>();
//            QUESTIONS & LACKS:
//              1) Is it necessary to include something apart from class vertices?
//              2) Private classes inside other classes?
//              3) Static declaration blocks not considered

            @Override
            public void visit(ClassOrInterfaceDeclaration n, Void arg) {
                classStack.push(n);
                addClassDeclaration(n);
                super.visit(n, arg);
                classStack.pop();
            }

            @Override
            public void visit(FieldDeclaration n, Void arg) {
                addFieldDeclaration(n, classStack.peek());
            }

            @Override
            public void visit(MethodDeclaration n, Void arg) {
                addCallableDeclaration(n, classStack.peek());
            }

            @Override
            public void visit(ConstructorDeclaration n, Void arg) {
                addCallableDeclaration(n, classStack.peek());
            }
        }, null);
    }

    /** Add a class declaration vertex to the class graph */
    protected void addClassDeclaration(ClassOrInterfaceDeclaration n) {
        ClassGraph.Vertex v = new ClassGraph.Vertex(n);
        // Required string to match ClassOrInterfaceType and ClassOrInterfaceDeclaration. QualifiedName Not Valid
        vertexDeclarationMap.put(n.getNameAsString(), v);
        addVertex(v);
    }

    /** Add a field declaration vertex to the class graph */
    protected void addFieldDeclaration(FieldDeclaration n, ClassOrInterfaceDeclaration c){
        ClassGraph.Vertex v = new ClassGraph.Vertex(n);
        // Key value: hashCode + toString() to avoid multiple field declarations with the same syntax in different classes
        vertexDeclarationMap.put(c.getFullyQualifiedName().get()+ "." + n.toString(), v);
        addVertex(v);
    }

    /** Add a method/constructor declaration vertex to the class graph */
    protected void addCallableDeclaration(CallableDeclaration<?> n, ClassOrInterfaceDeclaration c){
        assert n instanceof ConstructorDeclaration || n instanceof MethodDeclaration;
        ClassGraph.Vertex v = new ClassGraph.Vertex(n);
        vertexDeclarationMap.put(c.getFullyQualifiedName().get()+ "." + n.getSignature().toString(), v);
        addVertex(v);
    }

    /** Find the class declarations, field declarations, and method declarations and build the corresponding
     * member/extends/implements relationships in the given list of compilation units. */
    protected void buildEdges(NodeList<CompilationUnit> arg) {
        arg.accept(new VoidVisitorAdapter<Void>() {
            private final Deque<ClassOrInterfaceDeclaration> classStack = new LinkedList<>();

            @Override
            public void visit(ClassOrInterfaceDeclaration n, Void arg) {
                classStack.push(n);
                Vertex v = vertexDeclarationMap.get(n.getNameAsString());
                addClassEdges(v);
                super.visit(n, arg);
                classStack.pop();
            }

            @Override
            public void visit(FieldDeclaration n, Void arg) {
                ClassOrInterfaceDeclaration clazz = classStack.peek();
                Vertex c = vertexDeclarationMap.get(clazz.getNameAsString());
                Vertex v = vertexDeclarationMap.get(clazz.getFullyQualifiedName().get()+ "." + n.toString());
                addEdge(c, v, new ClassArc.Member());
            }

            @Override
            public void visit(MethodDeclaration n, Void arg) {
                ClassOrInterfaceDeclaration clazz = classStack.peek();
                Vertex c = vertexDeclarationMap.get(clazz.getNameAsString());
                Vertex v = vertexDeclarationMap.get(clazz.getFullyQualifiedName().get()+ "." + n.getSignature().toString());
                addEdge(c, v, new ClassArc.Member());
            }

            @Override
            public void visit(ConstructorDeclaration n, Void arg) {
                ClassOrInterfaceDeclaration clazz = classStack.peek();
                Vertex c = vertexDeclarationMap.get(clazz.getNameAsString());
                Vertex v = vertexDeclarationMap.get(clazz.getFullyQualifiedName().get()+ "." + n.getSignature().toString());
                addEdge(c, v, new ClassArc.Member());
            }
        }, null);
    }

    protected void addClassEdges(Vertex v){
        assert v.declaration instanceof ClassOrInterfaceDeclaration;
        ClassOrInterfaceDeclaration dv = (ClassOrInterfaceDeclaration) v.declaration;
        dv.getExtendedTypes().forEach(p -> {
            Vertex source = vertexDeclarationMap.get(p.getNameAsString());
            if (source != null && containsVertex(v))
                addEdge(source, v, new ClassArc.Extends());
        });
        dv.getImplementedTypes().forEach(p -> {
            Vertex source = vertexDeclarationMap.get(p.getNameAsString());
            if (source != null && containsVertex(v))
                addEdge(source, v, new ClassArc.Implements());
        });
    }

    /** Creates a graph-appropriate DOT exporter. */
    public DOTExporter<CallableDeclaration<?>, CallGraph.Edge<?>> getDOTExporter() {
        DOTExporter<CallableDeclaration<?>, CallGraph.Edge<?>> dot = new DOTExporter<>();
        dot.setVertexAttributeProvider(decl -> Utils.dotLabel(decl.getDeclarationAsString(false, false, false)));
        dot.setEdgeAttributeProvider(edge -> Utils.dotLabel(edge.getCall().toString()));
        return dot;
    }

    /** A vertex containing the declaration it represents. It only exists because
     *  JGraphT relies heavily on equals comparison, which may not be correct in declarations. */
    protected static class Vertex {
        // First ancestor common class in the JavaParser hierarchy for
        // ClassOrInterfaceDeclaration, FieldDeclaration and CallableDeclaration
        protected final BodyDeclaration<?> declaration;

        public Vertex(BodyDeclaration<?> declaration) {
            this.declaration = declaration;
        }

        /** The declaration represented by this node. */
        public BodyDeclaration<?> getDeclaration() {
            return declaration;
        }

        @Override
        public int hashCode() {
            return Objects.hash(declaration, declaration.getRange());
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof CallGraph.Vertex && ASTUtils.equalsWithRangeInCU(((CallGraph.Vertex) obj).declaration, declaration);
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }

    protected static class ClassArc extends Arc {
        /** An arc that connects a class with another one that inherits from it. */
        protected static class Extends extends ClassArc {}
        /** An arc that connects an interface to a class that implements it. */
        protected static class Implements extends ClassArc {}
        /** An arc that connects a class with a field or method contained in it. */
        protected static class Member extends ClassArc {}
    }
}


