package ai.libs.hasco.simplified;

import ai.libs.hasco.simplified.schedulers.EvalExecScheduler;
import ai.libs.hasco.simplified.std.BestCandidateCache;
import com.google.common.eventbus.EventBus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.*;

@Lazy
@Configuration(proxyBeanMethods = false)
@ComponentScan
public class SimpleHASCOConf {

    @Bean
    @Primary
    public ComponentEvaluator componentEvaluator(ApplicationContext context) {
        return (ComponentEvaluator) context.getBean("evaluator");
    }

    @Bean
    @Primary
    public ComponentRegistry componentRegistry(ApplicationContext context) {
        return (ComponentRegistry) context.getBean("registry");
    }

    @Bean
    @Primary
    public EventBus eventBus(ApplicationContext context) {
        return (EventBus) context.getBean("bus");
    }


}
