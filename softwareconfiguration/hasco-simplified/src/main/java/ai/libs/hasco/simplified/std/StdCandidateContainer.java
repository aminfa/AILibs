package ai.libs.hasco.simplified.std;

import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.simplified.ClosedList;
import ai.libs.hasco.simplified.OpenList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class StdCandidateContainer implements ClosedList, OpenList {

    private final PriorityQueue<ComponentInstance> queue;

    private final EvaluationCache evaluationCache;

    private final static Logger logger = LoggerFactory.getLogger(StdCandidateContainer.class);

    public StdCandidateContainer() {
        this.evaluationCache = new EvaluationCache();
        this.queue = new PriorityQueue<>(evaluationCache);
    }

    @Override
    public boolean isClosed(ComponentInstance candidate) {
        return evaluationCache.containsResult(candidate);
    }

    private void enterResult(ComponentInstance candidate, Optional<Double> result) {
        if(result.isPresent()) {
            evaluationCache.insertEvalResult(candidate, result.get());
        } else {
            evaluationCache.insertEvalError(candidate);
        }
    }

    @Override
    public void insert(ComponentInstance candidate,
                       List<ComponentInstance> witnesses, List<Optional<Double>> results) {

        OptionalDouble averageScore = results.stream()
                .filter(Optional::isPresent)
                .mapToDouble(Optional::get)
                .average();

        if(averageScore.isPresent()) {
            evaluationCache.insertEvalResult(candidate, averageScore.getAsDouble());
        } else {
            evaluationCache.insertEvalError(candidate);
        }

        if(witnesses.size() != results.size()) {
            logger.warn("The witnesses and result lists have different sizes: {} ≠ {}",
                    witnesses.size(), results.size());
        }
        for (int i = 0; i < witnesses.size() && i < results.size(); i++) {
            enterResult(witnesses.get(i), results.get(i));
        }
        

        queue.add(candidate);
    }

    @Override
    public Optional<Optional<Double>> retrieve(ComponentInstance prototype, ComponentInstance sample) {
        return Optional.ofNullable(evaluationCache.retrieveResult(sample));
    }

    public void insertRoot(ComponentInstance root) {
        evaluationCache.insertEvalResult(root, 0.);
        queue.add(root);
    }

    @Override
    public synchronized boolean hasNext() {
        return !queue.isEmpty();
    }

    @Override
    public synchronized ComponentInstance popNext() {
        return queue.poll();
    }

    public PriorityQueue<ComponentInstance> getQueue() {
        return queue;
    }

    public int getCacheSize() {
        return evaluationCache.getSize();
    }
}
