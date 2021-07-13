package es.upv.mist.slicing.graphs;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import es.upv.mist.slicing.arcs.Arc;
import es.upv.mist.slicing.nodes.ObjectTree;
import es.upv.mist.slicing.utils.ASTUtils;
import es.upv.mist.slicing.utils.StaticConfig;
import org.jgrapht.graph.DirectedPseudograph;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClassGraph extends DirectedPseudograph<ClassGraph.Vertex<?>, ClassGraph.ClassArc> implements Buildable<NodeList<CompilationUnit>> {
    private static ClassGraph instance = null;

    /** Generates and returns a new class graph. This destroys the reference to the previous instance. */
    public static ClassGraph getNewInstance() {
        instance = null;
        return getInstance();
    }

    public static ClassGraph getInstance() {
        if (instance == null)
            instance = new ClassGraph();
        return instance;
    }

    /** A map from the FQ class name to its corresponding vertex. Use {@code mapKey(...)} to locate the key. */
    private final Map<String, ClassGraph.Vertex<? extends TypeDeclaration<?>>> classDeclarationMap = new HashMap<>();
    /** A map from the field name to its corresponding vertex. Use {@code mapKey(...)} to locate the key. */
    private final Map<String, ClassGraph.Vertex<FieldDeclaration>> fieldDeclarationMap = new HashMap<>();
    /** A map from the method's signature to its corresponding vertex. Use {@code mapKey(...)} to locate the key. */
    private final Map<String, ClassGraph.Vertex<CallableDeclaration<?>>> methodDeclarationMap = new HashMap<>();

    private boolean built = false;

    private ClassGraph() {
        super(null, null, false);
    }

    public Collection<ClassGraph.Vertex<? extends TypeDeclaration<?>>> typeVertices() {
        return classDeclarationMap.values();
    }

    /** Locates the vertex that represents a given class or interface declaration.
     *  If the vertex is not contained in the graph, {@code null} will be returned. */
    protected Vertex<? extends TypeDeclaration<?>> findClassVertex(TypeDeclaration<?> declaration) {
        return classDeclarationMap.get(mapKey(declaration));
    }

    /** Whether this graph contains the given type as a vertex. */
    public boolean containsType(ResolvedType type) {
        return type.isReferenceType() && classDeclarationMap.containsKey(mapKey(type.asReferenceType()));
    }

    /** Set of method declarations that override the given argument. */
    public Set<MethodDeclaration> overriddenSetOf(MethodDeclaration method) {
        return subclassesStreamOf(findClassVertex(method.findAncestor(TypeDeclaration.class).orElseThrow()))
                .flatMap(vertex -> outgoingEdgesOf(vertex).stream()
                        .filter(ClassArc.Member.class::isInstance)
                        .map(ClassGraph.this::getEdgeTarget)
                        .filter(v -> v.declaration.isMethodDeclaration())
                        .filter(v -> v.declaration.asMethodDeclaration().getSignature().equals(method.getSignature()))
                        .map(v -> v.declaration.asMethodDeclaration()))
                .collect(Collectors.toSet());
    }

    /** Locates a field declaration within a given type, given its name. */
    public Optional<FieldDeclaration> findClassField(ResolvedType resolvedType, String fieldName) {
        return Optional.ofNullable(classDeclarationMap.get(mapKey(resolvedType.asReferenceType())))
                .flatMap(v -> findClassField(v, fieldName));
    }

    /** @see #findClassField(ResolvedType,String) */
    @SuppressWarnings("unchecked")
    public Optional<FieldDeclaration> findClassField(Vertex<? extends TypeDeclaration<?>> vertex, String fieldName) {
        var field = vertex.getDeclaration().getFieldByName(fieldName);
        if (field.isPresent())
            return field;
        return incomingEdgesOf(vertex).stream()
                .filter(ClassArc.Extends.class::isInstance)
                .map(this::getEdgeSource)
                .map(v -> (Vertex<? extends TypeDeclaration<?>>) v)
                .findAny()
                .flatMap(parent -> findClassField(parent, fieldName));
    }

    /** Returns all child classes of the given class, including itself. */
    public Set<? extends TypeDeclaration<?>> subclassesOf(TypeDeclaration<?> clazz) {
        return subclassesOf(findClassVertex(clazz));
    }

    /** Returns all child classes of the given class, including itself. */
    public Set<? extends TypeDeclaration<?>> subclassesOf(ResolvedClassDeclaration clazz) {
        return subclassesOf(classDeclarationMap.get(mapKey(clazz)));
    }

    public Set<? extends TypeDeclaration<?>> subclassesOf(ResolvedReferenceType type) {
        return subclassesOf(classDeclarationMap.get(mapKey(type)));
    }

    /** @see #subclassesOf(TypeDeclaration) */
    protected Set<? extends TypeDeclaration<?>> subclassesOf(Vertex<? extends TypeDeclaration<?>> v) {
        if (v.getDeclaration() instanceof EnumDeclaration)
            return Set.of(v.getDeclaration());
        return subclassesStreamOf(v)
                .map(Vertex::getDeclaration)
                .map(ClassOrInterfaceDeclaration.class::cast)
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    protected Stream<Vertex<? extends TypeDeclaration<?>>> subclassesStreamOf(Vertex<? extends TypeDeclaration<?>> classVertex) {
        return Stream.concat(Stream.of(classVertex), outgoingEdgesOf(classVertex).stream()
                .filter(ClassArc.Extends.class::isInstance)
                .map(this::getEdgeTarget)
                .map(v -> (Vertex<? extends TypeDeclaration<?>>) v)
                .flatMap(this::subclassesStreamOf));
    }

    // TODO: this method ignores default method implementations in interfaces, as can be overridden.
    /** Looks up a method in the graph, going up the class inheritance tree to locate a
     *  matching method. If no match can be found, throws an {@link IllegalArgumentException}. */
    public MethodDeclaration findMethodByTypeAndSignature(TypeDeclaration<?> type, CallableDeclaration<?> declaration) {
        Vertex<CallableDeclaration<?>> v = methodDeclarationMap.get(mapKey(declaration, type));
        if (v != null && v.declaration.isMethodDeclaration())
            return v.declaration.asMethodDeclaration();
        if (type.isClassOrInterfaceDeclaration()) {
            Optional<ClassOrInterfaceDeclaration> parentType = parentOf(type.asClassOrInterfaceDeclaration());
            if (parentType.isPresent())
                return findMethodByTypeAndSignature(parentType.get(), declaration);
        }
        throw new IllegalArgumentException("Cannot find the given declaration: " + declaration);
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
            return Optional.of(generateObjectTreeFor(callableDeclaration.findAncestor(TypeDeclaration.class).orElseThrow()));
        } else {
            throw new IllegalArgumentException("Invalid callable declaration type");
        }
    }

    public Optional<ObjectTree> generateObjectTreeForType(ResolvedType type) {
        if (type.isReferenceType()) {
            Vertex<? extends TypeDeclaration<?>> v = classDeclarationMap.get(mapKey(type.asReferenceType()));
            if (v != null)
                return Optional.of(generateObjectTreeFor(v));
        }
        return Optional.empty();
    }

    public ObjectTree generateObjectTreeFor(TypeDeclaration<?> declaration) {
        return generateObjectTreeFor(classDeclarationMap.get(mapKey(declaration)));
    }

    public ObjectTree generateObjectTreeFor(ResolvedReferenceType type) {
        return generateObjectTreeFor(classDeclarationMap.get(mapKey(type)));
    }

    protected ObjectTree generateObjectTreeFor(Vertex<? extends TypeDeclaration<?>> classVertex) {
        if (classVertex == null)
            return new ObjectTree();
        return generatePolyObjectTreeFor(classVertex, new ObjectTree(), ObjectTree.ROOT_NAME, 0);
    }

    protected ObjectTree generatePolyObjectTreeFor(Vertex<? extends TypeDeclaration<?>> classVertex, ObjectTree tree, String level, int depth) {
        if (depth >= StaticConfig.K_LIMIT)
            return tree;
        Set<? extends TypeDeclaration<?>> types = subclassesOf(classVertex);
        if (types.isEmpty()) {
            generateObjectTreeFor(classVertex, tree, level, depth);
        } else {
            for (TypeDeclaration<?> type : types) {
                Vertex<? extends TypeDeclaration<?>> subclassVertex = classDeclarationMap.get(mapKey(type));
                if (!findAllFieldsOf(subclassVertex).isEmpty()) {
                    ObjectTree newType = tree.addType(ASTUtils.resolvedTypeDeclarationToResolvedType(type.resolve()), level);
                    generateObjectTreeFor(subclassVertex, tree, level + '.' + newType.getMemberNode().getLabel(), depth);
                }
            }
        }
        return tree;
    }

    protected void generateObjectTreeFor(Vertex<? extends TypeDeclaration<?>> classVertex, ObjectTree tree, String level, int depth) {
        Map<String, Vertex<? extends TypeDeclaration<?>>> classFields = findAllFieldsOf(classVertex);
        for (var entry : classFields.entrySet()) {
            tree.addField(level + '.' + entry.getKey());
            if (entry.getValue() != null)
                generatePolyObjectTreeFor(entry.getValue(), tree, level + '.' + entry.getKey(), depth);
        }
    }

    protected Map<String, Vertex<? extends TypeDeclaration<?>>> findAllFieldsOf(Vertex<? extends TypeDeclaration<?>> classVertex) {
        TypeDeclaration<?> type = classVertex.getDeclaration();
        assert !type.isClassOrInterfaceDeclaration() ||
                !type.asClassOrInterfaceDeclaration().isInterface();
        Map<String, Vertex<? extends TypeDeclaration<?>>> fieldMap = new HashMap<>();
        while (type != null) {
            for (FieldDeclaration field : type.getFields()) {
                for (VariableDeclarator var : field.getVariables()) {
                    if (fieldMap.containsKey(var.getNameAsString()))
                        continue;
                    Vertex<? extends TypeDeclaration<?>> v = null;
                    if (var.getType().isClassOrInterfaceType()) {
                        try {
                            v = classDeclarationMap.get(mapKey(var.getType().asClassOrInterfaceType().resolve()));
                        } catch (UnsolvedSymbolException ignored) {
                        }
                    }
                    fieldMap.put(var.getNameAsString(), v);
                }
            }
            if (type.isClassOrInterfaceDeclaration()) {
                type = parentOf(type.asClassOrInterfaceDeclaration()).orElse(null);
            } else {
                type = null;
            }
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

    protected String mapKey(TypeDeclaration<?> n) {
        return n.getFullyQualifiedName().orElseThrow();
    }

    protected String mapKey(ResolvedClassDeclaration n) {
        return n.getQualifiedName();
    }

    protected String mapKey(ResolvedReferenceType n) {
        return n.getQualifiedName();
    }

    protected String mapKey(CallableDeclaration<?> declaration, TypeDeclaration<?> clazz) {
        return clazz.getFullyQualifiedName().orElseThrow() + "." + declaration.getSignature();
    }

    protected String mapKey(FieldDeclaration declaration, TypeDeclaration<?> clazz) {
        return clazz.getFullyQualifiedName().orElseThrow() + "." + declaration;
    }

    /** Find the class declarations, the field declaration, and method and constructor declarations (vertices)
     * in the given list of compilation units. */
    protected void buildVertices(NodeList<CompilationUnit> arg) {
        arg.accept(new VoidVisitorAdapter<Void>() {
            private final Deque<TypeDeclaration<?>> typeStack = new LinkedList<>();
//            QUESTIONS & LACKS:
//              1) Is it necessary to include something apart from class vertices?
//              2) Private classes inside other classes?
//              3) Static declaration blocks not considered

            @Override
            public void visit(ClassOrInterfaceDeclaration n, Void arg) {
                typeStack.push(n);
                addTypeDeclaration(n);
                super.visit(n, arg);
                typeStack.pop();
            }

            @Override
            public void visit(EnumDeclaration n, Void arg) {
                typeStack.push(n);
                addTypeDeclaration(n);
                super.visit(n, arg);
                typeStack.pop();
            }

            @Override
            public void visit(FieldDeclaration n, Void arg) {
                assert typeStack.peek() != null;
                addFieldDeclaration(n, typeStack.peek());
            }

            @Override
            public void visit(MethodDeclaration n, Void arg) {
                assert typeStack.peek() != null;
                addCallableDeclaration(n, typeStack.peek());
            }

            @Override
            public void visit(ConstructorDeclaration n, Void arg) {
                assert typeStack.peek() != null;
                addCallableDeclaration(n, typeStack.peek());
            }
        }, null);
    }

    /** Add a type declaration vertex to the class graph, to represent classes and enums. */
    protected void addTypeDeclaration(TypeDeclaration<?> n) {
        ClassGraph.Vertex<TypeDeclaration<?>> v = new ClassGraph.Vertex<>(n);
        classDeclarationMap.put(mapKey(n), v);
        addVertex(v);
    }

    /** Add a field declaration vertex to the class graph */
    protected void addFieldDeclaration(FieldDeclaration n, TypeDeclaration<?> c){
        ClassGraph.Vertex<FieldDeclaration> v = new ClassGraph.Vertex<>(n);
        fieldDeclarationMap.put(mapKey(n, c), v);
        addVertex(v);
    }

    /** Add a method/constructor declaration vertex to the class graph */
    protected void addCallableDeclaration(CallableDeclaration<?> n, TypeDeclaration<?> c){
        assert n instanceof ConstructorDeclaration || n instanceof MethodDeclaration;
        ClassGraph.Vertex<CallableDeclaration<?>> v = new ClassGraph.Vertex<>(n);
        methodDeclarationMap.put(mapKey(n, c), v);
        addVertex(v);
    }

    /** Find the class declarations, field declarations, and method declarations and build the corresponding
     * member/extends/implements relationships in the given list of compilation units. */
    protected void buildEdges(NodeList<CompilationUnit> arg) {
        arg.accept(new VoidVisitorAdapter<Void>() {
            private final Deque<TypeDeclaration<?>> typeStack = new LinkedList<>();

            @Override
            public void visit(ClassOrInterfaceDeclaration n, Void arg) {
                typeStack.push(n);
                var v = classDeclarationMap.get(mapKey(n));
                addClassEdges(v);
                super.visit(n, arg);
                typeStack.pop();
            }

            @Override
            public void visit(EnumDeclaration n, Void arg) {
                typeStack.push(n);
                super.visit(n, arg);
                typeStack.pop();
            }

            @Override
            public void visit(FieldDeclaration n, Void arg) {
                assert !typeStack.isEmpty();
                TypeDeclaration<?> type = typeStack.peek();
                var c = classDeclarationMap.get(mapKey(type));
                Vertex<FieldDeclaration> v = fieldDeclarationMap.get(mapKey(n, type));
                addEdge(c, v, new ClassArc.Member());
            }

            @Override
            public void visit(MethodDeclaration n, Void arg) {
                assert !typeStack.isEmpty();
                TypeDeclaration<?> type = typeStack.peek();
                var c = classDeclarationMap.get(mapKey(type));
                Vertex<CallableDeclaration<?>> v = methodDeclarationMap.get(mapKey(n, type));
                addEdge(c, v, new ClassArc.Member());
            }

            @Override
            public void visit(ConstructorDeclaration n, Void arg) {
                assert !typeStack.isEmpty();
                TypeDeclaration<?> type = typeStack.peek();
                var c = classDeclarationMap.get(mapKey(type));
                Vertex<CallableDeclaration<?>> v = methodDeclarationMap.get(mapKey(n, type));
                addEdge(c, v, new ClassArc.Member());
            }
        }, null);
    }

    protected void addClassEdges(Vertex<? extends TypeDeclaration<?>> v) {
        if (v.declaration instanceof EnumDeclaration)
            return; // nothing to do, it is final and cannot extend nor implement user-defined types
        ClassOrInterfaceDeclaration c = (ClassOrInterfaceDeclaration) v.declaration;
        c.getExtendedTypes().forEach(p -> {
            Vertex<?> source = classDeclarationMap.get(mapKey(p.resolve()));
            if (source != null && containsVertex(v))
                addEdge(source, v, new ClassArc.Extends());
        });
        c.getImplementedTypes().forEach(p -> {
            Vertex<?> source = classDeclarationMap.get(mapKey(p.resolve()));
            if (source != null && containsVertex(v))
                addEdge(source, v, new ClassArc.Implements());
        });
    }

    /** A vertex containing the declaration it represents. It only exists because
     *  JGraphT relies heavily on equals comparison, which may not be correct in declarations. */
    public static class Vertex<T extends BodyDeclaration<?>> {
        // First ancestor common class in the JavaParser hierarchy for
        // ClassOrInterfaceDeclaration, FieldDeclaration and CallableDeclaration
        protected final T declaration;

        public Vertex(T declaration) {
            this.declaration = declaration;
        }

        /** The declaration represented by this node. */
        public T getDeclaration() {
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

    public static class ClassArc extends Arc {
        /** An arc that connects a class with another one that inherits from it. */
        protected static class Extends extends ClassArc {}
        /** An arc that connects an interface to a class that implements it. */
        protected static class Implements extends ClassArc {}
        /** An arc that connects a class with a field or method contained in it. */
        protected static class Member extends ClassArc {}
    }
}


