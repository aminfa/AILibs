package ai.libs.jaicore.search.algorithms.standard.lds;

import ai.libs.jaicore.search.core.interfaces.StandardORGraphSearchFactory;
import ai.libs.jaicore.search.model.other.EvaluatedSearchGraphPath;
import ai.libs.jaicore.search.probleminputs.GraphSearchWithNodeRecommenderInput;

public class BestFirstLimitedDiscrepancySearchFactory<N, A, V extends Comparable<V>>
extends StandardORGraphSearchFactory<GraphSearchWithNodeRecommenderInput<N, A>, EvaluatedSearchGraphPath<N, A, V>, N, A, V> {

	@Override
	public BestFirstLimitedDiscrepancySearch<N, A, V> getAlgorithm() {
		if (this.getInput() == null) {
			throw new IllegalArgumentException("Cannot create algorithm; problem input has not been set yet");
		}
		return this.getAlgorithm(this.getInput());
	}

	@Override
	public BestFirstLimitedDiscrepancySearch<N, A, V> getAlgorithm(final GraphSearchWithNodeRecommenderInput<N, A> input) {
		return new BestFirstLimitedDiscrepancySearch<>(input);
	}
}
