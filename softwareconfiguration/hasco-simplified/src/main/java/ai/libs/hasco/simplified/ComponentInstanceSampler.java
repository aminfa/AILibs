package ai.libs.hasco.simplified;

import ai.libs.hasco.model.ComponentInstance;

import java.util.List;

public interface ComponentInstanceSampler {
    List<ComponentInstance> drawSamples(ComponentInstance refined);
}
