package hasco.variants;

import hasco.RefinementConfiguredSoftwareConfigurationProblem;
import hasco.core.HASCO;
import jaicore.basic.algorithm.AlgorithmProblemTransformer;
import jaicore.planning.algorithms.forwarddecomposition.ForwardDecompositionReducer;
import jaicore.planning.graphgenerators.task.tfd.TFDNode;
import jaicore.search.core.interfaces.IGraphSearchFactory;
import jaicore.search.model.probleminputs.GraphSearchProblemInput;

public class HASCOViaFD<ISearch, V extends Comparable<V>> extends HASCO<ISearch, TFDNode, String, V> {

	public HASCOViaFD(RefinementConfiguredSoftwareConfigurationProblem<V> configurationProblem,
			IGraphSearchFactory<ISearch, ?, TFDNode, String, V, ?, ?, ?> searchFactory, AlgorithmProblemTransformer<GraphSearchProblemInput<TFDNode, String, V>, ISearch> searchProblemTransformer) {
		super(configurationProblem, new ForwardDecompositionReducer<>(), searchFactory, searchProblemTransformer);
	}
	
}
