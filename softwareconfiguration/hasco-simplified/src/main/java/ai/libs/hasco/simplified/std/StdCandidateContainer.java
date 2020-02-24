package ai.libs.hasco.simplified.std;

import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.simplified.ClosedList;
import ai.libs.hasco.simplified.OpenList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiConsumer;

import static org.slf4j.LoggerFactory.getLogger;

@org.springframework.stereotype.Component
public class StdCandidateContainer implements ClosedList, OpenList, Comparator<CIRoot> {

    private final static Logger logger = getLogger(StdCandidateContainer.class);

    private final PriorityQueue<CIRoot> queue;

    private final List<CIRoot> firstRoots = new ArrayList<>();

    private BiConsumer<ComponentInstance, Double> sampleConsumer = (componentInstance, aDouble) -> {};

    public StdCandidateContainer() {
        this.queue = new PriorityQueue<>(this);
    }

    @Override
    public boolean isClosed(ComponentInstance candidate) {
        return castToRoot(candidate).hasBeenEvaluated();
    }

    private CIRoot castToRoot(ComponentInstance candidate) {
        if(candidate instanceof CIRoot) {
            return ((CIRoot) candidate);
        } else {
            throw new RuntimeException("Candidate is not root: " + candidate);
        }
    }

    public void setSampleConsumer(BiConsumer<ComponentInstance, Double> sampleConsumer) {
        this.sampleConsumer = sampleConsumer;
    }

    @Override
    public void close(ComponentInstance candidate,
                      List<ComponentInstance> witnesses, List<Optional<Double>> results) {
        CIRoot root = castToRoot(candidate);
        EvalReport report;
        if(root.hasBeenEvaluated()) {
            report = root.getEvalReport();
            report.enterResults(witnesses, results);
        } else {
            report = new EvalReport(witnesses, results);
            root.setEvalReport(report);
        }
        queue.add(root);

        for (int i = 0; i < witnesses.size(); i++) {
            if(results.get(i).isPresent()) {
                sampleConsumer.accept(witnesses.get(i),
                        results.get(i).get());
            }
        }
    }

    public void insertRoot(ComponentInstance root) {
        firstRoots.add(castToRoot(root));
        queue.add(castToRoot(root));
    }

    @Override
    public synchronized boolean hasNext() {
        return !queue.isEmpty();
    }

    @Override
    public synchronized ComponentInstance popNext() {
        return queue.poll();
    }

    public PriorityQueue<CIRoot> getQueue() {
        return queue;
    }

    public int getCacheSize() {
        return -1; // TODO remove this method, as the cache is not used anymore.
    }

    @Override
    public int compare(CIRoot o1, CIRoot o2) {
        if(firstRoots.contains(o1)) {
            if(firstRoots.contains(o2)) {
                return 0;
            } else {
                return -1;
            }
        } else if(firstRoots.contains(o2)) {
            return 1;
        } else {
            return o1.compareTo(o2);
        }
    }
}
