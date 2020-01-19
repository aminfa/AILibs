package ai.libs.hasco.simplified;

import ai.libs.hasco.model.ComponentInstance;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Allows parallel access.
 */
public class EvaluationCache implements Comparator<ComponentInstance> {

    private Map<ComponentInstance, Optional<Double>> evalResults = new HashMap<>();

    private synchronized void inject(ComponentInstance ci, Double result) {
        Optional<Double> newResult = Optional.ofNullable(result);
        Optional<Double> prevResult = evalResults.putIfAbsent(ci, newResult);
        if(prevResult != null) {
            String errText = String.format("The component instance, %s," +
                            " already has an eval result cached." +
                            "\nComponent instance: %s" +
                            "\nPrev Result: %s" +
                            "\nNew Result: %s",
                    ci.getComponent().getName(),
                    ci.toString(),
                    resultToText(prevResult),
                    resultToText(newResult));
            throw new IllegalArgumentException(errText);
        }
        evalResults.put(ci, newResult);
    }

    private String resultToText(Optional<Double> result) {
        return result.map(Object::toString).orElse("Err");
    }

    public void insertEvalError(ComponentInstance ci) {
        Objects.requireNonNull(ci);
        this.inject(ci, null);
    }

    public void insertEvalResult(ComponentInstance ci, Double result) {
        Objects.requireNonNull(ci);
        Objects.requireNonNull(result);
        this.inject(ci, result);
    }

    public boolean containsResult(ComponentInstance ci) {
        return evalResults.containsKey(ci);
    }

    private synchronized Optional<Double> readResult(ComponentInstance ci) {
        Objects.requireNonNull(ci);
        if(!evalResults.containsKey(ci)) {
            // TODO - do we throw exception instead?
//            throw new IllegalArgumentException("No result cached for: " + ci.toString());
            return Optional.empty();
        }
        return evalResults.get(ci);
    }

    @Override
    public int compare(ComponentInstance o1, ComponentInstance o2) {
        Optional<Double> r1 = readResult(o1);
        Optional<Double> r2 = readResult(o2);
        if(r1.isPresent()) {
            if(r2.isPresent()) {
                // Both results are present compare by natural ordering
                return Double.compare(r1.get(), r2.get());
            } else {
                // r1 has result but r2 doesn't.
                return -1;
            }
        } else {
            if(r2.isPresent()) {
                // r2 has result but r1 doesn't.
                return 1;
            } else {
                // Both results are not present
                return 0;
            }
        }
    }


}
