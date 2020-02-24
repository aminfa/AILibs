package ai.libs.hasco.simplified.schedulers;

import ai.libs.hasco.model.ComponentInstance;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

class SampleBatch implements Iterator<Integer> {

    private ComponentInstance refinement;

    private Timer batchTimer;

    private List<ComponentInstance> samples;
    private Timer[] sampleTimers;
    private Optional[] evalResults;
    private Future[] futures;

    AtomicInteger sampleCursor = new AtomicInteger(0);

    public SampleBatch(ComponentInstance refinement, List<ComponentInstance> samples,
                       long refinementMaxTime, long sampleMaxTime) {
        this.batchTimer = new Timer(refinementMaxTime);
        this.refinement = refinement;
        this.samples = samples;
        evalResults= new Optional[samples.size()];
        sampleTimers = new Timer[samples.size()];
        for (int i = 0; i < sampleTimers.length; i++) {
            sampleTimers[i] = new Timer(sampleMaxTime);
        }
        futures = new Future[samples.size()];
    }

    Optional<Long> nextTimeout() {
        if(!isRunning()) {
            return Optional.empty();
        }
        long nextTimeout = batchTimer.timeoutTime();
        for (int index = 0; index < samples.size(); index++) {
            long sampleTimeoutTime = sampleTimers[index].timeoutTime();
            if(sampleTimeoutTime < nextTimeout) {
                nextTimeout = sampleTimeoutTime;
            }
        }
        return Optional.of(nextTimeout);
    }

    synchronized Optional<Integer> sampleIndexWithSmallestTimeLeft() {
        if(!isRunning()) {
            return Optional.empty();
        }
        long minTimeleft = Long.MAX_VALUE;
        int minIndex = -1;
        for (int index = 0; index < samples.size(); index++) {
            if(sampleTimers[index].isRunning()) {
                long tl = sampleTimers[index].timeLeft();
                if(tl < minTimeleft) {
                    minTimeleft = tl;
                    minIndex = index;
                }
            }
        }
        if(minIndex == -1) {
            return Optional.empty();
        }
        else {
            return Optional.of(minIndex);
        }
    }


    synchronized boolean hasUnstartedSamples() {
        if(samples.size() > 0 && !batchTimer.hasStarted()) {
            return true;
        } else {
            for (Timer sampleTimer : sampleTimers) {
                if(!sampleTimer.hasStarted()) {
                    return true;
                }
            }
        }
        return false;
    }

    synchronized void setSampleEvalResult(int index, Optional<Double> result) {
        evalResults[index] = result;
        sampleTimers[index].end();
    }

    synchronized boolean isRunning() {
        if(!batchTimer.isRunning()) {
            return false;
        }
        for (int index = 0; index < samples.size(); index++) {
            if(sampleTimers[index].isRunning()) {
                return true;
            }
        }
        return false;
    }

    void startTimer() {
        batchTimer.start();
    }

    synchronized void startSampleTimer(int index) {
        startTimer();
        sampleTimers[index].start();
    }

    @Override
    public boolean hasNext() {
        return sampleCursor.get() < samples.size();
    }

    @Override
    public synchronized Integer next() {
        return sampleCursor.getAndIncrement();
    }

    ComponentInstance getSample(int index) {
        return samples.get(index);
    }

    List<ComponentInstance> getSamples() {
        return samples;
    }

    void setFuture(int index, Future future) {
        futures[index] = future;
    }

    Future<Optional<Double>> getFuture(int index) {
        return futures[index];
    }

    Timer getTimer(int i) {
        return sampleTimers[i];
    }

    public ComponentInstance getRefinement() {
        return refinement;
    }

    public Optional<Double> getResult(int index) {
        return evalResults[index];
    }

    public long getTimeLeft() {
        return batchTimer.timeLeft();
    }
}
