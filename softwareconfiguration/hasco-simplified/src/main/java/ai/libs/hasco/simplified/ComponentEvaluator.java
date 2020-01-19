package ai.libs.hasco.simplified;

import ai.libs.hasco.model.ComponentInstance;

import java.util.Optional;

public interface ComponentEvaluator {

    Optional<Double> eval(ComponentInstance refined);

}
