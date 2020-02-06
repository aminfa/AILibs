package ai.libs.hasco.simplified.std;

import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.simplified.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * This is the standard hasco implementation of SimpleHASCO that mimics the default reduction-based implementation. <br>
 * Each std hasco instance can be used exactly one time. <br>
 * In order to use this class do the following steps: <br>
 *  - Set the component registry, evaluator and seed. (Recommended, but optional) <br>
 *  - Set the required interface. <br>
 *  - Then call `init` to initialize a SimpleHASCO instance, which then can be accessed through a getter. <br>
 *
 */
public class StdHASCO {

    private final static Logger logger = LoggerFactory.getLogger(StdHASCO.class);

    private ComponentRegistry registry = null;

    private Long seed = null;

    private StdCandidateContainer candidateContainer = null;

    private BestCandidateCache bestCandidateCache = null;

    private StdRefiner refiner = null;

    private StdSampler sampler = null;

    private ComponentEvaluator evaluator;

    private String requiredInterface;

    private SimpleHASCO runner;

    public StdHASCO() {

    }

    private boolean initialized() {
        return runner != null;
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

    public StdCandidateContainer getOpenList() {
        if(candidateContainer == null) {
            candidateContainer = new StdCandidateContainer();
        }
        return candidateContainer;
    }

    public BestCandidateCache getClosedList() {
        if(bestCandidateCache == null) {
            bestCandidateCache = new BestCandidateCache();
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

    public void setRequiredInterface(String requiredInterface) {
        assertUninitialized();
        this.requiredInterface = requiredInterface;
    }

    public void setEvaluator(ComponentEvaluator evaluator) {
        assertUninitialized();
        this.evaluator = evaluator;
    }

    public void init() {
        assertUninitialized();
        // Get all the components that are offering the required interface
        List<Component> providers = registry.getProvidersOf(getRequiredInterface());
        if(providers.isEmpty()) {
            logger.warn("Not a single component provides the requested interface: {}", getRequiredInterface());
        }

        // insert the components that provide the required interface as roots
        providers.stream()
                .map(CIPhase1::createRoot)
                .forEach(getOpenList()::insertRoot);

        // Create the single runner.
        runner = new SimpleHASCO(getOpenList(),
                getClosedList(),
                getEvaluator(),
                getSampler(),
                getRefiner());

        logger.info("Initialized a Simple HASCO instance for the requested interface: {}", getRequiredInterface());
        assertInitialized();
    }

    public SimpleHASCO getRunner() {
        assertInitialized();
        return runner;
    }


    public Optional<ComponentInstance> getBestSeenCandidate() {
        return Optional.ofNullable(getClosedList().bestCandidate);
    }

    public Optional<Double> getBestSeenScore() {
        if(getBestSeenCandidate().isPresent())
            return Optional.of(getClosedList().bestScore);
        else
            return Optional.empty();
    }

    public class BestCandidateCache implements ClosedList {

        private double bestScore = Double.MAX_VALUE;

        private ComponentInstance root = null;

        private ComponentInstance bestCandidate = null;

        private boolean newCandidateFound = false;

        @Override
        public boolean isClosed(ComponentInstance candidate) {
            return getOpenList().isClosed(candidate);
        }

        private synchronized void checkAndUpdateCandidate(ComponentInstance root,
                                                          ComponentInstance candidate,
                                                          double score) {
            if(bestScore > score) {
                logger.info("A new best candidate found, score: {}", score);
                this.root = root;
                this.bestCandidate = candidate;
                this.bestScore = score;
                newCandidateFound = true;
            }
        }


        @Override
        public void insert(ComponentInstance componentInstance,
                           List<ComponentInstance> witnesses,
                           List<Optional<Double>> results) {
            getOpenList().insert(componentInstance, witnesses, results);
            for (int i = 0; i < witnesses.size(); i++) {
                Optional<Double> optScore = results.get(i);
                if(!optScore.isPresent()) {
                    continue;
                }
                Double score = optScore.get();
                ComponentInstance candidate = witnesses.get(i);
                checkAndUpdateCandidate(componentInstance, candidate, score);
            }
        }

        @Override
        public Optional<Optional<Double>> retrieve(ComponentInstance prototype, ComponentInstance sample) {
            return getOpenList().retrieve(prototype, sample);
        }

    }
}
