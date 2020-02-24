package ai.libs.hasco.simplified.std;

import ai.libs.hasco.model.ComponentInstance;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * Allows parallel access.
 */
@Component
public class EvaluationCache implements Comparator<ComponentInstance> {

//    private Map<ComponentInstance, Optional<Double>> evalResults = new HashMap<>();
    private Map<String, Optional<Double>> evalResults = new HashMap<>();

    private synchronized void inject(ComponentInstance ci, Double result) {
        Optional<Double> newResult = Optional.ofNullable(result);
        String componentHash = ComponentDigest.digest(ci);
        Optional<Double> prevResult = evalResults.putIfAbsent(componentHash, newResult);
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
//            throw new IllegalArgumentException(errText);
        }
//        evalResults.put(componentHash, newResult);
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
        return evalResults.containsKey(ComponentDigest.digest(ci));
    }

    public Optional<Double> retrieveResult(ComponentInstance sample) {
        return evalResults.get(ComponentDigest.digest(sample));
    }

    private synchronized Optional<Double> readResult(ComponentInstance ci) {
        Objects.requireNonNull(ci);
        String componentHashVal = ComponentDigest.digest(ci);
        if(!evalResults.containsKey(componentHashVal)) {
            // TODO - do we throw exception instead?
//            throw new IllegalArgumentException("No result cached for: " + ci.toString());
            return Optional.empty();
        }
        return evalResults.get(componentHashVal);
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

    public int getSize() {
        return evalResults.size();
    }
}
