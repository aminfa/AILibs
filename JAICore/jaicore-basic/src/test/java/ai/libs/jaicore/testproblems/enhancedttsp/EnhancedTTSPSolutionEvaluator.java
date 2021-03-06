package ai.libs.jaicore.testproblems.enhancedttsp;

import ai.libs.jaicore.basic.IObjectEvaluator;
import it.unimi.dsi.fastutil.shorts.ShortList;

public class EnhancedTTSPSolutionEvaluator implements IObjectEvaluator<ShortList, Double> {

	private final EnhancedTTSP problem;

	public EnhancedTTSPSolutionEvaluator(final EnhancedTTSP problem) {
		super();
		this.problem = problem;
	}

	@Override
	public Double evaluate(final ShortList solutionTour) {
		EnhancedTTSPNode state = this.problem.getInitalState();
		for (short next : solutionTour) {
			state = this.problem.computeSuccessorState(state, next);
		}
		return state.getTime() - this.problem.getHourOfDeparture();
	}

}
