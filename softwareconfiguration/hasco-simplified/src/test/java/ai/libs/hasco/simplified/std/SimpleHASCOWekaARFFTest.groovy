package ai.libs.hasco.simplified.std

import ai.libs.hasco.serialization.ComponentLoader
import ai.libs.hasco.simplified.ComponentEvaluator
import ai.libs.hasco.simplified.ComponentRegistry
import ai.libs.hasco.simplified.std.ml.SimpleHASCOWEKABuilder
import ai.libs.jaicore.ml.classification.loss.dataset.EClassificationPerformanceMeasure
import ai.libs.jaicore.ml.core.evaluation.evaluator.SupervisedLearnerExecutor
import ai.libs.jaicore.ml.weka.WekaUtil
import ai.libs.jaicore.ml.weka.dataset.IWekaInstances
import ai.libs.jaicore.ml.weka.dataset.WekaInstances
import ai.libs.mlplan.multiclass.wekamlplan.weka.WekaPipelineFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Ignore
import spock.lang.Specification
import weka.core.Instances

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class SimpleHASCOWekaARFFTest extends Specification {

    private final static Logger logger = LoggerFactory.getLogger("ai.libs.hasco.simplified.TESTS");

    static List<IWekaInstances> split
    static Instances data

    static def loadData() {
        long start = System.currentTimeMillis();
        def arffSource
        arffSource = "testrsc/datasets/heart.statlog.arff"
//        arffSource = "testrsc/datasets/flags.arff"
//        arffSource = "testrsc/datasets/waveform.arff"
        File file = new File(arffSource);
        data = new Instances(new FileReader(file));
        logger.info("Data read. Time to create dataset object was {}ms", System.currentTimeMillis() - start);
        data.setClassIndex(data.numAttributes() - 1);
        split = WekaUtil.getStratifiedSplit(new WekaInstances(data), 0, 0.7);
    }

    static ComponentLoader loader
    static ComponentRegistry registry

    static def loadProblem() {
        def problemFile = "testrsc/wekaproblems/autoweka.json"
        loader = new ComponentLoader(new File(problemFile))
        registry = ComponentRegistry.fromComponentLoader(loader)
    }

    static WekaPipelineFactory pipelineFactory
    static SupervisedLearnerExecutor learnerExecutor
    static ComponentEvaluator pipelineEvaluator


    static def createEvaluator() {
        pipelineFactory = new WekaPipelineFactory()
        learnerExecutor = new SupervisedLearnerExecutor();
        pipelineEvaluator = { componentInstance ->
            if(!ImplUtil.isValidComponentPrototype(componentInstance)) {
                logger.warn("Invalid component instance was evaluated: " + componentInstance);
            }
            def report
            try {
                def learner = pipelineFactory.getComponentInstantiation(componentInstance)
                report = learnerExecutor.execute(learner, split.get(0), split.get(1))
            } catch(Throwable error) {
                logger.warn("Error during evaluation of {}:", componentInstance, error)
                return Optional.empty()
            }
            if(report.getException() != null) {
                logger.warn("Error during evaluation of {}:", componentInstance, report.getException())
                return Optional.empty()
            }
            def predList = report.getPredictionDiffList()
            Double loss = Double.valueOf(EClassificationPerformanceMeasure.ERRORRATE.loss(predList.groundTruthAsList, predList.predictionsAsList))
            if(loss.naN || loss.infinite) {
                logger.warn("Evaluation of {} resulted in {}.", componentInstance, loss)
                return Optional.empty()
            }
            logger.info("Evaluation of {} resulted in {}.",componentInstance, loss)
            return Optional.of(loss);
        }
    }

    def setupSpec() {
        loadData()
        loadProblem()
        createEvaluator()
    }


    @Ignore
    def "test"() {
        SimpleHASCOStdBuilder stdHASCO = new SimpleHASCOStdBuilder()
        stdHASCO.registry = registry
        stdHASCO.seed = 1L
        stdHASCO.evaluator = pipelineEvaluator
        stdHASCO.requiredInterface = "AbstractClassifier"
        def bestSeen = stdHASCO.bestCandidateCache

        stdHASCO.init()
        def runner = stdHASCO.simpleHASCOInstance
        def stepCount = new AtomicInteger(1)
        def bestScoreYet = Optional.<Double>empty()
        while(runner.step()) {
            logger.info("Finished step {}.", stepCount.andIncrement)
            if(bestSeen.checkBestCandidateAndUpdate()) {
                bestScoreYet = bestSeen.bestSeenScore

                logger.info("New best score: {}, Component instance:\n{}",
                        bestScoreYet.get(), bestSeen.bestSeenCandidate.get())
            }
        }
        logger.info("Finished after {} many steps.", stepCount.get())
        if(bestScoreYet.isPresent())
            logger.info("The best score seen was {}.", bestScoreYet.get())
        else
            logger.warn("Not component instance could be evaluated successfully.")

        expect:
        bestScoreYet.isPresent()
    }

    def "test builder" () {
        def builder = new SimpleHASCOWEKABuilder()
        builder.componentLoader = loader
        builder.seed = 1L
        builder.setRefinementTime(30, TimeUnit.SECONDS)
        builder.setSampleTime(10, TimeUnit.SECONDS)
        builder.setSampleConsumer({ ci, result ->
            println("Received component instance ${ci} with result ${result}.")
        })
        builder.flagRandomNumericSampling = true
        builder.threadCount = 8
        builder.minEvalQueueSize = 4
        builder.samplesPerRefinement = 4

        builder.splits = split

        def runner = builder.simpleHASCOInstance
        runner.runUntil(10, TimeUnit.SECONDS)

        expect:
        builder.bestCandidateCache.bestSeenCandidate.isPresent()
    }


}
