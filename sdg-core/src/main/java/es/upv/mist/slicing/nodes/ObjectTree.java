package es.upv.mist.slicing.nodes;

import es.upv.mist.slicing.nodes.oo.MemberNode;
import es.upv.mist.slicing.utils.Utils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ObjectTree implements Cloneable {
    public static final String ROOT_NAME = "-root-";

    private static final Pattern FIELD_SPLIT = Pattern.compile("^(?<root>(([_0-9A-Za-z]+\\.)*this)|([_0-9A-Za-z]+)|(-root-))(\\.(?<fields>.+))?$");

    private final Map<String, ObjectTree> childrenMap = new HashMap<>();

    private MemberNode memberNode;

    public ObjectTree() {
        this(ROOT_NAME);
    }

    public ObjectTree(String memberName) {
        memberNode = new MemberNode(memberName, null);
    }

    private ObjectTree(String memberName, ObjectTree parent) {
        this.memberNode = new MemberNode(memberName, parent.memberNode);
    }

    protected String getMemberName() {
        return memberNode == null ? ROOT_NAME : memberNode.getLabel();
    }

    public MemberNode getMemberNode() {
        return memberNode;
    }

    public void setMemberNode(MemberNode memberNode) {
        GraphNode<?> oldParent = null;
        if (this.memberNode != null)
            oldParent = this.memberNode.getParent();
        this.memberNode = memberNode;
        if (oldParent != null)
            this.memberNode.setParent(oldParent);
        childrenMap.values().forEach(ot -> ot.memberNode.setParent(memberNode));
    }

    public boolean hasChildren() {
        return !childrenMap.isEmpty();
    }

    public void addField(String fieldName) {
        String members = removeRoot(fieldName);
        addNonRootField(members);
    }

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

    public void addAll(ObjectTree tree) {
        for (Map.Entry<String, ObjectTree> entry : tree.childrenMap.entrySet())
            if (childrenMap.containsKey(entry.getKey()))
                childrenMap.get(entry.getKey()).addAll(entry.getValue());
            else
                childrenMap.put(entry.getKey(), entry.getValue().clone(this));
    }

    public boolean isLeaf(String memberWithoutRoot) {
        return findObjectTreeOfMember(memberWithoutRoot).childrenMap.isEmpty();
    }

    /**
     * Copies a subtree from source into another subtree in target.
     *
     * @param source       The source of the nodes.
     * @param target       The tree where nodes will be added
     * @param sourcePrefix The prefix to be consumed before copying nodes. Without root.
     * @param targetPrefix The prefix to be consumed before copying nodes. Without root.
     */
    public static void copyTree(ObjectTree source, ObjectTree target, String sourcePrefix, String targetPrefix) {
        ObjectTree a = source.findObjectTreeOfMember(sourcePrefix);
        ObjectTree b = target.findObjectTreeOfMember(targetPrefix);
        a.addAll(b);
    }

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

    public boolean hasMember(String member) {
        String field = removeRoot(member);
        return hasNonRootMember(field);
    }

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

    public MemberNode getNodeFor(String member) {
        String field = removeRoot(member);
        return getNodeForNonRoot(field);
    }

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

    public boolean isEmpty() {
        return childrenMap.isEmpty();
    }

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
                while (node.getParent() != null && node.getParent() instanceof MemberNode && node.getParent().getLabel().matches("^(USE|DEF|DEC)\\{")) {
                    node = (MemberNode) node.getParent();
                    builder.insert(0, '.');
                    builder.insert(0, node.getLabel());
                }
                return builder.insert(0, "-root-.").toString();
            }
        };
    }

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

    public static String removeRoot(String fieldWithRoot) {
        Matcher matcher = FIELD_SPLIT.matcher(fieldWithRoot);
        if (matcher.matches())
            return matcher.group("fields") != null ? matcher.group("fields") : "";
        throw new IllegalArgumentException("Field should be of the form <obj>.<field>, <Type>.this.<field>, where <obj> may not contain dots.");
    }

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
