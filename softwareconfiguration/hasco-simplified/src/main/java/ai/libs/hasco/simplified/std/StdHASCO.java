package ai.libs.hasco.simplified.std;

import ai.libs.hasco.simplified.*;
import it.unimi.dsi.fastutil.Hash;

import java.util.Collections;
import java.util.HashMap;
import java.util.Random;

public class StdHASCO extends SimpleHASCO {


    private StdHASCO(OpenList openList, ClosedList closedList, ComponentEvaluator evaluator, ComponentInstanceSampler sampler, ComponentRefiner refiner) {
        super(openList, closedList, evaluator, sampler, refiner);
    }

    public static SimpleHASCO createStdImpl(ComponentRegistry registry, ComponentEvaluator evaluator, String requiredInterface, long seed) {
        StdCandidateContainer container = new StdCandidateContainer();
        registry.getProvidersOf(requiredInterface).forEach(provider -> {
            CIPhase1 root = CIPhase1.createRoot(provider);
            container.insertRoot(root);
        });
        StdRefiner refiner = new StdRefiner(registry);
        StdSampler sampler = new StdSampler(registry, seed);
        return new SimpleHASCO(container, container, evaluator, sampler, refiner);
    }
}
