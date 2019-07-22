package tfm.utils;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.Statement;
import edg.graphlib.Arrow;
import tfm.arcs.cfg.ControlFlowArc;
import tfm.graphs.CFGGraph;
import tfm.nodes.CFGNode;

import java.util.*;
import java.util.function.Predicate;

public class Utils {

    public static final String PROGRAMS_FOLDER = "src/main/java/tfm/programs/";

    public static BlockStmt blockWrapper(Statement statement) {
        if (statement.isBlockStmt())
            return statement.asBlockStmt();

        return new BlockStmt(new NodeList<>(statement));
    }

    public static boolean isLoop(Statement statement) {
        return statement.isWhileStmt()
                || statement.isDoStmt()
                || statement.isForStmt()
                || statement.isForEachStmt();
    }

    public static Statement findFirstAncestorStatementFrom(Statement statement, Predicate<Statement> predicate) {
        if (predicate.test(statement)) {
            return statement;
        }

        if (!statement.getParentNode().isPresent()) {
            return new EmptyStmt();
        }

        return findFirstAncestorStatementFrom((Statement) statement.getParentNode().get(), predicate);
    }

    public static <E> List<E> emptyList() {
        return new ArrayList<>(0);
    }

    public static <E> Set<E> emptySet() {
        return new HashSet<>(0);
    }

    public static Set<CFGNode> findLastDefinitionsFrom(CFGNode startNode, String variable) {
//        Logger.log("=======================================================");
//        Logger.log("Starting from " + startNode);
//        Logger.log("Looking for variable " + variable);
//        Logger.log(cfgGraph.toString());
        return findLastDefinitionsFrom(new HashSet<>(), startNode, startNode, variable);
    }

    private static Set<CFGNode> findLastDefinitionsFrom(Set<Integer> visited, CFGNode startNode, CFGNode currentNode, String variable) {
        visited.add(currentNode.getId());

//        Logger.log("On " + currentNode);

        Set<CFGNode> res = new HashSet<>();

        for (Arrow arrow : currentNode.getIncomingArrows()) {
            ControlFlowArc controlFlowArc = (ControlFlowArc) arrow;

            CFGNode from = (CFGNode) controlFlowArc.getFromNode();

//            Logger.log("Arrow from node: " + from);

            if (!Objects.equals(startNode, from) && visited.contains(from.getId())) {
//                Logger.log("It's already visited. Continuing...");
                continue;
            }

            if (from.getDefinedVariables().contains(variable)) {
//                Logger.log("Contains defined variable: " + variable);
                res.add(from);
            } else {
//                Logger.log("Doesn't contain the variable, searching inside it");
                res.addAll(findLastDefinitionsFrom(visited, startNode, from, variable));
            }
        }

//        Logger.format("Done with node %s", currentNode.getId());

        return res;
    }
}
