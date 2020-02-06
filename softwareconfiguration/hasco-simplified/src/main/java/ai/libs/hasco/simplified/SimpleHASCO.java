package ai.libs.hasco.simplified;

import ai.libs.hasco.model.ComponentInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SimpleHASCO {

    private final static Logger logger = LoggerFactory.getLogger(SimpleHASCO.class);

    private OpenList openList;

    private ClosedList closedList;
    
    private ComponentEvaluator evaluator;

    private ComponentInstanceSampler sampler;

    private ComponentRefiner refiner;

    public SimpleHASCO(OpenList openList, ClosedList closedList, ComponentEvaluator evaluator, ComponentInstanceSampler sampler, ComponentRefiner refiner) {
        this.openList = openList;
        this.closedList = closedList;
        this.evaluator = evaluator;
        this.sampler = sampler;
        this.refiner = refiner;
    }

    private void drawSamplesAndEvaluate(ComponentInstance prototype, ComponentInstance refinement) throws InterruptedException {
        List<ComponentInstance> samples = sampler.drawSamples(refinement);
        List<Optional<Double>> results = new ArrayList<>(samples.size());
        for (ComponentInstance sample : samples) {
            Optional<Double> evalResult;
            Optional<Optional<Double>> retrievedResult = closedList.retrieve(prototype, sample);
            if(retrievedResult.isPresent()) {
                /*
                 * This sample has already been evaluated once:
                 */
                logger.trace("A component sample was already evaluated. The cached result is reused.");
                evalResult = retrievedResult.get();
            } else {
                /*
                 * Evaluate this sample for the first time.
                 */
                logger.trace("A component sample has no cached result, using the evaluator instead.");
                evalResult = evaluator.eval(sample);
            }
            results.add(evalResult);
            /*
             * Check if the thread is interrupted while the evaluator was processing
             */
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
        closedList.insert(refinement, samples, results);
    }

    /**
     * Refines the given candidate. It is assumed that it was taken from the open list. <br>
     * The candidate is refined and if necessary, the refined component instances are sampled and evaluated. <br>
     * The results are attached to the component instance and added to the closed list.
     *
     * @throws InterruptedException thrown, if the current thread is interrupted. This method checks for interruption while it evaluates candidate samples.
     * @param prototype The prototype to be processed.
     */
    private void refine(ComponentInstance prototype) throws InterruptedException {
        List<ComponentInstance> refinements = refiner.refine(prototype);
        Objects.requireNonNull(refinements, "Refiner returned null. Instead return empty list.");
        for(ComponentInstance refinedComponent : refinements) {
            drawSamplesAndEvaluate(prototype, refinedComponent);
        }
    }

    /**
     * Processes a single candidate from the open list. <br>
     * The candidate is removed from the list, then refined into more components instances. <br>
     * If necessary, the refined component instances are sampled and evaluated. <br>
     * The results are attached to the component instance and added to the open list again.
     *
     * If the open list is empty, nothing happens and false is returned.
     *
     * @return true, if a candidate has been popped from the list and processed.
     * @throws InterruptedException thrown, if the current thread is interrupted. This method checks for interruption while it evaluates candidates.
     */
    public boolean step() throws InterruptedException {
        if(!openList.hasNext()) {
            return false;
        }
        ComponentInstance prototype = openList.popNext();
        refine(prototype);
        return true;
    }

    /**
     * Processes all candidates in the open list.
     *
     * @throws InterruptedException thrown when the current thread is interrupted.
     */
    public void runSequentially() throws InterruptedException{
        while(openList.hasNext()) {
            step();
            if(Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
    }

    public OpenList getOpenList() {
        return openList;
    }

    public ClosedList getClosedList() {
        return closedList;
    }

    //    public void runInParallel(int numberOfThreads) throws InterruptedException, ExecutionException {
//        if(numberOfThreads <= 1) {
//            runSequentially();
//            return;
//        }
//        ExecutorService runner = Executors.newFixedThreadPool(numberOfThreads);
//        List<Callable<Object>> steps = new ArrayList<>();
//        while(openList.hasNext()) {
//            synchronized (this) {
//                for (int i = 0; i < numberOfThreads && openList.hasNext(); i++) {
//                    ComponentInstance prototype = openList.popNext();
//                    steps.add(() -> {this.process(prototype);return null;});
//                }
//                List<Future<Object>> futures = runner.invokeAll(steps);
//                for (Future future: futures) {
//                    try {
//                        if(future.isCancelled())
//                        future.get();
//                        future.isCancelled();
//                    } catch (ExecutionException e) {
//                        e.printStackTrace();
//                    } catch ()
//
//                }
//            }
//            if(Thread.interrupted()) {
//                throw new InterruptedException();
//            }
//        }
//    }
}