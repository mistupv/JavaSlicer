package es.upv.mist.slicing.nodes;

import com.github.javaparser.resolution.types.ResolvedType;
import es.upv.mist.slicing.nodes.oo.MemberNode;
import es.upv.mist.slicing.nodes.oo.PolyMemberNode;
import es.upv.mist.slicing.utils.Utils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static es.upv.mist.slicing.graphs.cfg.CFGBuilder.VARIABLE_NAME_OUTPUT;
import static es.upv.mist.slicing.graphs.exceptionsensitive.ESCFG.ACTIVE_EXCEPTION_VARIABLE;

/**
 * A tree data structure that mimics the tree found in an object's fields.
 * Each tree contains a MemberNode that represents its, including a name.
 * If the variable is undefined when the tree is created, the root of this
 * tree will be named "-root-". The real name of the root can be found in
 * its associated VariableAction. <br/>
 *
 * Object trees may not be reused, and must be cloned via {@link #clone()}.
 * Otherwise, the MemberNodes representing the tree will be the same in the graph.
 */
public class ObjectTree implements Cloneable {
    /** The default name of a tree's root. */
    public static final String ROOT_NAME = "-root-";

    /** Regex pattern to split the root from the fields of a field access expression. */
    private static final Pattern FIELD_SPLIT = Pattern.compile("^(?<root>(([_0-9A-Za-z]+\\.)*this)|([_0-9A-Za-z]+)|(" + ROOT_NAME + ")|(" + VARIABLE_NAME_OUTPUT + ")|(" + ACTIVE_EXCEPTION_VARIABLE + "))(\\.(?<fields>.+))?$");

    /** Direct children of this tree node, mapped by field name. */
    private final Map<String, ObjectTree> childrenMap = new HashMap<>();
    /** The MemberNode that represents this tree node in the PDG and SDG. */
    private final MemberNode memberNode;

    /** Create a root of a new object tree with the default name. */
    public ObjectTree() {
        this(ROOT_NAME);
    }

    /** Create a root of a new object tree with the given name. */
    public ObjectTree(String memberName) {
        this(new MemberNode(memberName, null));
    }

    /** Create a child tree node for the given field, whose node is linked to the given parent. */
    private ObjectTree(String memberName, ObjectTree parent) {
        this(new MemberNode(memberName, parent.memberNode));
    }

    /** Create a child tree node for the given type, whose node is linked to the given parent. */
    private ObjectTree(ResolvedType resolvedType, ObjectTree parent) {
        this(new PolyMemberNode(resolvedType, parent.memberNode));
    }

    /** Create a child tree with the given member node. */
    private ObjectTree(MemberNode memberNode) {
        this.memberNode = memberNode;
    }

    /** The name of the variable or field represented by this tree. It doesn't include ancestors. */
    protected String getMemberName() {
        return memberNode == null ? ROOT_NAME : memberNode.getLabel();
    }

    public MemberNode getMemberNode() {
        return memberNode;
    }

    /** Whether this object tree has fields. */
    public boolean hasChildren() {
        return !childrenMap.isEmpty();
    }

    /** Whether the field passed as argument has children. */
    public boolean hasChildren(String memberWithRoot) {
        String member = removeRoot(memberWithRoot);
        if (member.isEmpty())
            return hasChildren();
        return hasChildrenInternal(member);
    }

    protected boolean hasChildrenInternal(String members) {
        if (members.contains(".")) {
            int firstDot = members.indexOf('.');
            String first = members.substring(0, firstDot);
            String rest = members.substring(firstDot + 1);
            childrenMap.computeIfAbsent(first, f -> new ObjectTree(f, this));
            return childrenMap.get(first).hasChildrenInternal(rest);
        } else {
            return childrenMap.get(members).hasChildren();
        }
    }

    /** Whether this object tree immediately contains polymorphic nodes. */
    public boolean hasPoly() {
        return childrenMap.values().stream().anyMatch(ot -> ot.getMemberNode() instanceof PolyMemberNode);
    }

