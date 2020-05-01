package ai.libs.hasco.simplified.std;

import ai.libs.hasco.core.HASCOSolutionCandidate;
import ai.libs.hasco.core.RefinementConfiguredSoftwareConfigurationProblem;
import ai.libs.hasco.core.SoftwareConfigurationProblem;
import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.serialization.ComponentLoader;
import ai.libs.hasco.simplified.*;
import ai.libs.hasco.simplified.schedulers.ParallelRefParallelSampleScheduler;
import ai.libs.jaicore.basic.sets.PartialOrderedSet;
import com.google.common.eventbus.EventBus;
import org.aeonbits.owner.ConfigCache;
import org.api4.java.algorithm.IAlgorithmFactory;
import org.api4.java.common.attributedobjects.IObjectEvaluator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static ai.libs.hasco.simplified.SimpleHASCOConfig.*;

/**
 * This is the standard hasco implementation of SimpleHASCO that mimics the default reduction-based implementation. <br>
 * Each std hasco instance can be used at most for one search. <br>
 * In order to use this class do the following steps: <br>
 *  - Set the component registry, evaluator and seed. <br>
 *  - Set the required interface. <br>
 *  - Then call `init` to initialize a SimpleHASCO instance, which then can be accessed through a getter. <br>
 *
 */
