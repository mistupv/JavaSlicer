package tfm.nodes;

/** A singleton class that provides unique ids for {@link GraphNode}s. */
public class IdHelper {
    private static final int START_ID = 0;
    private static final IdHelper INSTANCE = new IdHelper();

    private long nextId;

    private IdHelper() {
        nextId = START_ID;
    }

    synchronized long getNextId() {
        return nextId++;
    }

    static IdHelper getInstance() {
        return INSTANCE;
    }
}
