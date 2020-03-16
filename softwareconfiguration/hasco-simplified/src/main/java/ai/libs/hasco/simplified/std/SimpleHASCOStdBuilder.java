package ai.libs.hasco.simplified.std;

import ai.libs.hasco.core.HASCOSolutionCandidate;
import ai.libs.hasco.core.RefinementConfiguredSoftwareConfigurationProblem;
import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.serialization.ComponentLoader;
import ai.libs.hasco.simplified.*;
import org.api4.java.algorithm.IAlgorithmFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.util.*;
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
public class SimpleHASCOStdBuilder implements
        IAlgorithmFactory<RefinementConfiguredSoftwareConfigurationProblem<Double>, HASCOSolutionCandidate<Double>, SimpleHASCOAlgorithmView> {

    private final static Logger logger = LoggerFactory.getLogger(SimpleHASCOStdBuilder.class);

    private AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();

    private final Map<String, Object> builderProperties = new HashMap<>();

    private BiConsumer<ComponentInstance, Double> sampleConsumer = (componentInstance, aDouble) -> {};

    boolean initFlag = false;

    public SimpleHASCOStdBuilder() {
    }

    public SimpleHASCOStdBuilder(AnnotationConfigApplicationContext ctx) {
        this.ctx = ctx;
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
        this.builderProperties.put("hasco.simplified.seed", seed.toString());
    }

    public void setRegistry(ComponentRegistry registry) {
        assertUninitialized();
        ctx.getBeanFactory().registerSingleton("registry", registry);
    }

    public void setComponentLoader(ComponentLoader componentLoader) {
        ComponentRegistry registry = ComponentRegistry.fromComponentLoader(componentLoader);
        setRegistry(registry);
    }

    public void setSampleConsumer(BiConsumer<ComponentInstance, Double> sampleConsumer) {
        Objects.requireNonNull(sampleConsumer);
        assertUninitialized();
        ctx.getBeanFactory().registerSingleton("sampleConsumer", sampleConsumer);
    }

    public void setRequiredInterface(String requiredInterface) {
        assertUninitialized();
        this.builderProperties.put("hasco.simplified.rootRequiredInterface", requiredInterface);
    }

    public void setEvaluator(ComponentEvaluator evaluator) {
        assertUninitialized();
        ctx.getBeanFactory().registerSingleton("evaluator", evaluator);
    }

    public void setSerialScheduling() {
        this.setScheduling("serial");
    }

    public void setParallelScheduling() {
        this.setScheduling("parallel");
    }

    public void setAsyncSerialScheduling() {
        this.setScheduling("async-serial");
    }

    public void setAsyncParallelScheduling() {
        this.setScheduling("async-parallel");
    }

    public void setScheduling(String scheduling) {
        assertInitialized();
        Objects.requireNonNull(scheduling);
        this.builderProperties.put("hasco.simplified.scheduling", scheduling);
    }

    public void setRefinementTime(long duration, TimeUnit timeUnit) {
        long refinementTime = timeUnit.toMillis(duration);
        if(refinementTime < 1) {
            throw new IllegalArgumentException("The max refinement time needs to be positive: " + refinementTime);
        }
        builderProperties.put("hasco.simplified.refinementTime", Long.toString(refinementTime));
    }

    public void setSampleTime(long duration, TimeUnit timeUnit) {
        long sampleTime = timeUnit.toMillis(duration);
        if(sampleTime < 1) {
            throw new IllegalArgumentException("The max sample time needs to be positive: " + sampleTime);
        }
        builderProperties.put("hasco.simplified.sampleTime", Long.toString(sampleTime));
    }

    public void setFlagRandomNumericSampling(boolean flagRandomNumericSampling) {
        builderProperties.put("hasco.simplified.std.randomNumericSplit", String.valueOf(flagRandomNumericSampling));
        builderProperties.put("hasco.simplified.std.drawNumericSplitMeans", String.valueOf(!flagRandomNumericSampling));
    }

    public void setThreadCount(int threadCount) {
        if(threadCount < 1) {
            throw new IllegalArgumentException("The thread count needs to be positive: " + threadCount);
        }
        builderProperties.put("hasco.simplified.threadCount", String.valueOf(threadCount));
    }

    public void setMinEvalQueueSize(int minEvalQueueSize) {
        if(minEvalQueueSize < 1) {
            throw new IllegalArgumentException("The queue size needs to be positive: " + minEvalQueueSize);
        }
        builderProperties.put("hasco.simplified.minEvalQueueSize", String.valueOf(minEvalQueueSize));
    }

    public void setSamplesPerRefinement(int samplesPerRefinement) {
        if(samplesPerRefinement < 1) {
            throw new IllegalArgumentException("Samples need to be positive: " + samplesPerRefinement);
        }
        builderProperties.put("hasco.simplified.std.samplesPerRefinement", String.valueOf(samplesPerRefinement));
    }

    public void initializeBuilderProperties() {
        ConfigurableEnvironment environment = ctx.getEnvironment();

        MutablePropertySources propertySources = environment.getPropertySources();
        propertySources.addFirst(new MapPropertySource("BUILDER_PROPERTIES_" + this.toString(), builderProperties));
    }

    public void initializeCtx() {
        assertUninitialized();
        initializeBuilderProperties();
        ctx.register(SimpleHASCOStdConf.class);
        ctx.refresh();
    }

    public void init() {
        assertUninitialized();
        initializeCtx();

        ComponentRegistry registry = ctx.getBean(ComponentRegistry.class);
        String requiredInterface = ctx.getEnvironment().getProperty("hasco.simplified.rootRequiredInterface");
        Objects.requireNonNull(requiredInterface, "No required interface defined.");

        // Get all the components that are offering the required interface
        List<Component> providers = registry.getProvidersOf(requiredInterface);
        if(providers.isEmpty()) {
            logger.warn("Not a single component provides the requested interface: {}", requiredInterface);
        }

        StdCandidateContainer candidates = ctx.getBean(StdCandidateContainer.class);

        // insert the components that provide the required interface as roots
        providers.stream()
                .map(CIPhase1::createRoot)
                .forEach(candidates::insertRoot);

        initFlag = true;

        logger.info("Initialized a Simple HASCO instance for the requested interface: {}", requiredInterface);
        assertInitialized();
    }

    @Override
    public SimpleHASCOAlgorithmView getAlgorithm() {
        return ctx.getBean(SimpleHASCOAlgorithmView.class);
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
        return ctx.getBean(SimpleHASCO.class);
    }

    public StdCandidateContainer getStdCandidateContainer() {
        assertInitialized();
        return ctx.getBean(StdCandidateContainer.class);
    }


    public BestCandidateCache getBestCandidateCache() {
        assertInitialized();
        return ctx.getBean(BestCandidateCache.class);
    }

}
