package ai.libs.hasco.simplified;

import ai.libs.hasco.model.ComponentInstance;
import org.springframework.stereotype.Component;

@Component
public interface OpenList {

    boolean hasNext();

    ComponentInstance popNext();

}
