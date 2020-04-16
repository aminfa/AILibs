package ai.libs.hasco.simplified.std;

import ai.libs.hasco.core.RefinementConfiguredSoftwareConfigurationProblem;
import ai.libs.hasco.core.SoftwareConfigurationProblem;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.simplified.*;
import ai.libs.hasco.simplified.schedulers.EvalExecScheduler;
import ai.libs.hasco.simplified.schedulers.ParallelRefParallelSampleScheduler;
import ai.libs.hasco.simplified.schedulers.SerialRefSerialSampleScheduler;
import com.google.common.eventbus.EventBus;
import org.api4.java.common.attributedobjects.IObjectEvaluator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;

import java.util.Objects;

@Configuration
@ComponentScan
public class SimpleHASCOStdConf extends SimpleHASCOConf{

    @Bean
    public OpenList openList(StdCandidateContainer container) {
        return container;
    }

    @Bean
    @Primary
    StdCandidateContainer stdCandidateContainer(EventBus eventBus) {
        return new StdCandidateContainer(eventBus);
    }


    @Bean
    public ClosedList closedList(BestCandidateCache bestCandidate) {
        return bestCandidate;
    }

    @Bean
    @Primary
    BestCandidateCache bestCandidateCache(StdCandidateContainer container) {
        return new BestCandidateCache(container);
    }

    @Bean
    @Primary
    EvalExecScheduler evalExecScheduler(ApplicationContext ctx, @Value("${hasco.simplified.scheduling:serial}") String scheduling) {
        if(scheduling.equals("serial")) {
            return new SerialRefSerialSampleScheduler();
        } else if(scheduling.equals("parallel")) {
            return new ParallelRefParallelSampleScheduler(
                    Integer.parseInt(
                        Objects.requireNonNull(
                            ctx.getEnvironment().getProperty("hasco.simplified.threadCount"))));
        } else {
            throw new IllegalArgumentException(scheduling);
        }
    }

    @Bean
    @Primary
    RefinementService refinementService(StdRefinementService service) {
        return service;
    }

    @Bean
    @Primary
    ComponentInstanceSampler componentInstanceSampler(StdSampler sampler) {
        return sampler;
    }

    @Bean
    @Primary
    ComponentRefiner componentRefiner(StdRefiner stdRefiner) {
        return stdRefiner;
    }

    @Bean
    RefinementConfiguredSoftwareConfigurationProblem refinementConfiguredSoftwareConfigurationProblem(ApplicationContext ctx, ComponentEvaluator evaluator, ComponentRegistry registry, @Value("${hasco.simplified.rootRequiredInterface}") String requiredInterface) {
        IObjectEvaluator<ComponentInstance, Double> objectEvaluator = evaluator.toIObjectEvaluator();
        SoftwareConfigurationProblem coreProblem = new SoftwareConfigurationProblem<Double>(registry.getComponentList(), requiredInterface, objectEvaluator);
        RefinementConfiguredSoftwareConfigurationProblem<Double> input = new RefinementConfiguredSoftwareConfigurationProblem<>(coreProblem, registry.getParamConfigs());
        return input;
    }

}
