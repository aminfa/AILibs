package ai.libs.hasco.simplified.std;


import ai.libs.hasco.model.ComponentInstance;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

public class EvalReport implements Comparable<EvalReport> {

    private Optional<Double> score;

    private List<ComponentInstance> witnesses;

    private List<Optional<Double>> witnessScore;

    public EvalReport(List<ComponentInstance> witnesses,
                      List<Optional<Double>> witnessScore) {
        this.witnesses = witnesses;
        this.witnessScore = witnessScore;
        recalculateScore();
    }

    public void recalculateScore() {
        if(witnesses.size() < witnessScore.size()) {
            throw new IllegalStateException("Less witnesses than scores.");
        }
        if(witnesses.size() > witnessScore.size()) {
            throw new IllegalStateException("Less scores than witnesses.");
        }
        OptionalDouble averageScore = witnessScore.stream()
                .filter(Optional::isPresent)
                .mapToDouble(Optional::get)
                .min();
        score = Optional.ofNullable(
                    averageScore.isPresent() ?
                        averageScore.getAsDouble() :
                        null);
    }

    public Optional<Double> getScore() {
        return score;
    }

    public List<ComponentInstance> getWitnesses() {
        return witnesses;
    }

    public List<Optional<Double>> getWitnessScores() {
        return witnessScore;
    }

    @Override
    public int compareTo(EvalReport o) {
        Optional<Double> r1 = this.getScore();
        Optional<Double> r2 = o.getScore();

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

    public void enterResults(List<ComponentInstance> witnesses,
                             List<Optional<Double>> witnessScore) {
        this.witnesses.addAll(witnesses);
        this.witnessScore.addAll(witnessScore);
        recalculateScore();
    }
}
