package es.upv.mist.slicing.nodes;

import es.upv.mist.slicing.nodes.oo.MemberNode;
import es.upv.mist.slicing.utils.Utils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final Pattern FIELD_SPLIT = Pattern.compile("^(?<root>(([_0-9A-Za-z]+\\.)*this)|([_0-9A-Za-z]+)|(-root-))(\\.(?<fields>.+))?$");

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
        memberNode = new MemberNode(memberName, null);
    }

    /** Create a child tree node for the given field, whose node is linked to the given parent. */
    private ObjectTree(String memberName, ObjectTree parent) {
        this.memberNode = new MemberNode(memberName, parent.memberNode);
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

    /**
     * Insert a field with the given name. This method should only be called on a root object tree.
     * This method may be used to add multiple levels simultaneously, calling this method with
     * the argument {@code "a.b.c"} on a new root tree, it will create the tree "b" inside the root
     * and "c" inside "b".
     * @param fieldName The field to be added, should include the root variable name. For example,
     *                  to add the field "x" to a variable "a", this argument should be "a.x".
     */
    public void addField(String fieldName) {
        String members = removeRoot(fieldName);
        addNonRootField(members);
    }

    /** Similar to {@link #addField(String)}, but may be called at any level
     *  and the argument must not contain the root variable. */
    private void addNonRootField(String members) {
        if (members.contains(".")) {
            int firstDot = members.indexOf('.');
            String first = members.substring(0, firstDot);
            String rest = members.substring(firstDot + 1);
            childrenMap.computeIfAbsent(first, f -> new ObjectTree(f, this));
            childrenMap.get(first).addNonRootField(rest);
        } else {
            childrenMap.computeIfAbsent(members, f -> new ObjectTree(f, this));
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
     * Copies a subtree from source into another subtree in target.
     *
     * @param source       The source of the nodes.
     * @param target       The tree where nodes will be added
     * @param sourcePrefix The prefix to be consumed before copying nodes. Without root.
     * @param targetPrefix The prefix to be consumed before copying nodes. Without root.
     */
    public static void copyTargetTreeToSource(ObjectTree source, ObjectTree target, String sourcePrefix, String targetPrefix) {
        ObjectTree a = source.findObjectTreeOfMember(sourcePrefix);
        ObjectTree b = target.findObjectTreeOfMember(targetPrefix);
        a.addAll(b);
    }

    /**
     * Locate an object tree that represents a field of this object.
     * @param member The field, without a root prefix.
     */
    ObjectTree findObjectTreeOfMember(String member) {
        ObjectTree result = this;
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
            result = result.childrenMap.get(first);
            member = rest;
        }
        return result;
    }

    /** Whether this object tree contains the given member. The argument should contain the root variable name. */
    public boolean hasMember(String member) {
        String field = removeRoot(member);
        return hasNonRootMember(field);
    }

    /** Similar to hasMember, but valid at any level of the tree and the argument should not contain
     *  the root variable's name.
     *  @see #hasMember(String) */
    private boolean hasNonRootMember(String members) {
        if (members.contains(".")) {
            int firstDot = members.indexOf('.');
            String first = members.substring(0, firstDot);
            String rest = members.substring(firstDot + 1);
            return childrenMap.containsKey(first) && childrenMap.get(first).hasNonRootMember(rest);
        } else {
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
                else
                    builder.append(node.getLabel());
                while (node.getParent() instanceof MemberNode && node.getParent().getLabel().matches("^(USE|DEF|DEC)\\{")) {
                    node = (MemberNode) node.getParent();
                    builder.insert(0, '.');
                    builder.insert(0, node.getLabel());
                }
                return builder.insert(0, ROOT_NAME + ".").toString();
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

    /** @see #treeIterator() */
    Iterable<ObjectTree> treeIterable() {
        return this::treeIterator;
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
        ObjectTree clone = new ObjectTree(getMemberName(), parent);
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