    /** A set of entry pairs, containing the field name and its corresponding tree. It is unmodifiable. */
    public Set<Map.Entry<String, ObjectTree>> entrySet() {
        return Collections.unmodifiableSet(childrenMap.entrySet());
    }

    /** Insert a polymorphic node for the given type. The type node will be
     *  generated immediately beneath this tree node. */
    public ObjectTree addType(ResolvedType rt) {
        assert !rt.describe().isBlank();
        assert !(memberNode instanceof PolyMemberNode);
        return childrenMap.computeIfAbsent(rt.describe(), n -> new ObjectTree(rt, this));
    }

    /**
     * Insert a field with the given name. This method should only be called on a root object tree.
     * This method may be used to add multiple levels simultaneously, calling this method with
     * the argument {@code "a.b.c"} on a new root tree, it will create the tree "b" inside the root
     * and "c" inside "b".
     * @param fieldName The field to be added, should include the root variable name. For example,
     *                  to add the field "x" to a variable "a", this argument should be "a.x".
     */
    public ObjectTree addField(String fieldName) {
        String members = removeRoot(fieldName);
        return addNonRootField(members);
    }

    /** Insert a field in the current level of object tree. The field should be a variable name,
     *  and not contain dots or be blank. */
    public ObjectTree addImmediateField(String fieldName) {
        if (fieldName.contains(".") || fieldName.isBlank())
            throw new IllegalArgumentException("field name must not include dots or be blank!");
        return childrenMap.computeIfAbsent(fieldName, f -> new ObjectTree(f, this));
    }

    /** Similar to {@link #addField(String)}, but may be called at any level
     *  and the argument must not contain the root variable. */
    private ObjectTree addNonRootField(String members) {
        if (members.contains(".")) {
            int firstDot = members.indexOf('.');
            String first = members.substring(0, firstDot);
            String rest = members.substring(firstDot + 1);
            childrenMap.computeIfAbsent(first, f -> new ObjectTree(f, this));
            return childrenMap.get(first).addNonRootField(rest);
        } else {
            return childrenMap.computeIfAbsent(members, f -> new ObjectTree(f, this));
        }
    }

    /** Copies the structure of another object tree into this object tree.
     *  All elements inserted in the current tree are a copy of the argument's children and members. */
    public void addAll(ObjectTree tree) {
        for (Map.Entry<String, ObjectTree> entry : tree.childrenMap.entrySet())
            if (childrenMap.containsKey(entry.getKey()))
                childrenMap.get(entry.getKey()).addAll(entry.getValue());
            else
                childrenMap.put(entry.getKey(), entry.getValue().clone(this));
    }

    /**
     * Copies a subtree from source into another subtree in target. The tree may be
     * pasted multiple times, if there are polymorphic nodes that are not explicitly marked
     * in the prefix arguments.
     * @param source       The source of the nodes.
     * @param target       The tree where nodes will be added
     * @param sourcePrefix The prefix to be consumed before copying nodes. Without root.
     * @param targetPrefix The prefix to be consumed before copying nodes. Without root.
     */
    public static void copyTargetTreeToSource(ObjectTree source, ObjectTree target, String sourcePrefix, String targetPrefix) {
        Collection<ObjectTree> a = source.findObjectTreeOfPolyMember(sourcePrefix);
        Collection<ObjectTree> b = target.findObjectTreeOfPolyMember(targetPrefix);
        for (ObjectTree sourceTree : a)
            for (ObjectTree targetTree : b)
                sourceTree.addAll(targetTree);
    }

    /** Obtains the set of nodes in this object tree that have no children. */
    public Collection<MemberNode> leaves() {
        return streamLeaves().collect(Collectors.toSet());
    }

    /** @see #leaves() */
    protected Stream<MemberNode> streamLeaves() {
        if (childrenMap.isEmpty())
            return Stream.of(memberNode);
        return childrenMap.values().stream()
                .flatMap(ObjectTree::streamLeaves);
    }

