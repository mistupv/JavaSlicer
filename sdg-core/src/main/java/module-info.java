module sdg.core {
  requires com.github.javaparser.core;
  requires com.github.javaparser.symbolsolver.core;
  requires org.jgrapht.core;

  exports es.upv.mist.slicing.slicing;
  exports es.upv.mist.slicing.graphs;
  exports es.upv.mist.slicing.graphs.sdg;
  exports es.upv.mist.slicing.graphs.augmented;
  exports es.upv.mist.slicing.graphs.pdg;
  exports es.upv.mist.slicing.graphs.exceptionsensitive;
}
