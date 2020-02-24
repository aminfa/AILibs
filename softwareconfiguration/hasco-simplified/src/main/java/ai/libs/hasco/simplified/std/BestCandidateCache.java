package ai.libs.hasco.simplified.std;

import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.simplified.ClosedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class BestCandidateCache implements ClosedList {

    private final static Logger logger = LoggerFactory.getLogger(BestCandidateCache.class);

    private double bestScore = Double.MAX_VALUE;

    private ComponentInstance root = null;

    private ComponentInstance bestCandidate = null;

    private boolean newCandidateFound = false;

    private ClosedList innerList;

    @Autowired
    public BestCandidateCache(StdCandidateContainer stdCandidateContainer) {
        this.innerList = stdCandidateContainer;
    }

    public ComponentInstance getBestCandidateRefinement() {
        return root;
    }

    public boolean hasNewBestCandidate() {
        return newCandidateFound;
    }

    public synchronized boolean checkBestCandidateAndUpdate() {
        boolean oldVal = newCandidateFound;
        newCandidateFound = false;
        return oldVal;
    }

    public Optional<ComponentInstance> getBestSeenCandidate() {
        return Optional.ofNullable(bestCandidate);
    }

    public Optional<Double> getBestSeenScore() {
        if(getBestSeenCandidate().isPresent())
            return Optional.of(bestScore);
        else
            return Optional.empty();
    }

    @Override
    public boolean isClosed(ComponentInstance candidate) {
        return innerList.isClosed(candidate);
    }

    private synchronized void checkAndUpdateCandidate(ComponentInstance root,
                                                      ComponentInstance candidate,
                                                      double score) {
        if(bestScore > score) {
            logger.info("A new best candidate found, score: {}", score);
            this.root = root;
            this.bestCandidate = candidate;
            this.bestScore = score;
            newCandidateFound = true;
        }
    }


    @Override
    public void close(ComponentInstance componentInstance,
                      List<ComponentInstance> witnesses,
                      List<Optional<Double>> results) {
        innerList.close(componentInstance, witnesses, results);
        for (int i = 0; i < witnesses.size(); i++) {
            Optional<Double> optScore = results.get(i);
            if(!optScore.isPresent()) {
                continue;
            }
            Double score = optScore.get();
            ComponentInstance candidate = witnesses.get(i);
            checkAndUpdateCandidate(componentInstance, candidate, score);
        }
    }

}
