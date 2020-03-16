package ai.libs.hasco.simplified;

import ai.libs.hasco.model.ComponentInstance;
import org.api4.java.common.attributedobjects.IObjectEvaluator;
import org.api4.java.common.attributedobjects.ObjectEvaluationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public interface ComponentEvaluator {

    Logger logger = LoggerFactory.getLogger(ComponentEvaluator.class);

    static ComponentEvaluator fromIObjectEvaluator(IObjectEvaluator<ComponentInstance, Double> compositionEvaluator) {
        return sample -> {
            try {
                return Optional.ofNullable(compositionEvaluator.evaluate(sample));
            } catch(ObjectEvaluationFailedException ex) {
                logger.error("Object eval error: ", ex);
                return Optional.empty();
            }
        };
    }

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

    default IObjectEvaluator<ComponentInstance, Double> toIObjectEvaluator() {
        return new IObjectEvaluator<ComponentInstance, Double>() {
            @Override
            public Double evaluate(ComponentInstance object) throws InterruptedException, ObjectEvaluationFailedException {
                Optional<Double> evaluation = eval(object);
                return evaluation.orElseThrow(() -> new ObjectEvaluationFailedException("Evaluation of the following component instance failed:\n" + object ));
            }
        };
    }
}
