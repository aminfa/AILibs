package ai.libs.hasco.simplified;

import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.simplified.std.ImplUtil;
import ai.libs.jaicore.ml.classification.loss.dataset.EClassificationPerformanceMeasure;
import ai.libs.jaicore.ml.core.evaluation.evaluator.SupervisedLearnerExecutor;
import ai.libs.jaicore.ml.weka.classification.learner.IWekaClassifier;
import ai.libs.jaicore.ml.weka.dataset.IWekaInstances;
import ai.libs.mlplan.multiclass.wekamlplan.weka.WekaPipelineFactory;
import org.api4.java.ai.ml.core.evaluation.IPredictionAndGroundTruthTable;
import org.api4.java.ai.ml.core.evaluation.execution.ILearnerRunReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class WekaClassifierComponentEvaluator implements ComponentEvaluator {

    private final static Logger logger = LoggerFactory.getLogger(WekaClassifierComponentEvaluator.class);

    private final List<IWekaInstances> split;

    private final WekaPipelineFactory pipelineFactory = new WekaPipelineFactory();

    private final SupervisedLearnerExecutor learnerExecutor = new SupervisedLearnerExecutor();

    public WekaClassifierComponentEvaluator(List<IWekaInstances> split) {
        this.split = split;
    }

    @Override
    public Optional<Double> eval(ComponentInstance root) throws InterruptedException {
        if(!ImplUtil.isValidComponentPrototype(root)) {
            logger.warn("Invalid component instance was evaluated: " + root);
        }
        ILearnerRunReport report;
        Optional<ComponentInstance> classifierComponent = ImplUtil.findFirstProviderOfInterface(root,
                "AbstractClassifier");
        if(!classifierComponent.isPresent()) {
            logger.warn("No component instance fulfills the AbstractClassifier interface.");
            return Optional.empty();
        }
        ComponentInstance componentInstance = classifierComponent.get();
        try {
            IWekaClassifier learner = pipelineFactory.getComponentInstantiation(componentInstance);
            report = learnerExecutor.execute(learner, split.get(0), split.get(1));
        } catch(Throwable error) {
            logger.warn("Error during evaluation of {}:", componentInstance, error);
            return Optional.empty();
        }
        if(report.getException() != null) {
            logger.warn("Error during evaluation of {}:", componentInstance, report.getException());
            return Optional.empty();
        }
        IPredictionAndGroundTruthTable<?, ?> predList = report.getPredictionDiffList();
        Double loss = EClassificationPerformanceMeasure.ERRORRATE.loss(predList.getGroundTruthAsList(), predList.getPredictionsAsList());
        if(loss.isNaN() || loss.isInfinite()) {
            logger.warn("Evaluation of {} resulted in {}.", componentInstance, loss);
            return Optional.empty();
        }
        logger.info("Evaluation of {} resulted in {}.",componentInstance, loss);
        return Optional.of(loss);
    }
}
