module sdg.core {
  requires com.github.javaparser.core;
  requires com.github.javaparser.symbolsolver;
  requires org.jgrapht.core;
  requires java.logging;

  exports es.upv.mist.slicing.slicing;
  exports es.upv.mist.slicing.graphs;
  exports es.upv.mist.slicing.graphs.sdg;
  exports es.upv.mist.slicing.graphs.augmented;
  exports es.upv.mist.slicing.graphs.pdg;
  exports es.upv.mist.slicing.graphs.exceptionsensitive;
}
