package ai.libs.hasco.simplified;

import ai.libs.hasco.model.ComponentInstance;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface ComponentRefiner {
    List<ComponentInstance> refine(ComponentInstance componentInstance);
}
