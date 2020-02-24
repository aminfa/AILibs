package ai.libs.hasco.simplified.std;

import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.serialization.ComponentLoader;
import ai.libs.hasco.simplified.*;
import ai.libs.hasco.simplified.schedulers.ParallelRefParallelSampleScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * This is the standard hasco implementation of SimpleHASCO that mimics the default reduction-based implementation. <br>
 * Each std hasco instance can be used at most for one search. <br>
 * In order to use this class do the following steps: <br>
 *  - Set the component registry, evaluator and seed. <br>
 *  - Set the required interface. <br>
 *  - Then call `init` to initialize a SimpleHASCO instance, which then can be accessed through a getter. <br>
 *
 */
public class SimpleHASCOStdBuilder {

    private final static Logger logger = LoggerFactory.getLogger(SimpleHASCOStdBuilder.class);

//    private final ApplicationContext ctx = new AnnotationConfigApplicationContext(SimpleHASCOStdConf.class);

    private ComponentRegistry registry = null;

    private Long seed = null;

    private StdCandidateContainer candidateContainer = null;

    private BestCandidateCache bestCandidateCache = null;

    private StdRefiner refiner = null;

    private StdSampler sampler = null;

    protected ComponentEvaluator evaluator;

    protected String requiredInterface;

    private long refinementTime = 8000, sampleTime = 2000;

    private boolean flagRandomNumericSampling = true;

    private int threadCount = 8;

    private int minEvalQueueSize = 4,
                samplesPerRefinement = 4;

    private BiConsumer<ComponentInstance, Double> sampleConsumer = (componentInstance, aDouble) -> {};

    private SimpleHASCO simpleHASCO;

    public SimpleHASCOStdBuilder() {

    }

    private boolean initialized() {
        return simpleHASCO != null;
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

    public StdRefiner getRefiner() {
        if(refiner == null) {
            refiner = new StdRefiner(getRegistry());
        }
        return refiner;
    }

    public StdSampler getSampler() {
        if(sampler == null)
            sampler = new StdSampler(getRegistry(), getSeed());
        return sampler;
    }

    public String getRequiredInterface() {
        if(requiredInterface == null) {
            throw new IllegalStateException();
        }
        return requiredInterface;
    }

    public ComponentEvaluator getEvaluator() {
        if(evaluator == null) {
            logger.warn("Evaluator wasn't initialized. Using a constant evaluator: `component_instance -> 1.0`");
            evaluator = c -> Optional.of(1.0);
        }
        return evaluator;
    }

    public ComponentRegistry getRegistry() {
        if(registry == null) {
            registry = ComponentRegistry.emptyRegistry();
            logger.warn("Component registry was not set. Using an empty component registry.");
        }
        return registry;
    }

    public Long getSeed() {
        if(seed == null) {
            seed = new Random().nextLong();
            logger.warn("Seed was not set. Using a random seed: {}", seed);
        }
        return seed;
    }

    public StdCandidateContainer getStdCandidateContainer() {
        if(candidateContainer == null) {
            candidateContainer = new StdCandidateContainer();
        }
        return candidateContainer;
    }

    public BestCandidateCache getBestCandidateCache() {
        if(bestCandidateCache == null) {
            bestCandidateCache = new BestCandidateCache(getStdCandidateContainer());
        }
        return bestCandidateCache;
    }

    public void setSeed(Long seed) {
        assertUninitialized();
        if(sampler != null)
            throw new IllegalStateException();
        this.seed = seed;
    }

    public void setRegistry(ComponentRegistry registry) {
        assertUninitialized();
        if(sampler != null || refiner != null)
            throw new IllegalStateException();
        this.registry = registry;
    }

    public void setComponentLoader(ComponentLoader componentLoader) {
        ComponentRegistry registry = ComponentRegistry.fromComponentLoader(componentLoader);
        setRegistry(registry);
    }

    public void setCandidateContainer(StdCandidateContainer candidateContainer) {
        assertUninitialized();
        this.candidateContainer = candidateContainer;
    }

    public void setRefiner(StdRefiner refiner) {
        assertUninitialized();
        this.refiner = refiner;
    }

    public void setSampler(StdSampler sampler) {
        assertUninitialized();
        this.sampler = sampler;
    }

    public void setSampleConsumer(BiConsumer<ComponentInstance, Double> sampleConsumer) {
        Objects.requireNonNull(sampleConsumer);
        this.sampleConsumer = sampleConsumer;
    }

    public void setRequiredInterface(String requiredInterface) {
        assertUninitialized();
        this.requiredInterface = requiredInterface;
    }

    public void setEvaluator(ComponentEvaluator evaluator) {
        assertUninitialized();
        this.evaluator = evaluator;
    }

    public void setRefinementTime(long duration, TimeUnit timeUnit) {
        this.refinementTime = timeUnit.toMillis(duration);
        if(sampleTime < 1) {
            throw new IllegalArgumentException("The max refinement time needs to be positive: " + sampleTime);
        }
    }

    public void setSampleTime(long duration, TimeUnit timeUnit) {
        this.sampleTime = timeUnit.toMillis(duration);
        if(sampleTime < 1) {
            throw new IllegalArgumentException("The max sample time needs to be positive: " + sampleTime);
        }
    }

    public void setFlagRandomNumericSampling(boolean flagRandomNumericSampling) {
        this.flagRandomNumericSampling = flagRandomNumericSampling;
    }

    public void setThreadCount(int threadCount) {
        if(threadCount < 1) {
            throw new IllegalArgumentException("The thread count needs to be positive: " + threadCount);
        }
        this.threadCount = threadCount;
    }

    public void setMinEvalQueueSize(int minEvalQueueSize) {
        if(minEvalQueueSize < 1) {
            throw new IllegalArgumentException("The queue size needs to be positive: " + minEvalQueueSize);
        }
        this.minEvalQueueSize = minEvalQueueSize;
    }

    public void setSamplesPerRefinement(int samplesPerRefinement) {
        if(samplesPerRefinement < 1) {
            throw new IllegalArgumentException("Samples need to be positive: " + samplesPerRefinement);
        }
        this.samplesPerRefinement = samplesPerRefinement;
    }

    public void init() {
        assertUninitialized();
        // Get all the components that are offering the required interface
        List<Component> providers = getRegistry().getProvidersOf(getRequiredInterface());
        if(providers.isEmpty()) {
            logger.warn("Not a single component provides the requested interface: {}", getRequiredInterface());
        }

        // insert the components that provide the required interface as roots
        providers.stream()
                .map(CIPhase1::createRoot)
                .forEach(getStdCandidateContainer()::insertRoot);

        getSampler().setSamplesPerDraw(samplesPerRefinement);
        getSampler().setDrawNumericSplitMeans(!flagRandomNumericSampling);

        // Create the single runner.
        simpleHASCO = new SimpleHASCO(getStdCandidateContainer(),
                getBestCandidateCache(),
                getEvaluator(),
                new ParallelRefParallelSampleScheduler(threadCount),
//                new SerialRefSerialSampleScheduler(),
                getSampler(),
                getRefiner());

        simpleHASCO.setMinEvalQueueSize(minEvalQueueSize);
        simpleHASCO.setRefinementEvalMaxTime(refinementTime);
        simpleHASCO.setSampleEvalMaxTime(sampleTime);

        getStdCandidateContainer().setSampleConsumer(this.sampleConsumer);

        logger.info("Initialized a Simple HASCO instance for the requested interface: {}", getRequiredInterface());
        assertInitialized();
    }

    public SimpleHASCO getSimpleHASCOInstance() {
        if(!initialized()) {
            init();
        }
        return simpleHASCO;
    }


}
