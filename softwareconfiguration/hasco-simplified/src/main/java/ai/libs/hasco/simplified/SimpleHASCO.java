package ai.libs.hasco.simplified;

import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.simplified.schedulers.EvalExecScheduler;
import ai.libs.hasco.simplified.schedulers.EvalQueue;
import org.aeonbits.owner.ConfigCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class SimpleHASCO {

    private final static Logger logger = LoggerFactory.getLogger(SimpleHASCO.class);

    private OpenList openList;

    private ClosedList closedList;

    private ComponentEvaluator evaluator;

    private EvalExecScheduler scheduler;

    private ComponentInstanceSampler sampler;

    private ComponentRefiner refiner;

    private final int minEvalQueueSize;

    private final long refinementEvalMaxTime;

    private final long sampleEvalMaxTime;


    {
        SimpleHASCOConfig config = ConfigCache.getOrCreate(SimpleHASCOConfig.class);
        this.minEvalQueueSize = config.getMinEvalQueueSize();
        this.refinementEvalMaxTime = config.getRefinementTime();
        this.sampleEvalMaxTime = config.getSampleTime();
    }

    public SimpleHASCO(OpenList openList,
                       ClosedList closedList,
                       ComponentEvaluator evaluator, EvalExecScheduler scheduler,
                       ComponentInstanceSampler sampler,
                       ComponentRefiner refiner) {
        this.openList = openList;
        this.closedList = closedList;
        this.evaluator = evaluator;
        this.scheduler = scheduler;
        this.sampler = sampler;
        this.refiner = refiner;
        logger.debug("Initialized Simple HASCO with {} min eval queue, {} refinement time and {} sample time.",
                minEvalQueueSize, refinementEvalMaxTime, sampleEvalMaxTime);
    }

    private void drawSamples(ComponentInstance refinement, EvalQueue queue) {
        List<ComponentInstance> samples = sampler.drawSamples(refinement);
        logger.info("Sampling refinement {} resulted in {} many samples.", refinement, samples.size());
        if(samples.isEmpty()) {
            closedList.close(refinement, Collections.emptyList(), Collections.emptyList());
        } else {
            queue.enqueue(refinement, samples);
        }
    }

    /**
     * Refines the given candidate. It is assumed that it was taken from the open list. <br>
     * The candidate is refined and if necessary, the refined component instances are sampled and evaluated. <br>
     * The results are attached to the component instance and added to the closed list.
     *
     * @param prototype The prototype to be processed.
     * @param queue
     */
    private void refine(ComponentInstance prototype, EvalQueue queue) {
        Objects.requireNonNull(prototype, "Prototype is null");
        List<ComponentInstance> refinements = refiner.refine(prototype);
        Objects.requireNonNull(refinements, "Refiner returned null. Instead return empty list.");
        logger.info("Refinement process yielded {} many refinements.", refinements.size());
        for(ComponentInstance refinedComponent : refinements) {
            drawSamples(refinedComponent, queue);
        }
    }

    /**
     * Processes candidates from the open list until . <br>
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
        EvalQueue queue = new EvalQueue(refinementEvalMaxTime, sampleEvalMaxTime);
        int size = queue.size();
        while(openList.hasNext() && (size < minEvalQueueSize || size == 0)) {
            ComponentInstance prototype = openList.popNext();
            refine(prototype, queue);
            size = queue.size();
        }
        if(queue.size() == 0) {
            return false;
        }
        scheduler.scheduleAndExecute(queue, evaluator, closedList);
        return true;
    }

    /**
     * Processes all candidates in the open list.
     *
     * @throws InterruptedException thrown when the current thread is interrupted.
     */
    public void runAll() throws InterruptedException{
        while(step()) {
            if(Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
    }

    public void runUntil(long duration, TimeUnit unit) throws InterruptedException {
        long threshold = System.currentTimeMillis() + unit.toMillis(duration);
        while(System.currentTimeMillis() <= threshold && step()) {
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

}