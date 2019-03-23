package tfm.arcs.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class VariableArcData extends ArcData {
    private List<String> variables;

    public VariableArcData(String... variables) {
        this(Arrays.asList(variables));
    }

    public VariableArcData(Collection<? extends String> variables) {
        this.variables = new ArrayList<>(variables);
    }

    public List<String> getVariables() {
        return variables;
    }

    @Override
    public boolean isVoid() {
        return false;
    }

    @Override
    public boolean isVariable() {
        return true;
    }

    @Override
    public String toString() {
        return variables.toString();
    }
}
