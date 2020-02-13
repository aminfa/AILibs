package ai.libs.hasco.simplified.std

import ai.libs.hasco.model.ComponentInstance
import ai.libs.hasco.serialization.ComponentLoader
import ai.libs.hasco.simplified.ComponentRegistry
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicInteger

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
                score += 0.5
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
        def evalCounter = new AtomicInteger(0);

        when:
        StdHASCO hasco = new StdHASCO()
        hasco.seed = seed
        hasco.requiredInterface = requiredInterface
        hasco.registry = compsRegistry
        hasco.evaluator = { componentInstance ->
            evalCounter.incrementAndGet()
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
                else if(Double.parseDouble(params['a3']) <= 8.0)
                    score += 1.0
                if(Double.parseDouble(params['a4']) >= 9.0)
                    score += 0.2
                else if(Double.parseDouble(params['a4']) >= 2.0)
                    score += 0.5
                if(Double.parseDouble(params['a5']) <= 6.0)
                    score += 1.0
                else if(Double.parseDouble(params['a5']) >= 8)
                    score += 0.5
                return Optional.of(score)
            } else {
                score += 0.5
                return Optional.of(score)
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
        int steps = 0;
        int bestSolutionFoundAt = 0
        boolean solutionFound = false
        int highestQueueSize = 0;
        int bestSolutionStepCacheSize = 0;
        int bestSolutionStepEvalCount = 0;
        while(runner.step()) {
            def queueSize = openList.queue.size()
            println("QUEUE SIZE IS: ${queueSize}")
            if(queueSize > highestQueueSize)
                highestQueueSize = queueSize
            steps++;
            if(!solutionFound) {
                def score = hasco.bestSeenScore
                if(score.isPresent()) {
                    if(score.get() == 1.0) {
                        bestSolutionFoundAt = steps;
                        solutionFound = true
                        bestSolutionStepCacheSize = openList.cacheSize
                        bestSolutionStepEvalCount = evalCounter.get()
                    }
                }
            }
        }
        println("The entire search space was searched in ${steps} many steps.\n" +
                "The number of evaluations are: ${evalCounter.get()}.\n" +
                "The cache size is: ${openList.cacheSize}");
        println("The queue reached a maximum size of ${highestQueueSize}.")
        println("The solution was found at step ${bestSolutionFoundAt}, " +
                "the first ${((double)bestSolutionFoundAt)/steps} %  of all steps.\n" +
                "The solution was found with ${bestSolutionStepEvalCount} evaluation, at a cache size of ${bestSolutionStepCacheSize}")

        def s = hasco.bestSeenCandidate.get();
        then:
        openList.queue.isEmpty()
        // A (a1=true, a2=v3, a3>=9.0, a4<=1.0, 5<=a5<=6)
        hasco.bestSeenScore.get() == 1.0
        s.component.name == 'A'
        s.parameterValues['a1'] == 'true'
        s.parameterValues['a2'] == 'v3'
        Double.parseDouble(s.parameterValues['a3']) >= 8.0
        Double.parseDouble(s.parameterValues['a4']) <= 2.0
        Double.parseDouble(s.parameterValues['a5'])  - 6.0 <= 2.0

    }

    def "test problem with dependencies"() {

        def requiredInterface = "IFace"
        def compsFile = new File("testrsc/problemwithdependencies.json")
        def compsLoader = new ComponentLoader(compsFile)
        def compsRegistry = ComponentRegistry.fromComponentLoader(compsLoader)
        def seed = 1L
        def illegalEvals = [];

        when:
        StdHASCO hasco = new StdHASCO()
        hasco.seed = seed
        hasco.requiredInterface = requiredInterface
        hasco.registry = compsRegistry
        hasco.evaluator = { componentInstance ->
            /*
             * The evaluator that is fed to HASCO.
             * It guides to search towards a specific solution.
             * In this test the best solution invalidate param dependencies.
             * At the end we check if param dependencies are respected.
             */
            if(!ImplUtil.isValidComponentPrototype(componentInstance)) {
                println("Invalid component instance was evaluated: " + componentInstance);
                illegalEvals.add(componentInstance)
            }
            double score = 1.0;
            // The best component is B (d = none, c = true)
            // The best valid component is B(d=green/blue/.., c=true)
            def params = componentInstance.parameterValues
            if(componentInstance.component.name == 'A') {
                score += 1.5
            } else {
                if(params['d'] != 'none')
                    score += 0.5
                if(params['c'] != 'true')
                    score += 0.7
            }
            return Optional.of(score)
        }
        hasco.init()
        def runner = hasco.runner
        def openList = hasco.openList

        def invalidComponent
        while(runner.step()) {
            // Lets see if we find an invalid B component
            invalidComponent = openList.queue.find { ci ->
                if (ci.component.name == 'B') {
                    def params = ci.parameterValues
                    if (params['d'] == 'none' && params['c'] == 'true') {
                        return true
                    }
                }
                return false
            }
            if(invalidComponent != null) {
                break;
            }
        }
        def bestCandidate = hasco.bestSeenCandidate.get()

        then:
        invalidComponent == null
        hasco.bestSeenScore.get() == 1.5
        bestCandidate.component.name == 'B'
        bestCandidate.parameterValues['c'] == 'true'
        bestCandidate.parameterValues['d'] in ["red", "green", "blue", "white", "black"]
        illegalEvals.isEmpty()
    }

    def "test numeric dependencies test"() {
        def requiredInterface = "IFace"
        def compsFile = new File("testrsc/numericparamdependencies.json")
        def compsLoader = new ComponentLoader(compsFile)
        def compsRegistry = ComponentRegistry.fromComponentLoader(compsLoader)
        def seed = -1L
        def illegalEvals = [];

        when:
        StdHASCO hasco = new StdHASCO()
        hasco.seed = seed
        hasco.requiredInterface = requiredInterface
        hasco.registry = compsRegistry
        hasco.evaluator = { componentInstance ->
            /*
             * The evaluator that is fed to HASCO.
             * It guides to search towards a specific solution.
             * In this test the best solution invalidate param dependencies.
             * At the end we check if param dependencies are respected.
             */
            if(!ImplUtil.isValidComponentPrototype(componentInstance)) {
                println("Invalid component instance was evaluated: " + componentInstance);
                illegalEvals.add(componentInstance)
            }
            double score = 1.0;
            // The best component is A (a1 = true, a3 >= 5, a4 <= 5)
            // The best valid component is A(a=true,5 >= a3 >= 3, a4 <= 5)
            def params = componentInstance.parameterValues
            if(params['a1'] != 'true')
                score += 1.0
            if(Double.parseDouble(params['a3']) <= 5.0 )
                score += 0.5
            if(Double.parseDouble(params['a4']) >= 5.0 )
                score += 1.0
            return Optional.of(score)
        }
        hasco.init()
        def runner = hasco.runner
        def openList = hasco.openList

        def invalidComponent
        runner.runSequentially()
        def bestCandidate = hasco.bestSeenCandidate.get()

        then:
        invalidComponent == null
        hasco.bestSeenScore.get() == 1.5
        bestCandidate.component.name == 'A'
        bestCandidate.parameterValues['a1'] == 'true'
        Double.parseDouble(bestCandidate.parameterValues['a3']) - 3 <= 2.0
        Double.parseDouble(bestCandidate.parameterValues['a4']) <= 5.0

        illegalEvals.isEmpty()
    }

    def "test tiny recursive problem"() {
        def requiredInterface = "IFace"
        def compsFile = new File("testrsc/tinyrecursiveproblem.json")
        def compsLoader = new ComponentLoader(compsFile)
        def compsRegistry = ComponentRegistry.fromComponentLoader(compsLoader)
        def seed = 1L

        when:
        StdHASCO hasco = new StdHASCO()
        hasco.seed = seed
        hasco.requiredInterface = requiredInterface
        hasco.registry = compsRegistry
        hasco.evaluator = { a ->
            /*
             * The evaluator that is fed to HASCO.
             * It guides to search towards a specific solution.
             */
            double score = 1.0;
            // The best component is A( B( b=false, C (c = 10) ))
            def params = a.parameterValues
            if(a.component.name != 'A') {
                throw new IllegalArgumentException()
            }
            def b = a.getSatisfactionOfRequiredInterfaces()['ri']
            if(b.component.name != 'B') {
                throw new IllegalArgumentException()
            }
            if(b.parameterValues['b'] != 'false') {
                score += 1.0
            }
            def c = b.satisfactionOfRequiredInterfaces['rid']
            if(c.component.name != 'C') {
                throw new IllegalArgumentException()
            }
            score += 11 - Double.parseDouble(c.parameterValues['c'])
            return Optional.of(score)
        }
        hasco.init()
        def runner = hasco.runner
        def openList = hasco.openList

        def invalidComponent
        while(runner.step()) {
        }
        def bc = hasco.bestSeenCandidate.get()
        def bs = hasco.bestSeenScore.get()
        then:
        bs <= 3.0
        bc.satisfactionOfRequiredInterfaces['ri'].parameterValues['b'] == 'false'
        bc.satisfactionOfRequiredInterfaces['ri']
                .satisfactionOfRequiredInterfaces['rid'].parameterValues['c'] == '9'
    }
}

















