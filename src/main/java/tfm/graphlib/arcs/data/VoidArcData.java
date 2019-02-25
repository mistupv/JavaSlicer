package tfm.graphlib.arcs.data;

public class VoidArcData extends ArcData {
    @Override
    public boolean isVoid() {
        return true;
    }

    @Override
    public boolean isVariable() {
        return false;
    }

    @Override
    public String toString() {
        return "void";
    }
}
