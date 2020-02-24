package ai.libs.hasco.simplified.schedulers;

import ai.libs.hasco.model.ComponentInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class EvalQueue {

    private long refinementMaxTime;

    private long sampleMaxTime;

    private List<SampleBatch> batches = new ArrayList<>();

    private int size = -1;

    public EvalQueue(long refinementMaxTime, long sampleMaxTime) {
        this.refinementMaxTime = refinementMaxTime;
        this.sampleMaxTime = sampleMaxTime;
    }

    public void enqueue(ComponentInstance refinement, List<ComponentInstance> samples) {
        Objects.requireNonNull(refinement, "refinement is null");
        if(Objects.requireNonNull(samples, "samples is null").isEmpty()) {
//            throw new IllegalArgumentException("no samples");
        }
        SampleBatch batch = new SampleBatch(refinement, samples, refinementMaxTime, sampleMaxTime);
        batches.add(batch);
        size = -1;
    }

    List<SampleBatch> getBatches() {
        return Collections.unmodifiableList(batches);
    }

    public int size() {
        if(size == -1) {
            size = batches.stream()
                    .map(batch->batch.getSamples().size())
                    .reduce(0, Integer::sum);
        }
        return size;
    }

    public int batchCount() {
        return batches.size();
    }
}
