package es.upv.mist.slicing.graphs.exceptionsensitive;

import es.upv.mist.slicing.graphs.jsysdg.JSysDG;
import es.upv.mist.slicing.slicing.AllenSlicingAlgorithm;
import es.upv.mist.slicing.slicing.SlicingAlgorithm;

public class AllenSDG extends JSysDG {
    @Override
    protected SlicingAlgorithm createSlicingAlgorithm() {
        return new AllenSlicingAlgorithm(this);
    }
}
