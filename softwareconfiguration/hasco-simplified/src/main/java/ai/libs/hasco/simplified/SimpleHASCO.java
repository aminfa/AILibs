package ai.libs.hasco.simplified;

import ai.libs.hasco.model.ComponentInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SimpleHASCO {

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



    /**
     * Processes the given candidate. It is assumed that it was taken from the open list. <br>
     * The candidate is refined and if necessary, the refined component instances are sampled and evaluated. <br>
     * The results are attached to the component instance and added to the open list again.
     *
     * If the open list is empty, nothing happens.
     *
     * @throws InterruptedException thrown, if the current thread is interrupted. This method checks for interruption while it evaluates candidate samples.
     * @param prototype The prototype to be processed.
     */
    private void process(ComponentInstance prototype) throws InterruptedException {
        List<ComponentInstance> refinements = refiner.refine(prototype);
        Objects.requireNonNull(refinements, "Refiner returned null. Instead return empty list.");
        for(ComponentInstance refinedComponent : refinements) {
            if (!closedList.isClosed(refinedComponent)) {
                List<ComponentInstance> samples = sampler.drawSamples(refinedComponent);
                List<Optional<Double>> results = new ArrayList<>(samples.size());
                for (ComponentInstance sample : samples) {
                    Optional<Double> evalResult = evaluator.eval(sample);
                    results.add(evalResult);
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                }
                closedList.insert(refinedComponent, samples, results);
            }
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
        process(prototype);
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