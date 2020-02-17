package ai.libs.hasco.simplified.schedulers;

import ai.libs.hasco.model.ComponentInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class EvalQueue {

    private long refinementMaxTime;

    private long sampleMaxTime;

    private List<SampleBatch> batches = new ArrayList<>();

    public EvalQueue(long refinementMaxTime, long sampleMaxTime) {
        this.refinementMaxTime = refinementMaxTime;
        this.sampleMaxTime = sampleMaxTime;
    }

    public void enqueue(ComponentInstance refinement, List<ComponentInstance> samples) {
        Objects.requireNonNull(refinement, "refinement is null");
        if(Objects.requireNonNull(samples, "samples is null").isEmpty()) {
            throw new IllegalArgumentException("no samples");
        }
        SampleBatch batch = new SampleBatch(refinement, samples, refinementMaxTime, sampleMaxTime);
        synchronized (this) {
            batches.add(batch);
        }
    }

    List<SampleBatch> getBatches() {
        return batches;
    }
}
