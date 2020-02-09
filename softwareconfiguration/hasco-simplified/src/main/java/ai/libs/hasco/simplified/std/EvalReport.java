package ai.libs.hasco.simplified.std;


import ai.libs.hasco.model.ComponentInstance;
import com.google.common.base.Optional;

import java.util.List;

public class EvalReport {

    private Optional<Double> score;

    private List<ComponentInstance> witnesses;

    public EvalReport(Optional<Double> score,
                      List<ComponentInstance> witnesses) {
        this.score = score;
        this.witnesses = witnesses;
    }

    public Optional<Double> getScore() {
        return score;
    }

    public List<ComponentInstance> getWitnesses() {
        return witnesses;
    }
}
