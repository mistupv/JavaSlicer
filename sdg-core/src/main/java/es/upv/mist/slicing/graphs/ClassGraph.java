package es.upv.mist.slicing.graphs;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.declarations.ResolvedClassDeclaration;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.utils.Pair;
import es.upv.mist.slicing.nodes.ObjectTree;
import es.upv.mist.slicing.utils.ASTUtils;
import es.upv.mist.slicing.utils.StaticConfig;
import org.jgrapht.graph.DefaultEdge;
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
    private final Map<String, ClassVertex<?>> classDeclarationMap = new HashMap<>();
    /** A map from the field name to its corresponding vertex. Use {@code mapKey(...)} to locate the key. */
    private final Map<Pair<ClassVertex<?>, String>, Vertex<FieldDeclaration>> fieldDeclarationMap = new HashMap<>();
    /** A map from the method's signature to its corresponding vertex. Use {@code mapKey(...)} to locate the key. */
    private final Map<Pair<ClassVertex<?>, String>, Vertex<CallableDeclaration<?>>> methodDeclarationMap = new HashMap<>();

    private boolean built = false;

    private ClassGraph() {
        super(null, null, false);
    }

    /** Locates the vertex that represents a given class or interface declaration.
     *  If the vertex is not contained in the graph, {@code null} will be returned. */
    protected ClassVertex<?> findClassVertex(ClassOrInterfaceDeclaration declaration) {
        return classDeclarationMap.get(mapKey(declaration));
    }

    /** Whether this graph contains the given type as a vertex. */
    public boolean containsType(ResolvedType type) {
        return type.isReferenceType() && classDeclarationMap.containsKey(mapKey(type.asReferenceType()));
    }

    /** Set of method declarations that override the given argument. */
    public Set<MethodDeclaration> overriddenSetOf(MethodDeclaration method) {
        return subclassesStreamOf(findClassVertex(method.findAncestor(ClassOrInterfaceDeclaration.class).orElseThrow()))
                .flatMap(vertex -> outgoingEdgesOf(vertex).stream()
                        .filter(ClassArc.Member.class::isInstance)
                        .map(ClassGraph.this::getEdgeTarget)
                        .map(Vertex::getDeclaration)
                        .filter(MethodDeclaration.class::isInstance)
                        .map(MethodDeclaration.class::cast)
                        .filter(decl -> decl.getSignature().equals(method.getSignature())))
                .collect(Collectors.toSet());
    }

    /** Locates a field declaration within a given type, given its name. */
    public Optional<FieldDeclaration> findClassField(ResolvedType resolvedType, String fieldName) {
        return Optional.ofNullable(classDeclarationMap.get(mapKey(resolvedType.asReferenceType())))
                .flatMap(v -> findClassField(v, fieldName));
    }

    /** @see #findClassField(ResolvedType,String) */
    public Optional<FieldDeclaration> findClassField(ClassVertex<?> vertex, String fieldName) {
        var field = vertex.getFieldByName(fieldName);
        if (field.isPresent())
            return field;
        return incomingEdgesOf(vertex).stream()
                .filter(ClassArc.Extends.class::isInstance)
                .map(this::getEdgeSource)
                .map(ClassVertex.class::cast)
                .findAny()
                .flatMap(parent -> findClassField(parent, fieldName));
    }

    /** Returns all child classes of the given class, including itself. */
    public Stream<ClassVertex<?>> subclassesOf(ClassOrInterfaceDeclaration clazz) {
        return subclassesStreamOf(findClassVertex(clazz));
    }

    /** Returns all child classes of the given class, including itself. */
    public Stream<ClassVertex<?>> subclassesOf(ResolvedClassDeclaration clazz) {
        return subclassesStreamOf(classDeclarationMap.get(mapKey(clazz)));
    }

    public Stream<ClassVertex<?>> subclassesOf(ResolvedReferenceType type) {
        return subclassesStreamOf(classDeclarationMap.get(mapKey(type)));
    }

    /** @see #subclassesOf(ClassOrInterfaceDeclaration) */
    public Set<ClassVertex<?>> subclassesOf(ClassVertex<?> v) {
        return subclassesStreamOf(v).collect(Collectors.toSet());
    }

    public Stream<ClassVertex<?>> subclassesStreamOf(ClassVertex<?> classVertex) {
        return Stream.concat(Stream.of(classVertex), outgoingEdgesOf(classVertex).stream()
                .filter(ClassArc.Extends.class::isInstance)
                .map(this::getEdgeTarget)
                .map(v -> (ClassVertex<?>) v)
                .flatMap(this::subclassesStreamOf));
    }

    // TODO: this method ignores default method implementations in interfaces, as can be overridden.
    /** Looks up a method in the graph, going up the class inheritance tree to locate a
     *  matching method. If no match can be found, throws an {@link IllegalArgumentException}. */
    public MethodDeclaration findMethodByTypeAndSignature(ClassVertex<?> classVertex, CallableDeclaration<?> declaration) {
        Vertex<CallableDeclaration<?>> v = methodDeclarationMap.get(mapKey(declaration, classVertex));
        if (v != null && v.declaration.isMethodDeclaration())
            return v.declaration.asMethodDeclaration();
        Optional<? extends ClassVertex<?>> parentType = parentOf(classVertex);
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
                .filter(ClassOrInterfaceDeclaration.class::isInstance)
                .map(ClassOrInterfaceDeclaration.class::cast)
                .findFirst();
    }

    public Optional<? extends ClassVertex<?>> parentOf(ClassVertex<?> vertex) {
        return incomingEdgesOf(vertex).stream()
                .filter(ClassArc.Extends.class::isInstance)
                .map(this::getEdgeSource)
                .filter(ClassVertex.class::isInstance)
                .map(v -> (ClassVertex<?>) v)
                .findFirst();
    }

    public ClassVertex<?> vertexOf(ResolvedType rt) {
        return Objects.requireNonNull(classDeclarationMap.get(mapKey(rt)), "Class not present in class graph: " + rt.describe());
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

    public Optional<ObjectTree> generateObjectTreeForType(ResolvedType type) {
        if (type.isReferenceType()) {
            ClassVertex<?> v = classDeclarationMap.get(mapKey(type.asReferenceType()));
            if (v != null)
                return Optional.of(generateObjectTreeFor(v));
        }
        return Optional.empty();
    }

    public ObjectTree generateObjectTreeFor(ClassOrInterfaceDeclaration declaration) {
        return generateObjectTreeFor(classDeclarationMap.get(mapKey(declaration)));
    }

    public ObjectTree generateObjectTreeFor(ResolvedReferenceType type) {
        return generateObjectTreeFor(classDeclarationMap.get(mapKey(type)));
    }

    protected ObjectTree generateObjectTreeFor(ClassVertex<?> classVertex) {
        if (classVertex == null)
            return new ObjectTree();
        return generatePolyObjectTreeFor(classVertex, new ObjectTree(), ObjectTree.ROOT_NAME, 0);
    }

    protected ObjectTree generatePolyObjectTreeFor(ClassVertex<?> classVertex, ObjectTree tree, String level, int depth) {
        if (depth >= StaticConfig.K_LIMIT)
            return tree;
        Set<ClassVertex<?>> types = subclassesOf(classVertex);
        if (types.isEmpty()) {
            generateObjectTreeFor(classVertex, tree, level, depth);
        } else {
            for (ClassVertex<?> type : types) {
                if (!findAllFieldsOf(type).isEmpty()) {
                    ObjectTree newType = tree.addType(type, level);
                    generateObjectTreeFor(type, tree, level + '.' + newType.getMemberNode().getLabel(), depth);
                }
            }
        }
        return tree;
    }

    protected void generateObjectTreeFor(ClassVertex<?> classVertex, ObjectTree tree, String level, int depth) {
        Map<String, ClassVertex<?>> classFields = findAllFieldsOf(classVertex);
        for (var entry : classFields.entrySet()) {
            tree.addField(level + '.' + entry.getKey());
            if (entry.getValue() != null)
                generatePolyObjectTreeFor(entry.getValue(), tree, level + '.' + entry.getKey(), depth);
        }
    }

    protected Map<String, ClassVertex<?>> findAllFieldsOf(ClassVertex<?> classVertex) {
        Map<String, ClassVertex<?>> fieldMap = new HashMap<>();
        if (classVertex.isInterface())
            return fieldMap;
        while (classVertex != null) {
            for (FieldDeclaration field : classVertex.getFields()) {
                for (VariableDeclarator var : field.getVariables()) {
                    if (fieldMap.containsKey(var.getNameAsString()))
                        continue;
                    ClassVertex<?> v = null;
                    if (var.getType().isClassOrInterfaceType()) {
                        try {
                            v = classDeclarationMap.get(mapKey(var.getType().asClassOrInterfaceType().resolve()));
                        } catch (UnsolvedSymbolException ignored) {
                        }
                    }
                    fieldMap.put(var.getNameAsString(), v);
                }
            }
            classVertex = parentOf(classVertex).orElse(null);
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

    protected String mapKey(ResolvedType n) {
        if (n.isReferenceType())
            return mapKey(n.asReferenceType());
        return n.describe();
    }

    protected String mapKey(ObjectCreationExpr n) {
        return n.getTokenRange().map(Object::toString).orElse("") +
                n.findCompilationUnit()
                .flatMap(CompilationUnit::getStorage)
                .map(Object::toString)
                .orElse("");
    }

    protected Pair<ClassVertex<?>, String> mapKey(CallableDeclaration<?> declaration, ClassVertex<?> clazz) {
        return new Pair<>(clazz, declaration.getSignature().asString());
    }

    protected Pair<ClassVertex<?>, String> mapKey(FieldDeclaration declaration, ClassVertex<?> clazz) {
        return new Pair<>(clazz, declaration.toString());
    }

    /** Find the class declarations, the field declaration, and method and constructor declarations (vertices)
     * in the given list of compilation units. */
    protected void buildVertices(NodeList<CompilationUnit> arg) {
        arg.accept(new VoidVisitorAdapter<Void>() {
            private final Deque<ClassVertex<?>> classStack = new LinkedList<>();
//            QUESTIONS & LACKS:
//              1) Is it necessary to include something apart from class vertices?
//              2) Private classes inside other classes?
//              3) Static declaration blocks not considered

            @Override
            public void visit(ClassOrInterfaceDeclaration n, Void arg) {
                NamedClassVertex v = new NamedClassVertex(n);
                classDeclarationMap.put(mapKey(n), v);
                addVertex(v);
                classStack.push(v);
                super.visit(n, arg);
                classStack.pop();
            }

            @Override
            public void visit(FieldDeclaration n, Void arg) {
                assert classStack.peek() != null;
                addFieldDeclaration(n, classStack.peek());
                addTypeToGraph(n.getVariable(0).getType());
                super.visit(n, arg);
            }

            @Override
            public void visit(VariableDeclarationExpr n, Void arg) {
                addTypeToGraph(n.getVariable(0).getType());
                super.visit(n, arg);
            }

            @Override
            public void visit(MethodDeclaration n, Void arg) {
                assert classStack.peek() != null;
                addCallableDeclaration(n, classStack.peek());
                if (!n.getType().isVoidType())
                    addTypeToGraph(n.getType());
                for (Parameter parameter : n.getParameters())
                    addTypeToGraph(parameter.getType());
                super.visit(n, arg);
            }

            @Override
            public void visit(ConstructorDeclaration n, Void arg) {
                assert classStack.peek() != null;
                addCallableDeclaration(n, classStack.peek());
                for (Parameter parameter : n.getParameters())
                    addTypeToGraph(parameter.getType());
                super.visit(n, arg);
            }

            @Override
            public void visit(ObjectCreationExpr n, Void arg) {
                if (n.getAnonymousClassBody().isPresent()) {
                    AnonymousClassVertex vertex = new AnonymousClassVertex(n);
                    classDeclarationMap.put(mapKey(n), vertex);
                    addVertex(vertex);
                    classStack.push(vertex);
                    n.getAnonymousClassBody().get().accept(this, arg);
                    classStack.pop();
                    n.getScope().ifPresent(scope -> scope.accept(this, arg));
                    n.getArguments().accept(this, arg);
                } else {
                    super.visit(n, arg);
                }
            }

            @Override
            public void visit(NameExpr n, Void arg) {
                addTypeToGraph(n);
                super.visit(n, arg);
            }

            @Override
            public void visit(FieldAccessExpr n, Void arg) {
                addTypeToGraph(n);
                super.visit(n, arg);
            }

            @Override
            public void visit(CatchClause n, Void arg) {
                if (n.getParameter().getType().isUnionType())
                    for (ReferenceType t : n.getParameter().getType().asUnionType().getElements())
                        addTypeToGraph(t);
                else
                    addTypeToGraph(n.getParameter().getType());
                super.visit(n, arg);
            }

            @Override
            public void visit(NullLiteralExpr n, Void arg) {
                addTypeToGraph(n);
                super.visit(n, arg);
            }
        }, null);
    }

    protected void addTypeToGraph(Type type) {
        LibraryClassVertex v = new LibraryClassVertex(type);
        if (addVertex(v))
            classDeclarationMap.put(mapKey(v.getResolvedType()), v);
    }

    protected void addTypeToGraph(Expression e) {
        LibraryClassVertex v = new LibraryClassVertex(e);
        if (addVertex(v))
            classDeclarationMap.put(mapKey(v.getResolvedType()), v);
    }

    @Override
    public boolean addVertex(Vertex<?> vertex) {
        if (vertex instanceof LibraryClassVertex && containsType((ClassVertex<?>) vertex))
            return false;
        if (vertex instanceof NamedClassVertex && containsType((ClassVertex<?>) vertex))
            return replaceVertex((NamedClassVertex) vertex);
        return super.addVertex(vertex);
    }

    public boolean containsType(ClassVertex<?> vertex) {
        return vertexSet().stream()
                .filter(ClassVertex.class::isInstance)
                .map(ClassVertex.class::cast)
                .anyMatch(v -> Objects.equals(v.getQualifiedName(), vertex.getQualifiedName()));
    }

    public boolean replaceVertex(NamedClassVertex vertex) {
        // 1. find equivalent vertex (will be library vertex)
        List<ClassVertex<?>> equivalents = vertexSet().stream()
                .filter(ClassVertex.class::isInstance)
                .map(v -> (ClassVertex<?>) v)
                .filter(v -> Objects.equals(v.getQualifiedName(), vertex.getQualifiedName()))
                .collect(Collectors.toList());
        // 2. remove vertices
        removeAllVertices(equivalents);
        // 3. Add vertex
        return super.addVertex(vertex);
    }

    /** Add a field declaration vertex to the class graph */
    protected void addFieldDeclaration(FieldDeclaration n, ClassVertex<?> c){
        ClassGraph.Vertex<FieldDeclaration> v = new ClassGraph.Vertex<>(n);
        fieldDeclarationMap.put(mapKey(n, c), v);
        addVertex(v);
    }

    /** Add a method/constructor declaration vertex to the class graph */
    protected void addCallableDeclaration(CallableDeclaration<?> n, ClassVertex<?> c){
        assert n instanceof ConstructorDeclaration || n instanceof MethodDeclaration;
        ClassGraph.Vertex<CallableDeclaration<?>> v = new ClassGraph.Vertex<>(n);
        methodDeclarationMap.put(mapKey(n, c), v);
        addVertex(v);
    }

    /** Find the class declarations, field declarations, and method declarations and build the corresponding
     * member/extends/implements relationships in the given list of compilation units. */
    protected void buildEdges(NodeList<CompilationUnit> arg) {
        arg.accept(new VoidVisitorAdapter<Void>() {
            private final Deque<ClassVertex<?>> classStack = new LinkedList<>();

            @Override
            public void visit(ClassOrInterfaceDeclaration n, Void arg) {
                ClassVertex<?> v = classDeclarationMap.get(mapKey(n));
                classStack.push(v);
                addClassEdges(v);
                super.visit(n, arg);
                classStack.pop();
            }

            @Override
            public void visit(ObjectCreationExpr n, Void arg) {
                if (n.getAnonymousClassBody().isPresent()) {
                    ClassVertex<?> v = classDeclarationMap.get(mapKey(n));
                    classStack.push(v);
                    addClassEdges(v);
                    n.getAnonymousClassBody().get().accept(this, arg);
                    classStack.pop();
                    n.getScope().ifPresent(scope -> scope.accept(this, arg));
                    n.getArguments().accept(this, arg);
                } else {
                    super.visit(n, arg);
                }
            }

            @Override
            public void visit(FieldDeclaration n, Void arg) {
                assert !classStack.isEmpty();
                ClassVertex<?> c = classStack.peek();
                Vertex<FieldDeclaration> v = fieldDeclarationMap.get(mapKey(n, c));
                addEdge(c, v, new ClassArc.Member());
                super.visit(n, arg);
            }

            @Override
            public void visit(MethodDeclaration n, Void arg) {
                assert !classStack.isEmpty();
                ClassVertex<?> c = classStack.peek();
                Vertex<CallableDeclaration<?>> v = methodDeclarationMap.get(mapKey(n, c));
                addEdge(c, v, new ClassArc.Member());
                super.visit(n, arg);
            }

            @Override
            public void visit(ConstructorDeclaration n, Void arg) {
                assert !classStack.isEmpty();
                ClassVertex<?> c = classStack.peek();
                Vertex<CallableDeclaration<?>> v = methodDeclarationMap.get(mapKey(n, c));
                addEdge(c, v, new ClassArc.Member());
                super.visit(n, arg);
            }
        }, null);
    }

    protected void addClassEdges(ClassVertex<?> v) {
        v.getExtendedTypes().forEach(p -> {
            Vertex<?> source = classDeclarationMap.get(mapKey(p.resolve()));
            if (source != null && containsVertex(v))
                addEdge(source, v, new ClassArc.Extends());
        });
        v.getImplementedTypes().forEach(p -> {
            Vertex<?> source = classDeclarationMap.get(mapKey(p.resolve()));
            if (source != null && containsVertex(v))
                addEdge(source, v, new ClassArc.Implements());
        });
    }

    /** A vertex containing the declaration it represents. It only exists because
     *  JGraphT relies heavily on equals comparison, which may not be correct in declarations. */
    public static class Vertex<T extends Node> {
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
            if (declaration instanceof CallableDeclaration<?>)
                return ((CallableDeclaration<?>) declaration).getDeclarationAsString(false, false, false);
            return declaration.toString();
        }
    }

    public static abstract class ClassVertex<T extends Node> extends Vertex<T> {
        public ClassVertex(T declaration) {
            super(declaration);
        }

        public abstract Optional<FieldDeclaration> getFieldByName(String fieldName);

        public abstract List<FieldDeclaration> getFields();

        public abstract boolean isInterface();

        public abstract List<ClassOrInterfaceType> getExtendedTypes();

        public abstract List<ClassOrInterfaceType> getImplementedTypes();

        public abstract String describe();

        public abstract String getQualifiedName();

        public abstract boolean isUserDefined();

        @Override
        public String toString() {
            return describe();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this)
                return true;
            return obj instanceof ClassVertex<?> &&
                    Objects.equals(getQualifiedName(), ((ClassVertex<?>) obj).getQualifiedName());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getQualifiedName());
        }
    }

    protected static class NamedClassVertex extends ClassVertex<ClassOrInterfaceDeclaration> {
        public NamedClassVertex(ClassOrInterfaceDeclaration declaration) {
            super(declaration);
        }

        @Override
        public Optional<FieldDeclaration> getFieldByName(String fieldName) {
            return declaration.getFieldByName(fieldName);
        }

        @Override
        public List<FieldDeclaration> getFields() {
            return declaration.getFields();
        }

        @Override
        public boolean isInterface() {
            return declaration.isInterface();
        }

        @Override
        public List<ClassOrInterfaceType> getExtendedTypes() {
            return declaration.getExtendedTypes();
        }

        @Override
        public List<ClassOrInterfaceType> getImplementedTypes() {
            return declaration.getImplementedTypes();
        }

        @Override
        public String describe() {
            return ASTUtils.resolvedTypeDeclarationToResolvedType(declaration.resolve()).describe();
        }

        @Override
        public String getQualifiedName() {
            return declaration.getFullyQualifiedName().orElseThrow();
        }

        @Override
        public boolean isUserDefined() {
            return true;
        }
    }

    protected static class LibraryClassVertex extends ClassVertex<Node> {

        protected final String qualifiedName;
        protected final ResolvedType type;

        public LibraryClassVertex(Type declaration) {
            super(declaration);
            if (declaration.isArrayType())
                type = declaration.asArrayType().getComponentType().resolve();
            else
                type = declaration.resolve();
            qualifiedName = generateQualifiedName();
        }

        public LibraryClassVertex(Expression expression) {
            super(expression);
            type = expression.calculateResolvedType();
            qualifiedName = generateQualifiedName();
        }

        protected String generateQualifiedName() {
            if (type.isReferenceType())
                return type.asReferenceType().getTypeDeclaration().get().getQualifiedName();
            else
                return type.describe();
        }

        public ResolvedType getResolvedType() {
            return type;
        }

        @Override
        public Optional<FieldDeclaration> getFieldByName(String fieldName) {
            return Optional.empty();
        }

        @Override
        public List<FieldDeclaration> getFields() {
            return Collections.emptyList();
        }

        @Override
        public boolean isInterface() {
            return type.isReferenceType() &&
                    type.asReferenceType().getTypeDeclaration().get().isInterface();
        }

        @Override
        public List<ClassOrInterfaceType> getExtendedTypes() {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ClassOrInterfaceType> getImplementedTypes() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String describe() {
            return type.describe();
        }

        @Override
        public String getQualifiedName() {
            return qualifiedName;
        }

        @Override
        public boolean isUserDefined() {
            return false;
        }
    }

    protected static class AnonymousClassVertex extends ClassVertex<ObjectCreationExpr> {
        private static int counter = 0;

        protected final int number = counter++;

        public AnonymousClassVertex(ObjectCreationExpr declaration) {
            super(declaration);
            if (declaration.getAnonymousClassBody().isEmpty())
                throw new IllegalArgumentException("The object creation must have an anonymous class!");
        }

        @Override
        public Optional<FieldDeclaration> getFieldByName(String fieldName) {
            for (BodyDeclaration<?> decl : declaration.getAnonymousClassBody().get())
                if (decl.isFieldDeclaration())
                    for (VariableDeclarator var : decl.asFieldDeclaration().getVariables())
                        if (Objects.equals(var.getNameAsString(), fieldName))
                            return Optional.of(decl.asFieldDeclaration());
            return Optional.empty();
        }

        @Override
        public List<FieldDeclaration> getFields() {
            return declaration.getAnonymousClassBody().get().stream()
                    .filter(BodyDeclaration::isFieldDeclaration)
                    .map(BodyDeclaration::asFieldDeclaration)
                    .collect(Collectors.toList());
        }

        @Override
        public boolean isInterface() {
            return false;
        }

        @Override
        public List<ClassOrInterfaceType> getExtendedTypes() {
            return getTypes(false);
        }

        @Override
        public List<ClassOrInterfaceType> getImplementedTypes() {
            return getTypes(true);
        }

        protected List<ClassOrInterfaceType> getTypes(boolean implemented) {
            // TODO: anonymous class of a JRE type
            if (implemented == declaration.getType().resolve().getTypeDeclaration().get().isInterface())
                return List.of(declaration.getType());
            return Collections.emptyList();
        }

        @Override
        public String describe() {
            return declaration.getTypeAsString() + number;
        }

        @Override
        public String getQualifiedName() {
            return describe();
        }

        @Override
        public boolean isUserDefined() {
            return true;
        }
    }

    public static class ClassArc extends DefaultEdge {
        /** An arc that connects a class with another one that inherits from it. */
        protected static class Extends extends ClassArc {}
        /** An arc that connects an interface to a class that implements it. */
        protected static class Implements extends ClassArc {}
        /** An arc that connects a class with a field or method contained in it. */
        protected static class Member extends ClassArc {}
    }
}


