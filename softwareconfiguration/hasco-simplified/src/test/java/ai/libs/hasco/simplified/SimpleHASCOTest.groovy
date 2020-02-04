package ai.libs.hasco.simplified

import ai.libs.hasco.model.Component
import ai.libs.hasco.model.ComponentInstance
import ai.libs.hasco.serialization.ComponentLoader
import ai.libs.hasco.simplified.std.CIPhase1
import ai.libs.hasco.simplified.std.StdHASCO
import spock.lang.Specification

class SimpleHASCOTest extends Specification {

    void setup() {
    }

    void cleanup() {
    }

    def "test simple problem"() {
        def requiredInterfaceName = 'IFace'
        def compsFile = new File("testrsc/simpleproblemwithtwocomponents.json")
        def compsLoader = new ComponentLoader(compsFile)
        def compsRegistry = ComponentRegistry.fromComponentLoader(compsLoader)
        def seed = 1L

        when: """
            We create a hasco runner on the two component problem.
            The evaluator will guide the path of the runner, by prioritizing certain components over others.
            This way we can check if the runner is indeed running through the search space the way intended.
        """
        StdHASCO hasco = new StdHASCO()
        hasco.seed = seed
        hasco.requiredInterface = requiredInterfaceName
        hasco.registry = compsRegistry
        hasco.evaluator = { componentInstance ->
            double score = 1.0;
            // The best component is A (a=true, b=v1)
            def params = componentInstance.parameterValues
            if(componentInstance.component.name == 'A') {
                if(params['a'] != 'true')
                    score += 1.0
                if(params['b'] != 'v1')
                    score += 1.0
            } else {
                // component B:
                score += 1.0
                // ignore parameters of B
            }
            return Optional.of(score)
        }
        StdHASCO.BestCandidateCache bestCandidate = hasco.closedList
        hasco.init()
        def runner = hasco.runner
        def openList = hasco.openList

        then:"""
           No solution has been seed yet.
           The open-list has two root components A and B with nothing refined yet.
        """
        ! hasco.getBestSeenCandidate().isPresent()
        openList.queue.size()  == 2

        when:
        ComponentInstance rootA = openList.queue.find { compInst ->
            compInst.component.name == "A"
        }
        ComponentInstance rootB = openList.queue.find { compInst ->
            compInst.component.name == "B"
        }

        then:
        [rootA, rootB].every { root ->
            root != null &&
            root instanceof CIPhase1 &&
            root.parameterValues.isEmpty() &&
            root.satisfactionOfRequiredInterfaces.isEmpty()
        }

        when:
        """
            Run to the end.
        """
        runner.runSequentially()
        def s = hasco.bestSeenCandidate.get()

        then:
        """
            Inspect best candidate found:
        """
        openList.queue.isEmpty()
        hasco.bestSeenScore.get() == 1.0
        s.component.name == 'A'
        s.parameterValues['a'] == 'true'
        s.parameterValues['b'] == 'v1'
    }

}
