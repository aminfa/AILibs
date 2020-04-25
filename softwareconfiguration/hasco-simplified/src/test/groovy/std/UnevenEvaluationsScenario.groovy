package ai.libs.hasco.simplified.std

import ai.libs.hasco.model.ComponentInstance
import ai.libs.hasco.serialization.ComponentLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Specification

import java.util.concurrent.TimeUnit

class UnevenEvaluationsScenario extends Specification {

    private final static Logger logger = LoggerFactory.getLogger(UnevenEvaluationsScenario.class);
    def "uneven evals" () {
        def cl = new ComponentLoader(new File('testrsc/difficultproblem.json'))
        def builder = new SimpleHASCOStdBuilder()
        builder.seed = 1L
        builder.minEvalQueueSize = 5
        builder.samplesPerRefinement = 2
        builder.threadCount = 8
        builder.setSampleTime(4, TimeUnit.SECONDS)
        builder.setRefinementTime(8, TimeUnit.SECONDS)
        builder.componentLoader = cl
        builder.requiredInterface = 'IFace'

        builder.evaluator = { ComponentInstance sample ->
            if(sample.component.name == 'A' && sample.parameterValues['a1'] == 'true') {
                logger.info "Starting to sleep for 3.5 seconds"
                Thread.sleep(3500)
                logger.info "Finished sleeping for 3.5 seconds"
            } else {
                Thread.sleep(100)
            }
            return Optional.of(Double.valueOf(1.0D))
        }
        def simpleHasco = builder.simpleHASCOInstance
        simpleHasco.runUntil(200, TimeUnit.SECONDS)
        expect:
        true
    }
}
