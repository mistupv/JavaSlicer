package tfm.nodes;

public class IdHelper {

    private static final int START_ID = 0;

    private static final IdHelper INSTANCE = new IdHelper();

    private long nextId;

    private IdHelper() {
        nextId = START_ID;
    }

    static IdHelper getInstance() {
        return INSTANCE;
    }

    synchronized long getNextId() {
        return nextId++;
    }

    /** DO NOT USE!!! */
    public static synchronized void reset() {
        getInstance().nextId = START_ID;
    }
}
