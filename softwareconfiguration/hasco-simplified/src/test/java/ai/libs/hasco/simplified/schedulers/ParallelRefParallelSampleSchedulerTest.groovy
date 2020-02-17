package ai.libs.hasco.simplified.schedulers

import ai.libs.hasco.model.Component
import ai.libs.hasco.model.ComponentInstance
import ai.libs.hasco.model.Parameter
import ai.libs.hasco.simplified.ClosedList
import ai.libs.hasco.simplified.ComponentEvaluator
import ai.libs.jaicore.basic.sets.PartialOrderedSet
import spock.lang.Specification

class ParallelRefParallelSampleSchedulerTest extends Specification {

    static ComponentInstance base;

    static {
        def component = new Component("base", [], [:], new PartialOrderedSet<Parameter>(), [])
        base = new ComponentInstance(component, [:], [:])
    }

    def "positiv parallel scheduling test"() {
        when:
        def queue = new EvalQueue(refMaxTime, sampleMaxTime)

        for (int a = 0; a < refinementCount; a++) {
            def refinement = new ComponentInstance(base)
            refinement.parameterValues['a'] = String.valueOf(a)
            def samples = []
            for (int b = 0; b < sampleCount; b++) {
                def sample  = new ComponentInstance(refinement)
                sample.parameterValues['b'] = String.valueOf(b)
                samples += sample
            }
            queue.enqueue(refinement, samples)
        }
        def expectedOrder = []
        for (int b = 0; b < refinementCount; b++) {
            for (int a = 0; a < sampleCount; a++) {
                expectedOrder += [
                        'a' : String.valueOf(a),
                        'b' : String.valueOf(b)
                ]
            }
        }
        def receivedEvalOrder = []
        ComponentEvaluator evaluator = { ComponentInstance sample ->
            receivedEvalOrder += sample.parameterValues
            Thread.sleep(evalDelay)
            return Optional.of(1.0)
        }

        def scheduler = new ParallelRefParallelSampleScheduler(threadCount)

        def receivedResults = []
        ClosedList closedList = new ClosedList() {
            @Override
            boolean isClosed(ComponentInstance candidate) {
                return false
            }

            @Override
            void insert(ComponentInstance componentInstance,
                        List<ComponentInstance> witnesses,
                        List<Optional<Double>> results) {
                receivedResults += witnesses.size()
            }
        }
        scheduler.scheduleAndExecute(queue, evaluator, closedList)

        then:
        receivedResults.size() == refinementCount
        receivedResults.every { it >= minSampleCount }
        threadCount != 1 || expectedOrder == receivedEvalOrder

        where:
        sampleCount | refinementCount   | refMaxTime    | sampleMaxTime | evalDelay | threadCount   | minSampleCount
        10          | 10                | 2000          | 100           | 10        | 1             | 10
        10          | 10                | 2000          | 100           | 30        | 5             | 10
        10          | 10                | 2000          | 100           | 30        | 5             | 10
        10          | 10                | 2000          | 100           | 30        | 10            | 6
        10          | 10                | 2000          | 100           | 50        | 10            | 10
    }

}
