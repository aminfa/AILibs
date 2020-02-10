package ai.libs.hasco.simplified.std;

import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.simplified.ClosedList;
import ai.libs.hasco.simplified.OpenList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class StdCandidateContainer implements ClosedList, OpenList, Comparator<CIRoot> {

    private final PriorityQueue<CIRoot> queue;

    private final List<CIRoot> firstRoots = new ArrayList<>();

//    private final EvaluationCache evaluationCache;

    private final static Logger logger = LoggerFactory.getLogger(StdCandidateContainer.class);

    public StdCandidateContainer() {
//        this.evaluationCache = new EvaluationCache();
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


    @Override
    public void insert(ComponentInstance candidate,
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
