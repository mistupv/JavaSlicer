package es.upv.mist.slicing.graphs;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.nodes.VariableAction.ObjectTree;
import es.upv.mist.slicing.utils.ASTUtils;
import es.upv.mist.slicing.utils.Utils;
import org.jgrapht.graph.DirectedPseudograph;
import org.jgrapht.nio.dot.DOTExporter;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClassGraph extends DirectedPseudograph<ClassGraph.Vertex, ClassGraph.ClassArc> implements Buildable<NodeList<CompilationUnit>> {
    private static ClassGraph instance = null;

    public static ClassGraph getNewInstance() {
        instance = null;
        return getInstance();
    }

    public static ClassGraph getInstance() {
        if (instance == null)
            instance = new ClassGraph();
        return instance;
    }

    /** The key of the vertex map needs to be a String because extendedTypes represent extended classes
     * as ClassOrInterfaceType objects while class declarations define classes as ClassOrInterfaceDeclaration
     * objects and there is no relationship to match them */
    private final Map<String, ClassGraph.Vertex> vertexDeclarationMap = new HashMap<>();

    private boolean built = false;

    private ClassGraph() {
        super(null, null, false);
    }

    /** Locates the vertex that represents a given class or interface declaration.
     *  The vertex must exist, or an exception will be thrown. */
    protected Vertex findClassVertex(ClassOrInterfaceDeclaration declaration) {
        return vertexDeclarationMap.get(mapKey(declaration));
    }

    protected Vertex findMethodVertex(CallableDeclaration<?> declaration) {
        return vertexDeclarationMap.get(mapKey(declaration, ASTUtils.getClassNode(declaration)));
    }

    public Set<MethodDeclaration> overriddenSetOf(MethodDeclaration method) {
        return subclassesStreamOf(classVertexOf(findMethodVertex(method)))
                .flatMap(vertex -> outgoingEdgesOf(vertex).stream()
                        .filter(ClassArc.Member.class::isInstance)
                        .map(ClassGraph.this::getEdgeTarget)
                        .filter(v -> v.declaration.isMethodDeclaration())
                        .filter(v -> v.declaration.asMethodDeclaration().getSignature().equals(method.getSignature()))
                        .map(v -> v.declaration.asMethodDeclaration()))
                .collect(Collectors.toSet());
    }

    protected Vertex classVertexOf(Vertex member) {
        assert member.declaration.isFieldDeclaration() ||
                member.declaration.isCallableDeclaration() ||
                member.declaration.isInitializerDeclaration();
        return incomingEdgesOf(member).stream()
                .filter(ClassArc.Member.class::isInstance)
                .map(this::getEdgeSource)
                .findFirst().orElseThrow();
    }

    /** Returns all child classes of the given class, including itself. */
    public Set<ClassOrInterfaceDeclaration> subclassesOf(ClassOrInterfaceDeclaration clazz) {
        return subclassesOf(findClassVertex(clazz));
    }

    /** Returns all child classes of the given class, including itself. */
    public Set<ClassOrInterfaceDeclaration> subclassesOf(ResolvedClassDeclaration clazz) {
        return subclassesOf(vertexDeclarationMap.get(mapKey(clazz)));
    }

    public Set<ClassOrInterfaceDeclaration> subclassesOf(ResolvedReferenceType type) {
        return subclassesOf(vertexDeclarationMap.get(mapKey(type)));
    }

    /** @see #subclassesOf(ClassOrInterfaceDeclaration) */
    protected Set<ClassOrInterfaceDeclaration> subclassesOf(Vertex v) {
        return subclassesStreamOf(v)
                .map(Vertex::getDeclaration)
                .map(ClassOrInterfaceDeclaration.class::cast)
                .collect(Collectors.toSet());
    }

    protected Stream<Vertex> subclassesStreamOf(Vertex classVertex) {
        return Stream.concat(Stream.of(classVertex), outgoingEdgesOf(classVertex).stream()
                .filter(ClassArc.Extends.class::isInstance)
                .map(this::getEdgeTarget)
                .flatMap(this::subclassesStreamOf));
    }

    // TODO: this method ignores default method implementations in interfaces, as can be overridden.
    /** Looks up a method in the graph, going up the class inheritance tree to locate a
     *  matching method. If no match can be found, throws an {@link IllegalArgumentException}. */
    public MethodDeclaration findMethodByTypeAndSignature(ClassOrInterfaceDeclaration type, CallableDeclaration<?> declaration) {
        Vertex v = vertexDeclarationMap.get(mapKey(declaration, type));
        if (v != null && v.declaration.isMethodDeclaration())
            return v.declaration.asMethodDeclaration();
        Optional<ClassOrInterfaceDeclaration> parentType = parentOf(type);
        if (parentType.isEmpty())
            throw new IllegalArgumentException("Cannot find the given declaration: " + declaration);
        return findMethodByTypeAndSignature(parentType.get(), declaration);
    }

    /** Find the parent class or interface of a given class. */
    public Optional<ClassOrInterfaceDeclaration> parentOf(ClassOrInterfaceDeclaration declaration) {
        return incomingEdgesOf(findClassVertex(declaration)).stream()
                .filter(ClassArc.Extends.class::isInstance)
                .map(this::getEdgeSource)
                .map(Vertex::getDeclaration)
                .filter(BodyDeclaration::isClassOrInterfaceDeclaration)
                .map(BodyDeclaration::asClassOrInterfaceDeclaration)
                .findFirst();
    }

    public Optional<ObjectTree> generateObjectTreeForReturnOf(CallableDeclaration<?> callableDeclaration) {
        if (callableDeclaration.isMethodDeclaration()) {
            MethodDeclaration method = callableDeclaration.asMethodDeclaration();
            if (method.getType().isClassOrInterfaceType())
                try {
                    return Optional.of(generateObjectTreeFor(method.getType().asClassOrInterfaceType().resolve()));
                } catch (UnsolvedSymbolException e) {
                    return Optional.empty();
                }
            else
                return Optional.empty();
        } else if (callableDeclaration.isConstructorDeclaration()) {
            return Optional.of(generateObjectTreeFor(ASTUtils.getClassNode(callableDeclaration)));
        } else {
            throw new IllegalArgumentException("Invalid callable declaration type");
        }
    }

    public ObjectTree generateObjectTreeFor(ClassOrInterfaceDeclaration declaration) {
        return generateObjectTreeFor(vertexDeclarationMap.get(mapKey(declaration)));
    }

    public ObjectTree generateObjectTreeFor(ResolvedReferenceType type) {
        return generateObjectTreeFor(vertexDeclarationMap.get(mapKey(type)));
    }

    protected ObjectTree generateObjectTreeFor(Vertex classVertex) {
        if (classVertex == null)
            return new ObjectTree();
        return generateObjectTreeFor(classVertex, new ObjectTree(), "-root-");
    }

    protected ObjectTree generateObjectTreeFor(Vertex classVertex, ObjectTree tree, String level) {
        Map<String, Vertex> classFields = findAllFieldsOf(classVertex);
        for (Map.Entry<String, Vertex> entry : classFields.entrySet()) {
            tree.addField(level + '.' + entry.getKey());
            if (entry.getValue() != null)
                generateObjectTreeFor(entry.getValue(), tree, level + '.' + entry.getKey());
        }
        return tree;
    }

    protected Map<String, Vertex> findAllFieldsOf(Vertex classVertex) {
        assert classVertex.declaration instanceof ClassOrInterfaceDeclaration;
        assert !classVertex.declaration.asClassOrInterfaceDeclaration().isInterface();
        ClassOrInterfaceDeclaration clazz = classVertex.getDeclaration().asClassOrInterfaceDeclaration();
        Map<String, Vertex> fieldMap = new HashMap<>();
        while (clazz != null) {
            for (FieldDeclaration field : clazz.getFields()) {
                for (VariableDeclarator var : field.getVariables()) {
                    if (fieldMap.containsKey(var.getNameAsString()))
                        continue;
                    Vertex v = null;
                    if (var.getType().isClassOrInterfaceType()) {
                        try {
                            v = vertexDeclarationMap.get(mapKey(var.getType().asClassOrInterfaceType().resolve()));
                        } catch (UnsolvedSymbolException ignored) {
                        }
                    }
                    fieldMap.put(var.getNameAsString(), v);
                }
            }
            Optional<ClassOrInterfaceDeclaration> parent = parentOf(clazz);
            if (parent.isEmpty())
                break;
            clazz = parent.get();
        }
        return fieldMap;
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

    protected String mapKey(ClassOrInterfaceDeclaration n) {
        return n.getFullyQualifiedName().orElseThrow();
    }

    protected String mapKey(ResolvedClassDeclaration n) {
        return n.getQualifiedName();
    }

    protected String mapKey(ResolvedReferenceType n) {
        return n.getQualifiedName();
    }

    protected String mapKey(CallableDeclaration<?> declaration, ClassOrInterfaceDeclaration clazz) {
        return clazz.getFullyQualifiedName().orElseThrow() + "." + declaration.getSignature();
    }

    protected String mapKey(FieldDeclaration declaration, ClassOrInterfaceDeclaration clazz) {
        return clazz.getFullyQualifiedName().orElseThrow() + "." + declaration;
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
                assert classStack.peek() != null;
                addFieldDeclaration(n, classStack.peek());
            }

            @Override
            public void visit(MethodDeclaration n, Void arg) {
                assert classStack.peek() != null;
                addCallableDeclaration(n, classStack.peek());
            }

            @Override
            public void visit(ConstructorDeclaration n, Void arg) {
                assert classStack.peek() != null;
                addCallableDeclaration(n, classStack.peek());
            }
        }, null);
    }

    /** Add a class declaration vertex to the class graph */
    protected void addClassDeclaration(ClassOrInterfaceDeclaration n) {
        ClassGraph.Vertex v = new ClassGraph.Vertex(n);
        // Required string to match ClassOrInterfaceType and ClassOrInterfaceDeclaration. QualifiedName Not Valid
        vertexDeclarationMap.put(mapKey(n), v);
        addVertex(v);
    }

    /** Add a field declaration vertex to the class graph */
    protected void addFieldDeclaration(FieldDeclaration n, ClassOrInterfaceDeclaration c){
        ClassGraph.Vertex v = new ClassGraph.Vertex(n);
        vertexDeclarationMap.put(mapKey(n, c), v);
        addVertex(v);
    }

    /** Add a method/constructor declaration vertex to the class graph */
    protected void addCallableDeclaration(CallableDeclaration<?> n, ClassOrInterfaceDeclaration c){
        assert n instanceof ConstructorDeclaration || n instanceof MethodDeclaration;
        ClassGraph.Vertex v = new ClassGraph.Vertex(n);
        vertexDeclarationMap.put(mapKey(n, c), v);
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
                Vertex v = vertexDeclarationMap.get(mapKey(n));
                addClassEdges(v);
                super.visit(n, arg);
                classStack.pop();
            }

            @Override
            public void visit(FieldDeclaration n, Void arg) {
                ClassOrInterfaceDeclaration clazz = classStack.peek();
                assert clazz != null;
                Vertex c = vertexDeclarationMap.get(mapKey(clazz));
                Vertex v = vertexDeclarationMap.get(mapKey(n, clazz));
                addEdge(c, v, new ClassArc.Member());
            }

            @Override
            public void visit(MethodDeclaration n, Void arg) {
                ClassOrInterfaceDeclaration clazz = classStack.peek();
                assert clazz != null;
                Vertex c = vertexDeclarationMap.get(mapKey(clazz));
                Vertex v = vertexDeclarationMap.get(mapKey(n, clazz));
                addEdge(c, v, new ClassArc.Member());
            }

            @Override
            public void visit(ConstructorDeclaration n, Void arg) {
                ClassOrInterfaceDeclaration clazz = classStack.peek();
                assert clazz != null;
                Vertex c = vertexDeclarationMap.get(mapKey(clazz));
                Vertex v = vertexDeclarationMap.get(mapKey(n, clazz));
                addEdge(c, v, new ClassArc.Member());
            }
        }, null);
    }

    protected void addClassEdges(Vertex v){
        assert v.declaration instanceof ClassOrInterfaceDeclaration;
        ClassOrInterfaceDeclaration dv = (ClassOrInterfaceDeclaration) v.declaration;
        dv.getExtendedTypes().forEach(p -> {
            Vertex source = vertexDeclarationMap.get(mapKey(p.resolve()));
            if (source != null && containsVertex(v))
                addEdge(source, v, new ClassArc.Extends());
        });
        dv.getImplementedTypes().forEach(p -> {
            Vertex source = vertexDeclarationMap.get(mapKey(p.resolve()));
            if (source != null && containsVertex(v))
                addEdge(source, v, new ClassArc.Implements());
        });
    }

    /** Creates a graph-appropriate DOT exporter. */
    public DOTExporter<ClassGraph.Vertex, ClassGraph.ClassArc> getDOTExporter() {
        DOTExporter<ClassGraph.Vertex, ClassGraph.ClassArc> dot = new DOTExporter<>();
        dot.setVertexAttributeProvider(vertex -> Utils.dotLabel(vertex.declaration.toString().replaceAll("\\{.*}", "")));
        dot.setEdgeAttributeProvider(edge -> Utils.dotLabel(edge.getClass().getSimpleName()));
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
            return declaration.toString();
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


