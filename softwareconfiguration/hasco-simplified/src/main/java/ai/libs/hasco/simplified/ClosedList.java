package ai.libs.hasco.simplified;

import ai.libs.hasco.model.ComponentInstance;

import java.util.List;
import java.util.Optional;

public interface ClosedList {

    boolean isClosed(ComponentInstance candidate);

    void insert(ComponentInstance componentInstance,
                List<ComponentInstance> witnesses,
                List<Optional<Double>> results);

    /**
     *
     * @param prototype
     * @param sample
     * @return Null if no entry is found, an empty optional if an error result was found, or optional of evaluation result
     */
    Optional<Optional<Double>> retrieve(ComponentInstance prototype, ComponentInstance sample);
}
