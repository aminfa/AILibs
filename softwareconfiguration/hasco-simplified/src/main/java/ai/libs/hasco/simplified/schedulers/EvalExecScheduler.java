package ai.libs.hasco.simplified.schedulers;

import ai.libs.hasco.simplified.ClosedList;
import ai.libs.hasco.simplified.ComponentEvaluator;
import org.springframework.stereotype.Component;

@Component
public interface EvalExecScheduler {

    void scheduleAndExecute(EvalQueue queue,
                            ComponentEvaluator evaluator,
                            ClosedList closedList) throws InterruptedException;

}
