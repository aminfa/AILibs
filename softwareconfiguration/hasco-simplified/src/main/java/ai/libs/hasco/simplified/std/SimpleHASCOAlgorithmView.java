package ai.libs.hasco.simplified.std;

import ai.libs.hasco.core.HASCOSolutionCandidate;
import ai.libs.hasco.core.RefinementConfiguredSoftwareConfigurationProblem;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.simplified.SimpleHASCO;
import ai.libs.jaicore.basic.IOwnerBasedAlgorithmConfig;
import ai.libs.jaicore.basic.algorithm.AAlgorithm;
import org.api4.java.algorithm.events.IAlgorithmEvent;
import org.api4.java.algorithm.exceptions.AlgorithmException;
import org.api4.java.algorithm.exceptions.AlgorithmExecutionCanceledException;
import org.api4.java.algorithm.exceptions.AlgorithmTimeoutedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SimpleHASCOAlgorithmView extends AAlgorithm<RefinementConfiguredSoftwareConfigurationProblem<Double>, HASCOSolutionCandidate<Double>> {

    private SimpleHASCO simpleHASCO;

    private BestCandidateCache candidateCache;

    @Autowired
    public SimpleHASCOAlgorithmView(Optional<IOwnerBasedAlgorithmConfig> config, RefinementConfiguredSoftwareConfigurationProblem<Double> input, SimpleHASCO simpleHASCO, BestCandidateCache cache) {
        super(config.orElse(null), input);
        this.simpleHASCO = simpleHASCO;
        this.candidateCache = cache;
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
}
