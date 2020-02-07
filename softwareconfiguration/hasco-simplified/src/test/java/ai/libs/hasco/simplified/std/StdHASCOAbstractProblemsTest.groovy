package ai.libs.hasco.simplified.std

import ai.libs.hasco.model.ComponentInstance
import ai.libs.hasco.serialization.ComponentLoader
import ai.libs.hasco.simplified.ComponentRegistry
import spock.lang.Specification

/**
 * Applying the Std HASCO implementation to some abstract problem sets.
 */
class StdHASCOAbstractProblemsTest extends Specification {

    def "test simple problem with two components"() {
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
            /*
             * The evaluator that is fed to HASCO.
             * It guides to search towards a specific solution.
             * At the end we will check if the solution has been found.
             */
            double score = 1.0;
            // The best component is A (a=true, b=v1)
            def params = componentInstance.parameterValues
            if(componentInstance.component.name == 'A') {
                if(params['a'] != 'true')
                    score += 1.5
                if(params['b'] != 'v1')
                    score += 1.5
            } else {
                // component B:
                score += 1.0
                // ignore parameters of B
            }
            return Optional.of(score)
        }
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
        s.component.name == 'A'
        s.parameterValues['a'] == 'true'
        s.parameterValues['b'] == 'v1'
        hasco.bestSeenScore.get() == 1.0
    }

    def "test difficult problem" () {
        def requiredInterface = "IFace"
        def compsFile = new File("testrsc/difficultproblem.json")
        def compsLoader = new ComponentLoader(compsFile)
        def compsRegistry = ComponentRegistry.fromComponentLoader(compsLoader)
        def seed = 1L

        when:
        StdHASCO hasco = new StdHASCO()
        hasco.seed = seed
        hasco.requiredInterface = requiredInterface
        hasco.registry = compsRegistry
        hasco.evaluator = { componentInstance ->
            /*
             * The evaluator that is fed to HASCO.
             * It guides to search towards a specific solution.
             * At the end we will check if the solution has been found.
             */
            double score = 1.0;
            // The best component is A (a1=true, a2=v3, a3>=9.0, a4<=1.0, 5<=a5<=6)
            def params = componentInstance.parameterValues
            if(componentInstance.component.name == 'A') {
                if(params['a1'] != 'true')
                    score += 0.5
                if(params['a2'] != 'v3')
                    score += 0.5
                if(Double.parseDouble(params['a3']) <= 5.0)
                    score += 0.5
                else if(Double.parseDouble(params['a3']) <= 9.0)
                    score += 1.0
                score += 0.5
                if(Double.parseDouble(params['a4']) >= 5.0)
                    score += 0.5
                else if(Double.parseDouble(params['a4']) >= 9.0)
                    score += 0.2
                if(Double.parseDouble(params['a5']) <= 5.0)
                    score += 1.0
                else if(Double.parseDouble(params['a5']) >= 6.0)
                    score += 0.5
                return Optional.of(score)
            } else {
                // component B returns an error
                return Optional.empty()
            }
        }
        hasco.init()
        def runner = hasco.runner
        def openList = hasco.openList

        then:
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
        runner.runSequentially()
        def s = hasco.bestSeenCandidate.get()

        then:
        openList.queue.isEmpty()
        // A (a1=true, a2=v3, a3>=9.0, a4<=1.0, 5<=a5<=6)
        s.component.name == 'A'
        s.parameterValues['a1'] == 'true'
        s.parameterValues['a2'] == 'v3'
        Double.parseDouble(s.parameterValues['a3']) >= 9.0
        Double.parseDouble(s.parameterValues['a4']) <= 1.0
        Double.parseDouble(s.parameterValues['a5'])  - 5.0 <= 1.0
        hasco.bestSeenScore.get() == 1.0

    }

}
