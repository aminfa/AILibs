package ai.libs.hasco.simplified.impl;

import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.simplified.ClosedList;
import ai.libs.hasco.simplified.ComponentRegistry;
import ai.libs.hasco.simplified.EvaluationCache;
import ai.libs.hasco.simplified.OpenList;

import java.util.*;

public class BestFirstList implements ClosedList, OpenList {

    private final PriorityQueue<ComponentInstance> queue;

    private final EvaluationCache evaluationCache;

    public BestFirstList(ComponentRegistry registry) {
        this.evaluationCache = new EvaluationCache();
        this.queue = new PriorityQueue<>(evaluationCache);
    }

    @Override
    public boolean isClosed(ComponentInstance candidate) {
        return evaluationCache.containsResult(candidate);
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

        queue.add(candidate);
    }

    @Override
    public synchronized boolean hasNext() {
        return !queue.isEmpty();
    }

    @Override
    public synchronized ComponentInstance popNext() {
        return queue.poll();
    }

}
