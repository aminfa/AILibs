package ai.libs.hasco.simplified.std;

import ai.libs.hasco.core.HASCOSolutionCandidate;
import ai.libs.hasco.core.RefinementConfiguredSoftwareConfigurationProblem;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.simplified.SimpleHASCO;
import ai.libs.jaicore.basic.IOwnerBasedAlgorithmConfig;
import ai.libs.jaicore.basic.algorithm.AAlgorithm;
import ai.libs.jaicore.basic.algorithm.AlgorithmInitializedEvent;
import ai.libs.jaicore.basic.algorithm.EAlgorithmState;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.api4.java.algorithm.events.IAlgorithmEvent;
import org.api4.java.algorithm.exceptions.AlgorithmException;
import org.api4.java.algorithm.exceptions.AlgorithmExecutionCanceledException;
import org.api4.java.algorithm.exceptions.AlgorithmTimeoutedException;

import java.util.Optional;

public class SimpleHASCOAlgorithmView extends AAlgorithm<RefinementConfiguredSoftwareConfigurationProblem<Double>, HASCOSolutionCandidate<Double>> {

    private SimpleHASCO simpleHASCO;

    private BestCandidateCache candidateCache;

    private final EventBus eventBus;

    public SimpleHASCOAlgorithmView(Optional<IOwnerBasedAlgorithmConfig> config,
                                    RefinementConfiguredSoftwareConfigurationProblem<Double> input,
                                    SimpleHASCO simpleHASCO, BestCandidateCache cache,
                                    EventBus eventBus) {
        super(config.orElse(null), input);
        this.simpleHASCO = simpleHASCO;
        this.candidateCache = cache;
        this.eventBus = eventBus;
        super.registerListener(new Object() {
            @Subscribe
            public void rebroadcast(Object anyEvent) {
                eventBus.post(anyEvent);
            }
        });
    }

    @Override
    public IAlgorithmEvent nextWithException() throws InterruptedException, AlgorithmExecutionCanceledException, AlgorithmTimeoutedException, AlgorithmException {
        return null;
    }

    @Override
    public HASCOSolutionCandidate<Double> call() throws InterruptedException, AlgorithmExecutionCanceledException, AlgorithmTimeoutedException, AlgorithmException {
        while (hasNext()) {
            next();
        }
        Optional<ComponentInstance> bestSeenCandidate = candidateCache.getBestSeenCandidate();
        if(!bestSeenCandidate.isPresent()) {
            throw new AlgorithmException("No candidate found..");
        }
//        return new HASCOSolutionCandidate<>(bestSeenCandidate.get(), );
        return null;
    }

    @Override
    public void registerListener(final Object listener) {
        this.eventBus.register(listener);
    }


    @Override
    public String toString() {
        return "SimpleHASCO{std}";
    }

}