    /** Similar to {@link #getNodesForPoly(String)}, but returns object trees
     *  instead of member nodes. */
    Collection<ObjectTree> findObjectTreeOfPolyMember(String member) {
        Collection<ObjectTree> result = List.of(this);
        while (!member.isEmpty()) {
            int firstDot = member.indexOf('.');
            String first, rest;
            if (firstDot != -1) {
                first = member.substring(0, firstDot);
                rest = member.substring(firstDot + 1);
            } else {
                first = member;
                rest = "";
            }
            result = result.stream().flatMap(res -> {
                ObjectTree ot = res.childrenMap.get(first);
                if (ot == null && res.childrenMap.size() > 0) {
                    Collection<ObjectTree> collection = new LinkedList<>();
                    for (ObjectTree child : childrenMap.values()) {
                        if (!(child.getMemberNode() instanceof PolyMemberNode) || !child.childrenMap.containsKey(first))
                            throw new IllegalArgumentException("Could not locate member in object tree");
                        collection.add(child.childrenMap.get(first));
                    }
                    return collection.stream();
                } else if (ot == null) {
                    throw new IllegalArgumentException("Could not locate member in object tree");
                } else {
                    return Stream.of(ot);
                }
            }).collect(Collectors.toList());
            member = rest;
        }
        return result;
    }

    /** Whether this object tree contains the given member. The argument should contain the root variable name. */
    public boolean hasMember(String member) {
        String field = removeRoot(member);
        return hasNonRootMember(field, false);
    }

    /** Whether this object tree contains the given member. The argument may omit typing
     *  information (i.e., 'a.x' will find 'a.A.x', where A is a polymorphic node). */
    public boolean hasPolyMember(String member) {
        String field = removeRoot(member);
        return hasNonRootMember(field, true);
    }

    /** Similar to hasMember, but valid at any level of the tree and the argument should not contain
     *  the root variable's name.
     *  @see #hasMember(String) */
    private boolean hasNonRootMember(String members, boolean polymorphic) {
        if (members.contains(".")) {
            int firstDot = members.indexOf('.');
            String first = members.substring(0, firstDot);
            String rest = members.substring(firstDot + 1);
            if (polymorphic && !childrenMap.containsKey(first) && !childrenMap.isEmpty())
                return childrenMap.values().stream()
                        .filter(ot -> ot.getMemberNode() instanceof PolyMemberNode)
                        .anyMatch(ot -> ot.hasNonRootMember(members, true));
            return childrenMap.containsKey(first) && childrenMap.get(first).hasNonRootMember(rest, polymorphic);
        } else {
            if (polymorphic && !childrenMap.containsKey(members) && !childrenMap.isEmpty())
                return childrenMap.values().stream()
                        .filter(ot -> ot.getMemberNode() instanceof PolyMemberNode)
                        .anyMatch(ot -> ot.hasNonRootMember(members, true));
            return childrenMap.containsKey(members);
        }
    }

    /** Obtain the member node that corresponds to the given field name (with root). */
    public MemberNode getNodeFor(String member) {
        String field = removeRoot(member);
        return getNodeForNonRoot(field);
    }

    /** Similar to getNodeFor, but valid at any level of the tree, and the argument must be the field only.
     *  @see #getNodeFor(String) */
    MemberNode getNodeForNonRoot(String members) {
        if (members.isEmpty()) {
            return memberNode;
        } else if (members.contains(".")) {
            int firstDot = members.indexOf('.');
            String first = members.substring(0, firstDot);
            String rest = members.substring(firstDot + 1);
            assert childrenMap.containsKey(first);
            return childrenMap.get(first).getNodeForNonRoot(rest);
        } else {
            assert childrenMap.containsKey(members);
            return childrenMap.get(members).memberNode;
        }
    }

    /** Similar to {@link #getNodeFor(String)}, but if the argument does not contain
     *  types, it will obtain all member nodes that represent a given field (in multiple
     *  types). For example, the argument 'a.x' may produce 'a.A.x' and 'a.B.x'; whereas
     *  the argument 'a.A.x' will only produce one node. */
    public Collection<MemberNode> getNodesForPoly(String memberWithRoot) {
        return findObjectTreeOfPolyMember(removeRoot(memberWithRoot)).stream()
                .map(ObjectTree::getMemberNode)
                .collect(Collectors.toList());
    }

