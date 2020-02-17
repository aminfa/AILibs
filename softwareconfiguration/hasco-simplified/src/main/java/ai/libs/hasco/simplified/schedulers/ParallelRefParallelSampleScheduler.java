package ai.libs.hasco.simplified.schedulers;

import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.simplified.ClosedList;
import ai.libs.hasco.simplified.ComponentEvaluator;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

public class ParallelRefParallelSampleScheduler implements EvalExecScheduler {

    private ExecutorService execService;

    private Supplier<ExecutorService> serviceSupplier;

    public ParallelRefParallelSampleScheduler(final int threadCount) {
        this(() -> Executors.newFixedThreadPool(threadCount));
    }

    public ParallelRefParallelSampleScheduler(Supplier<ExecutorService> serviceSupplier) {
        this.serviceSupplier = serviceSupplier;
        this.execService = serviceSupplier.get();
    }

    private void shutDownRunningTasks() {
        execService.shutdownNow();
        execService = serviceSupplier.get();
    }

    @Override
    public void scheduleAndExecute(EvalQueue queue, ComponentEvaluator evaluator, ClosedList closedList) throws InterruptedException {
        Objects.requireNonNull(queue, "Queue is null");
        List<SampleBatch> batches = queue.getBatches();
        if(batches.isEmpty()) {
            throw new IllegalArgumentException("Queue is empty");
        }
        Objects.requireNonNull(evaluator);
        Objects.requireNonNull(closedList);
        try{
            submitAllSamples(batches, evaluator);
            awaitTermination(batches);
            gatherResults(batches, closedList);
        } catch (InterruptedException ex) {
            shutDownRunningTasks();
            throw new InterruptedException(ex.getMessage());
        }
    }



    private void submitAllSamples(List<SampleBatch> batches, ComponentEvaluator evaluator) {
        Objects.requireNonNull(execService, "Executor service is null");
        if(execService.isShutdown() || execService.isTerminated()) {
            throw new IllegalArgumentException("Executor service is not functional");
        }
        boolean sampleAdded = true;
        while(sampleAdded) {
            sampleAdded = false;
            for (SampleBatch batch : batches) {
                if (!batch.hasNext()) {
                    continue;
                }
                Integer index = batch.next();
                EvalRunner runner = new EvalRunner(evaluator, batch, index);
                Future<Optional<Double>> future = execService.submit(runner);
                sampleAdded = true;
                batch.setFuture(index, future);
            }
        }
    }


    public void awaitTermination(List<SampleBatch> batches) throws InterruptedException {
        boolean hasNext = true;
        while(hasNext) {
            hasNext = false;
            boolean processedOneSample = waitForNextSample(batches);
            if(processedOneSample) {
                hasNext = true;
                continue;
            }
            if(unstartedSamplesExist(batches)) {
                sleepMilliSecond();
                hasNext = true;
            }
        }
    }

    private void sleepMilliSecond() throws InterruptedException {
        Thread.sleep(1);
    }

    private boolean unstartedSamplesExist(List<SampleBatch> batches) {
        for (SampleBatch batch : batches) {
            if(batch.hasUnstartedSamples()) {
                return true;
            }
        }
        return false;
    }

    boolean waitForNextSample(List<SampleBatch> batches) throws InterruptedException {
        long sleepTime = Long.MAX_VALUE;
        SampleBatch batch = null;
        int sampleIndex = 0;
        for (SampleBatch cursor : batches) {
            Optional<Integer> index = cursor.sampleIndexWithSmallestTimeLeft();
            if(index.isPresent()) {
                int i = index.get();
                long iTtl = cursor.getTimer(i).timeLeft();
                long batchTimeLeft = cursor.getTimeLeft();
                if(iTtl > batchTimeLeft) {
                    iTtl = batchTimeLeft;
                }
                if(sleepTime > iTtl) {
                    sleepTime = iTtl;
                    batch = cursor;
                    sampleIndex = i;
                }
            }
            if(sleepTime <= 0) {
                break;
            }
        }
        if(batch == null) {
            return false;
        }
        waitFor(batch, sampleIndex, sleepTime);
        return true;
    }

    void waitFor(SampleBatch batch, int sampleIndex, long sleepTime) throws InterruptedException {
        Future<Optional<Double>> future = batch.getFuture(sampleIndex);
        Optional<Double> result;
        try {
            result = future.get(sleepTime, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            result = Optional.empty();
        } catch (TimeoutException e) {
            // timed out:
            future.cancel(true);
            batch.getTimer(sampleIndex).interrupt();
            result = Optional.empty();
        }
        batch.setSampleEvalResult(sampleIndex, result);
    }


    private void gatherResults(List<SampleBatch> batches, ClosedList closedList) {
        for (SampleBatch batch : batches) {
            if(batch.isRunning()) {
                throw new IllegalStateException("The batch is still running");
            }
            gatherResults(batch, closedList);
        }
    }

    private void gatherResults(SampleBatch batch, ClosedList closedList) {
        ComponentInstance refinement = batch.getRefinement();
        List<ComponentInstance> evaluatedSamples = new ArrayList<>(batch.getSamples().size());
        List<Optional<Double>> results = new ArrayList<>(batch.getSamples().size());
        for (int index = 0; index < batch.getSamples().size(); index++) {
            ComponentInstance sample = batch.getSample(index);
            if(!batch.getTimer(index).hasFinished()) {
                throw new IllegalStateException("Sample " + index + " has not finished yet.");
            }
            if(batch.getTimer(index).hasBeenInterrupted()) {
                continue;
            }
            Optional<Double> result = batch.getResult(index);
            Objects.requireNonNull(result);
            evaluatedSamples.add(batch.getSample(index));
            results.add(result);
        }
        closedList.insert(refinement, evaluatedSamples, results);
    }
}

