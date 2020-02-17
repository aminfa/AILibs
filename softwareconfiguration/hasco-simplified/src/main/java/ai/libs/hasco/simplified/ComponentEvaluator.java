package ai.libs.hasco.simplified;

import ai.libs.hasco.model.ComponentInstance;

import java.util.Optional;

public interface ComponentEvaluator {

    /**
     * Simple-HASCO calls this method exactly once before it uses the eval on the sample.
     * The returned object is later on fed to the eval function.
     * TODO
     */
    default Object prepareEvaluator(ComponentInstance sample) {
        return null;
    }

    /**
     * Simple-HASCO calls this method exactly once right after it has called the eval method.
     *
     * @param sample
     */
    default void cleanupEvaluator(ComponentInstance sample) {

    }

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
