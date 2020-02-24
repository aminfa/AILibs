package ai.libs.hasco.simplified.schedulers;

import ai.libs.hasco.simplified.ClosedList;
import ai.libs.hasco.simplified.ComponentEvaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// TODO
public class SerialRefSerialSampleScheduler implements EvalExecScheduler {

//    private Thread runner = new Thread(this);


    @Override
    public void scheduleAndExecute(EvalQueue queue, ComponentEvaluator evaluator, ClosedList closedList) throws InterruptedException {
        for (SampleBatch batch : queue.getBatches()) {
            batch.startTimer();
            List<Optional<Double>> results = new ArrayList<>(batch.getSamples().size());
            for (int i = 0; i < batch.getSamples().size(); i++) {
                long timeLeft = batch.getTimeLeft();
                if(timeLeft <= 0) {
                    results.add(Optional.empty());
                    continue;
                }
                Timer sampleTimer = batch.getTimer(i);
                if(timeLeft > sampleTimer.timeLeft()) {
                    timeLeft = sampleTimer.timeLeft();
                }
                Optional<Double> eval = evaluator.eval(batch.getSample(i));
                results.add(eval);
            }
            closedList.close(batch.getRefinement(), batch.getSamples(), results);
        }
    }


}
