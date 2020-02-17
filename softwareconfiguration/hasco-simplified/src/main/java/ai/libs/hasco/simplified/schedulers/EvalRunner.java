package ai.libs.hasco.simplified.schedulers;

import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.simplified.ComponentEvaluator;

import java.util.Optional;
import java.util.concurrent.Callable;

class EvalRunner implements Callable<Optional<Double>> {

    private ComponentEvaluator evaluator;

    private ComponentInstance sample;

    private SampleBatch batch;

    private int index;

    EvalRunner(ComponentEvaluator evaluator, SampleBatch batch, int index) {
        this.evaluator = evaluator;
        this.sample = batch.getSample(index);
        this.batch = batch;
        this.index = index;
    }

    public Optional<Double> call() throws Exception {
//        evaluator.prepareEvaluator(sample);
        batch.startSampleTimer(index);
        Optional<Double> result = evaluator.eval(sample);
//        evaluator.cleanupEvaluator(sample);
        return result;
    }
}
