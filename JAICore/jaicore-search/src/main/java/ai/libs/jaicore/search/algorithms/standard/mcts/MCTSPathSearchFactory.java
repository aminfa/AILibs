package ai.libs.jaicore.search.algorithms.standard.mcts;

import ai.libs.jaicore.search.core.interfaces.IOptimalPathInORGraphSearchFactory;
import ai.libs.jaicore.search.core.interfaces.StandardORGraphSearchFactory;
import ai.libs.jaicore.search.model.other.EvaluatedSearchGraphPath;
import ai.libs.jaicore.search.probleminputs.GraphSearchWithPathEvaluationsInput;

public class MCTSPathSearchFactory<N, A, V extends Comparable<V>> extends StandardORGraphSearchFactory<GraphSearchWithPathEvaluationsInput<N, A, V>, EvaluatedSearchGraphPath<N, A, V>, N, A, V> implements IOptimalPathInORGraphSearchFactory<GraphSearchWithPathEvaluationsInput<N, A, V>, N, A, V> {
	private IPathUpdatablePolicy<N, A, V> treePolicy;
	private IPolicy<N, A, V> defaultPolicy;
	private V evaluationFailurePenalty;
	private boolean forbidDoublePaths;

	public IPathUpdatablePolicy<N, A, V> getTreePolicy() {
		return this.treePolicy;
	}

	public void setTreePolicy(final IPathUpdatablePolicy<N, A, V> treePolicy) {
		this.treePolicy = treePolicy;
	}

	public IPolicy<N, A, V> getDefaultPolicy() {
		return this.defaultPolicy;
	}

	public void setDefaultPolicy(final IPolicy<N, A, V> defaultPolicy) {
		this.defaultPolicy = defaultPolicy;
	}

	public V getEvaluationFailurePenalty() {
		return this.evaluationFailurePenalty;
	}

	public void setEvaluationFailurePenalty(final V evaluationFailurePenalty) {
		this.evaluationFailurePenalty = evaluationFailurePenalty;
	}

	public boolean isForbidDoublePaths() {
		return this.forbidDoublePaths;
	}

	public void setForbidDoublePaths(final boolean forbidDoublePaths) {
		this.forbidDoublePaths = forbidDoublePaths;
	}

	@Override
	public MCTSPathSearch<N, A, V> getAlgorithm() {
		return this.getAlgorithm(this.getInput());
	}

	@Override
	public MCTSPathSearch<N, A, V> getAlgorithm(final GraphSearchWithPathEvaluationsInput<N, A, V> problem) {
		return new MCTSPathSearch<>(problem, this.treePolicy, this.defaultPolicy, this.evaluationFailurePenalty, this.forbidDoublePaths);
	}
}
