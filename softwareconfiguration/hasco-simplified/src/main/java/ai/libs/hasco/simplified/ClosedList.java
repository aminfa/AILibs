package ai.libs.hasco.simplified;

import ai.libs.hasco.model.ComponentInstance;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public interface ClosedList {

    boolean isClosed(ComponentInstance candidate);

    void close(ComponentInstance componentInstance,
               List<ComponentInstance> witnesses,
               List<Optional<Double>> results);

}