    /** @return An iterable through the names (with full prefixes) of all members of this tree,
     *  excluding the root. */
    public Iterable<String> nameIterable() {
        return () -> new Iterator<>() {
            final Iterator<ObjectTree> it = treeIterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public String next() {
                ObjectTree element = it.next();
                StringBuilder builder = new StringBuilder();
                MemberNode node = element.memberNode;
                if (node == null)
                    return ROOT_NAME;
                else if (node instanceof PolyMemberNode)
                    return next();
                else
                    builder.append(node.getLabel());
                while (node.getParent() instanceof MemberNode) {
                    node = (MemberNode) node.getParent();
                    builder.insert(0, '.');
                    builder.insert(0, node.getLabel());
                }
                return builder.toString();
            }
        };
    }

    /** @return An iterable through the nodes of all members of this tree, excluding the root. */
    public Iterable<MemberNode> nodeIterable() {
        return () -> new Iterator<>() {
            final Iterator<ObjectTree> it = treeIterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public MemberNode next() {
                return it.next().memberNode;
            }
        };
    }

    /** @return An iterator through all the trees of this structure, excluding the root. */
    private Iterator<ObjectTree> treeIterator() {
        return new Iterator<>() {
            final Set<ObjectTree> remaining = new HashSet<>(childrenMap.values());
            Iterator<ObjectTree> childIterator = null;

            @Override
            public boolean hasNext() {
                if (childIterator == null || !childIterator.hasNext())
                    return !remaining.isEmpty();
                else
                    return true;
            }

            @Override
            public ObjectTree next() {
                if (childIterator == null || !childIterator.hasNext()) {
                    ObjectTree tree = Utils.setPop(remaining);
                    childIterator = tree.treeIterator();
                    return tree;
                } else {
                    return childIterator.next();
                }
            }
        };
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public Object clone() {
        ObjectTree clone = new ObjectTree(memberNode.getLabel());
        for (Map.Entry<String, ObjectTree> entry : childrenMap.entrySet())
            clone.childrenMap.put(entry.getKey(), entry.getValue().clone(clone));
        return clone;
    }

    private ObjectTree clone(ObjectTree parent) {
        ObjectTree clone = new ObjectTree(getMemberNode().copyToParent(parent.getMemberNode()));
        for (Map.Entry<String, ObjectTree> entry : childrenMap.entrySet())
            clone.childrenMap.put(entry.getKey(), entry.getValue().clone(clone));
        return clone;
    }

    /**
     * Utility method to remove the root variable from a string. The root element or root of
     * the object tree should be either "-root-", a valid variable name or an optionally type-prefixed
     * this (A.this, package.A.this or this).
     * @throws IllegalArgumentException When there is no root to remove.
     */
    public static String removeRoot(String fieldWithRoot) {
        Matcher matcher = FIELD_SPLIT.matcher(fieldWithRoot);
        if (matcher.matches())
            return matcher.group("fields") != null ? matcher.group("fields") : "";
        throw new IllegalArgumentException("Field should be of the form <obj>.<field>, <Type>.this.<field>, where <obj> may not contain dots.");
    }

    /**
     * Utility method to remove the fields a string, retaining just the root. The root element or root of
     * the object tree should be either "-root-", a valid variable name or an optionally type-prefixed
     * this (A.this, package.A.this or this).
     * @throws IllegalArgumentException When there are no fields to remove.
     */
    public static String removeFields(String fieldWithRoot) {
        Matcher matcher = FIELD_SPLIT.matcher(fieldWithRoot);
        if (matcher.matches() && matcher.group("root") != null)
            return matcher.group("root");
        throw new IllegalArgumentException("Field should be of the form <obj>.<field>, <Type>.this.<field>, where <obj> may not contain dots.");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObjectTree tree = (ObjectTree) o;
        return Objects.equals(getMemberName(), tree.getMemberName()) &&
                childrenMap.values().equals(tree.childrenMap.values());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMemberName(), childrenMap);
    }
}
