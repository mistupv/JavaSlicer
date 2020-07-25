package tfm.arcs.sdg;

public class ReturnArc extends InterproceduralArc {
    @Override
    public boolean isInterproceduralInputArc() {
        return false;
    }

    @Override
    public boolean isInterproceduralOutputArc() {
        return true;
    }
}
