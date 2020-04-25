package ai.libs.hasco.simplified;

import ai.libs.hasco.model.ComponentInstance;

import java.util.List;

public interface ComponentRefiner {
    List<ComponentInstance> refine(ComponentInstance componentInstance);
}
