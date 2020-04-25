package ai.libs.hasco.simplified;

import ai.libs.hasco.model.ComponentInstance;

import java.util.List;
import java.util.Optional;

public interface ClosedList {

    boolean isClosed(ComponentInstance candidate);

    void close(ComponentInstance componentInstance,
               List<ComponentInstance> witnesses,
               List<Optional<Double>> results);

}