public class SimpleHASCOStdBuilder implements
        IAlgorithmFactory<RefinementConfiguredSoftwareConfigurationProblem<Double>, HASCOSolutionCandidate<Double>, SimpleHASCOAlgorithmView> {

    private final static Logger logger = LoggerFactory.getLogger(SimpleHASCOStdBuilder.class);

    private ComponentRegistry registry;

    private String requiredInterface;

    private final SimpleHASCOConfig config = ConfigCache.getOrCreate(SimpleHASCOConfig.class);

    private ComponentEvaluator evaluator;

    boolean initFlag = false;

    private SimpleHASCO hasco;

    private StdCandidateContainer container;

    private BestCandidateCache candidateCache;

    private SimpleHASCOAlgorithmView view;

    private boolean artificialRoot = true;

    public SimpleHASCOStdBuilder() {
    }

    private boolean initialized() {
        return initFlag;
    }

    private void assertInitialized() {
        if(!initialized()) {
            throw new IllegalStateException("HASCO is not initialized. Call init first!");
        }
    }

    private void assertUninitialized() {
        if(initialized()) {
            throw new IllegalStateException("HASCO is already initialized.");
        }
    }

    public void setSeed(Long seed) {
        assertUninitialized();
        config.setProperty(SimpleHASCOConfig.HASCO_SIMPLIFIED_SEED, String.valueOf(seed));
    }

    public void setRegistry(ComponentRegistry registry) {
        assertUninitialized();
        this.registry = registry;
    }

    public void setComponentLoader(ComponentLoader componentLoader) {
        ComponentRegistry registry = ComponentRegistry.fromComponentLoader(componentLoader);
        setRegistry(registry);
    }

    public void setRequiredInterface(String requiredInterface) {
        assertUninitialized();
        this.requiredInterface = requiredInterface;
    }

    public void setPublishNodeEvent(boolean publishNodes) {
        assertUninitialized();
        config.setProperty(HASCO_SIMPLIFIED_PUBLISH_NODES, String.valueOf(publishNodes));
    }

    public void setEvaluator(ComponentEvaluator evaluator) {
        assertUninitialized();
        this.evaluator = evaluator;
    }

    public void setRefinementTime(long duration, TimeUnit timeUnit) {
        long refinementTime = timeUnit.toMillis(duration);
        if(refinementTime < 1) {
            throw new IllegalArgumentException("The max refinement time needs to be positive: " + refinementTime);
        }
        config.setProperty(HASCO_SIMPLIFIED_REFINEMENT_TIME,  Long.toString(refinementTime));
    }

    public void setSampleTime(long duration, TimeUnit timeUnit) {
        long sampleTime = timeUnit.toMillis(duration);
        if(sampleTime < 1) {
            throw new IllegalArgumentException("The max sample time needs to be positive: " + sampleTime);
        }
        config.setProperty(HASCO_SIMPLIFIED_SAMPLE_TIME, String.valueOf(sampleTime));
    }

    public void setFlagRandomNumericSampling(boolean flagRandomNumericSampling) {
        config.setProperty(HASCO_SIMPLIFIED_STD_RANDOM_NUMERIC_SPLIT, String.valueOf(flagRandomNumericSampling));
        config.setProperty(HASCO_SIMPLIFIED_STD_DRAW_NUMERIC_SPLIT_MEANS, String.valueOf(!flagRandomNumericSampling));
    }

    public void setThreadCount(int threadCount) {
        if(threadCount < 1) {
            throw new IllegalArgumentException("The thread count needs to be positive: " + threadCount);
        }
        config.setProperty(HASCO_SIMPLIFIED_THREAD_COUNT, String.valueOf(threadCount));
    }

    public void setMinEvalQueueSize(int minEvalQueueSize) {
        if(minEvalQueueSize < 1) {
            throw new IllegalArgumentException("The queue size needs to be positive: " + minEvalQueueSize);
        }
        config.setProperty(HASCO_SIMPLIFIED_MIN_EVAL_QUEUE_SIZE, String.valueOf(minEvalQueueSize));
    }

    public void setSamplesPerRefinement(int samplesPerRefinement) {
        if(samplesPerRefinement < 1) {
            throw new IllegalArgumentException("Samples need to be positive: " + samplesPerRefinement);
        }
        config.setProperty(HASCO_SIMPLIFIED_SAMPLES_PER_REFINEMENT, String.valueOf(samplesPerRefinement));
    }

    public void setArtificialRoot(boolean artificialRoot) {
        this.artificialRoot = artificialRoot;
    }


    private long getSeed() {
        Long seed = config.getSeed();
        if(seed == null) {
            return new Random().nextLong();
        }
        return seed;
    }

    public void init() {
        assertUninitialized();
        Objects.requireNonNull(requiredInterface, "No required interface defined.");

        // Get all the components that are offering the required interface
        List<Component> providers = registry.getProvidersOf(requiredInterface);
        if(providers.isEmpty()) {
            logger.warn("Not a single component provides the requested interface: {}", requiredInterface);
        }
//
//        StdCandidateContainer candidates = ctx.getBean(StdCandidateContainer.class);
//
//        candidates.setRequired(requiredInterface);
//
//        // insert the components that provide the required interface as roots
//        providers.stream()
//                .map(CIPhase1::createRoot)
//                .forEach(candidates::insertRoot);
//
        initFlag = true;

        logger.info("Initialized a Simple HASCO instance for the requested interface: {}", requiredInterface);
        assertInitialized();
    }

    @Override
    public SimpleHASCOAlgorithmView getAlgorithm() {
        if(!initialized()) {
            init();
            getSimpleHASCOInstance();
        }
        return view;
    }

    @Override
    public SimpleHASCOAlgorithmView getAlgorithm(final RefinementConfiguredSoftwareConfigurationProblem<Double> problem) {
        assertUninitialized();
        ComponentRegistry registry = new ComponentRegistry(new ArrayList<>(problem.getComponents()),
                problem.getParamRefinementConfig());
        setRegistry(registry);
        setRequiredInterface(problem.getRequiredInterface());
        ComponentEvaluator evaluator = ComponentEvaluator.fromIObjectEvaluator(problem.getCompositionEvaluator());
        setEvaluator(evaluator);
        return getAlgorithm();
    }

    public SimpleHASCO getSimpleHASCOInstance() {
        if(!initialized()) {
            init();
        }
        if(hasco != null) {
            return hasco;
        }
        container = new StdCandidateContainer();
        candidateCache = new BestCandidateCache(container);
        Objects.requireNonNull(evaluator);
        StdSampler sampler = new StdSampler(registry, getSeed());
        StdRefiner refiner = new StdRefiner(registry);
        ParallelRefParallelSampleScheduler scheduler = new ParallelRefParallelSampleScheduler(config.getWorkerThreadCount());
        hasco = new SimpleHASCO(container, candidateCache, evaluator, scheduler, sampler, refiner);

        IObjectEvaluator<ComponentInstance, Double> objectEvaluator = evaluator.toIObjectEvaluator();
        SoftwareConfigurationProblem coreProblem = new SoftwareConfigurationProblem<Double>(registry.getComponentList(), requiredInterface, objectEvaluator);
        RefinementConfiguredSoftwareConfigurationProblem<Double> input = new RefinementConfiguredSoftwareConfigurationProblem<>(coreProblem, registry.getParamConfigs());
        view = new SimpleHASCOAlgorithmView(Optional.of(config), input, hasco, candidateCache);
        container.addEventPublisher(view);
        container.setRequired(requiredInterface);
        initializeRootComponents();
        return hasco;
    }

    private void initializeRootComponents() {
        if(artificialRoot) {
            CIPhase1 root = createArtificialComponent();
            container.insertRoot(root);
        } else {
            registry.getProvidersOf(requiredInterface)
                .stream()
                .map(CIPhase1::createRoot)
                .forEach(container::insertRoot);
        }
    }

    private CIPhase1 createArtificialComponent() {
        Component artificialComponent = new Component("ROOT", Collections.emptyList(),
                Collections.singletonMap(Objects.requireNonNull(requiredInterface), Objects.requireNonNull(requiredInterface)),
                new PartialOrderedSet<>(), Collections.emptyList());
        CIPhase1 componentInstanceRoot = CIPhase1.createRoot(artificialComponent);
        return componentInstanceRoot;
    }

    public StdCandidateContainer getStdCandidateContainer() {
        assertInitialized();
        return container;
    }


    public BestCandidateCache getBestCandidateCache() {
        assertInitialized();
        return candidateCache;
    }

}
