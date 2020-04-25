package ai.libs.hasco.simplified;

import ai.libs.hasco.model.ComponentInstance;
import org.api4.java.common.attributedobjects.IObjectEvaluator;
import org.api4.java.common.attributedobjects.ObjectEvaluationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public interface ComponentEvaluator {

    Logger logger = LoggerFactory.getLogger(ComponentEvaluator.class);

    /**
     * Evaluates the component instance.
     * If the evaluation fails or the sample is invalid returns an empty Optional.
     *
     * @param sample The sample to be evaluated.
     * @return The evaluation result
     * @throws InterruptedException The thread was interrupted while working on the evaluation.
     */
    Optional<Double> eval(ComponentInstance sample) throws InterruptedException;

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
