package ai.libs.hasco.simplified;

import ai.libs.hasco.model.ComponentInstance;

import java.util.Optional;

public interface ComponentEvaluator {

    /**
     * Evaluates the component instance.
     * If the evaluation fails or the sample is invalid returns an empty Optional.
     *
     * @param sample The sample to be evaluated.
     * @return The evaluation result
     * @throws InterruptedException The thread was interrupted while working on the evaluation.
     */
    Optional<Double> eval(ComponentInstance sample) throws InterruptedException;

}
