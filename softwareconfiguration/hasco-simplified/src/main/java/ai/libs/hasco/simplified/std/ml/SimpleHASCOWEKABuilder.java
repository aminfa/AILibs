package ai.libs.hasco.simplified.std.ml;

import ai.libs.hasco.simplified.ComponentEvaluator;
import ai.libs.hasco.simplified.std.ImplUtil;
import ai.libs.hasco.simplified.std.SimpleHASCOStdBuilder;
import ai.libs.jaicore.ml.classification.loss.dataset.EClassificationPerformanceMeasure;
import ai.libs.jaicore.ml.core.evaluation.evaluator.SupervisedLearnerExecutor;
import ai.libs.jaicore.ml.weka.classification.learner.IWekaClassifier;
import ai.libs.jaicore.ml.weka.dataset.IWekaInstances;
import ai.libs.mlplan.multiclass.wekamlplan.weka.WekaPipelineFactory;
import org.api4.java.ai.ml.core.evaluation.IPredictionAndGroundTruthTable;
import org.api4.java.ai.ml.core.evaluation.execution.ILearnerRunReport;
import org.api4.java.ai.ml.core.evaluation.supervised.loss.IDeterministicHomogeneousPredictionPerformanceMeasure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class SimpleHASCOWEKABuilder extends SimpleHASCOStdBuilder {

    private final static Logger logger = LoggerFactory.getLogger(SimpleHASCOWEKABuilder.class);

    private WekaPipelineFactory pipelineFactory = new WekaPipelineFactory();

    private SupervisedLearnerExecutor learnerExecutor = new SupervisedLearnerExecutor();

    private IDeterministicHomogeneousPredictionPerformanceMeasure measure = EClassificationPerformanceMeasure.ERRORRATE;

    private List<IWekaInstances> splits;


    public void setPipelineFactory(WekaPipelineFactory pipelineFactory) {
        this.pipelineFactory = pipelineFactory;
    }

    public void setLearnerExecutor(SupervisedLearnerExecutor learnerExecutor) {
        this.learnerExecutor = learnerExecutor;
    }

    public void setMeasure(IDeterministicHomogeneousPredictionPerformanceMeasure measure) {
        this.measure = measure;
    }

    public void setSplits(List<IWekaInstances> splits) {
        this.splits = splits;
    }

    @Override
    public void init() {
        if(evaluator == null)
            initializeEvaluator();
        if(requiredInterface == null)
            setRequiredInterface("AbstractClassifier");
        super.init();
    }

    private void initializeEvaluator() {
        ComponentEvaluator pipelineEvaluator = componentInstance -> {
            if(!ImplUtil.isValidComponentPrototype(componentInstance)) {
                logger.warn("Invalid component instance was evaluated: " + componentInstance);
                return Optional.empty();
            }
            ILearnerRunReport report;
            try {
                IWekaClassifier instance = pipelineFactory.getComponentInstantiation(componentInstance);
                report = learnerExecutor.execute(instance, splits.get(0), splits.get(1));
            } catch(Throwable error) {
                logger.warn("Error during evaluation of {}:", componentInstance, error);
                return Optional.empty();
            }
            if(report.getException() != null) {
                logger.warn("Error during evaluation of {}:", componentInstance, report.getException());
                return Optional.empty();
            }
            IPredictionAndGroundTruthTable predList = report.getPredictionDiffList();
            Double loss = measure.loss(predList.getGroundTruthAsList(), predList.getPredictionsAsList());
            if(loss.isNaN() || loss.isInfinite()) {
                logger.warn("Evaluation of {} resulted in {}.", componentInstance, loss);
                return Optional.empty();
            }
            logger.info("Evaluation of {} resulted in {}.",componentInstance, loss);
            return Optional.of(loss);
        };
        setEvaluator(pipelineEvaluator);
    }
}
