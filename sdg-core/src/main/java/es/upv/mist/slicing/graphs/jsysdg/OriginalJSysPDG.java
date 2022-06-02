package es.upv.mist.slicing.graphs.jsysdg;

import es.upv.mist.slicing.graphs.pdg.PDG;
import es.upv.mist.slicing.nodes.GraphNode;
import es.upv.mist.slicing.nodes.VariableAction;

public class OriginalJSysPDG extends JSysPDG {
    public OriginalJSysPDG() {
        this(new JSysCFG());
    }

    public OriginalJSysPDG(JSysCFG cfg) {
        super(cfg);
    }

    @Override
    protected PDG.Builder createBuilder() {
        return new Builder();
    }

    // definicion de raiz --object-flow--> uso de raiz
    @Override
    protected void addObjectFlowDependencyArc(VariableAction definition, VariableAction usage) {
        throw new UnsupportedOperationException();
    }

    // definicion de miembro --object-flow--> definicion de raiz
    @Override
    protected void addObjectFlowDependencyArc(VariableAction nextDefinitionRoot, String[] memberDefined, VariableAction definition) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void addTotalDefinitionDependencyArc(VariableAction totalDefinition, VariableAction target, String[] member) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addDataDependencyArc(VariableAction src, VariableAction tgt) {
        throw new UnsupportedOperationException("Use flow or object-flow dependency");
    }

    protected class Builder extends JSysPDG.Builder {

        @Override
        protected void buildDataDependency() {
            addSyntheticNodesToPDG();
            applyTreeConnections();
            buildJSysDataDependency();
            valueDependencyForThrowStatements();
        }

        /** Compute flow, object flow and total definition dependence. */
        @Override
        protected void buildJSysDataDependency() {
            JSysCFG jSysCFG = (JSysCFG) cfg;
            for (GraphNode<?> node : vertexSet()) {
                for (VariableAction varAct : node.getVariableActions()) {
                    // Total definition dependence
                    if (varAct.isUsage())
                        buildUsageDependencies(jSysCFG, varAct);
                    else if (varAct.isDefinition())
                        buildDefinitionDependencies(jSysCFG, varAct);
                    else if (varAct.isDeclaration())
                        buildDeclarationDependencies(jSysCFG, varAct);
                }
            }
        }

        /** Generate dependencies to usages, including flow dependency for primitives,
         *  object flow for object roots and flow for object members. */
        private void buildUsageDependencies(JSysCFG jSysCFG, VariableAction varAct) {
            if (varAct.isPrimitive()) {
                jSysCFG.findLastDefinitionOfPrimitive(varAct).forEach(def -> addFlowDependencyArc(def, varAct));
            } else if (varAct.hasObjectTree()) {
                for (String[] member : varAct.getObjectTree().nameAsArrayIterable())
                    jSysCFG.findLastDefinitionOfObjectMember(varAct, member).forEach(def -> addFlowDependencyArc(def, varAct, member));
            }
        }

        /** Generates dec --> def flow and def --> def object flow dependencies. */
        private void buildDefinitionDependencies(JSysCFG jSysCFG, VariableAction varAct) {
            // Flow declaration --> definition
            if (!varAct.isSynthetic())
                jSysCFG.findDeclarationFor(varAct).ifPresent(dec -> addFlowDependencyArc(dec, varAct));
        }

        /** Generates dec --> def declaration dependencies for objects (constructors only). */
        private void buildDeclarationDependencies(JSysCFG jSysCFG, VariableAction varAct) {
            if (!varAct.getName().startsWith("this."))
                return;
            jSysCFG.findAllFutureObjectDefinitionsFor(varAct).forEach(def -> addDeclarationFlowDependencyArc(varAct, def));
        }
    }
}
