package ai.libs.hasco.simplified.std;

import ai.libs.hasco.core.HASCOSolutionCandidate;
import ai.libs.hasco.core.RefinementConfiguredSoftwareConfigurationProblem;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.simplified.SimpleHASCO;
import ai.libs.jaicore.basic.IOwnerBasedAlgorithmConfig;
import ai.libs.jaicore.basic.algorithm.AAlgorithm;
import ai.libs.jaicore.basic.algorithm.AlgorithmFinishedEvent;
import ai.libs.jaicore.basic.algorithm.AlgorithmInitializedEvent;
import ai.libs.jaicore.basic.algorithm.EAlgorithmState;
import ai.libs.jaicore.graph.Graph;
import ai.libs.jaicore.graphvisualizer.events.graph.GraphEvent;
import ai.libs.jaicore.graphvisualizer.plugin.nodeinfo.NodeInfoGenerator;
import ai.libs.jaicore.planning.core.Action;
import ai.libs.jaicore.planning.core.interfaces.IEvaluatedGraphSearchBasedPlan;
import ai.libs.jaicore.search.model.other.SearchGraphPath;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.api4.java.algorithm.events.IAlgorithmEvent;
import org.api4.java.algorithm.events.result.ISolutionCandidateFoundEvent;
import org.api4.java.algorithm.exceptions.AlgorithmException;
import org.api4.java.algorithm.exceptions.AlgorithmExecutionCanceledException;
import org.api4.java.algorithm.exceptions.AlgorithmTimeoutedException;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;

public class SimpleHASCOAlgorithmView extends AAlgorithm<RefinementConfiguredSoftwareConfigurationProblem<Double>, HASCOSolutionCandidate<Double>> implements Consumer<GraphEvent> {

    private SimpleHASCO simpleHASCO;

    private BestCandidateCache candidateCache;

    private ConcurrentLinkedDeque<GraphEvent> events = new ConcurrentLinkedDeque<>();

    public SimpleHASCOAlgorithmView(Optional<IOwnerBasedAlgorithmConfig> config,
                                    RefinementConfiguredSoftwareConfigurationProblem<Double> input,
                                    SimpleHASCO simpleHASCO, BestCandidateCache cache) {
        super(config.orElse(null), input);
        this.simpleHASCO = simpleHASCO;
        this.candidateCache = cache;
    }

    @Override
    public IAlgorithmEvent nextWithException() throws InterruptedException, AlgorithmExecutionCanceledException, AlgorithmTimeoutedException, AlgorithmException {
        try {
            this.registerActiveThread();
            switch (this.getState()) {
                case CREATED:
                    AlgorithmInitializedEvent initEvent = this.activate();
                    return initEvent;
                case ACTIVE:
                    while(events.isEmpty()) {
                        boolean hasNext = simpleHASCO.step();
                        if(!hasNext) {
                            return this.terminate();
                        }
                    }
                    GraphEvent graphEvent = events.removeFirst();
                    post(graphEvent);
                    return graphEvent;
                default:
                    throw new IllegalStateException("BestFirst search is in state " + this.getState() + " in which next must not be called!");
            }
        } finally {
            this.unregisterActiveThread();
        }
    }

    @Override
    public HASCOSolutionCandidate<Double> call() throws InterruptedException, AlgorithmExecutionCanceledException, AlgorithmTimeoutedException, AlgorithmException {
        while (hasNext()) {
            next();
        }
        Optional<ComponentInstance> bestSeenCandidate = candidateCache.getBestSeenCandidate();
        if(!bestSeenCandidate.isPresent()) {
            throw new AlgorithmException("No candidate found..");
        }
        return new HASCOSolutionCandidate(bestSeenCandidate.get(), new IEvaluatedGraphSearchBasedPlan(){

            @Override
            public Comparable getScore() {
                return ((CIRoot)bestSeenCandidate.get()).getEvalReport().getScore().get();
            }

            @Override
            public List<Action> getActions() {
                return null;
            }

            @Override
            public SearchGraphPath getSearchGraphPath() {
                return null;
            }
        }, 3000);
    }

    @Override
    public String toString() {
        return "SimpleHASCO{std}";
    }

    @Override
    public void accept(GraphEvent graphEvent) {
        events.addLast(graphEvent);
    }

    public static class SimpleNodeInfoGenerator implements NodeInfoGenerator<CIRoot> {

        @Override
        public String generateInfoForNode(CIRoot node) {
            StringBuilder sb = new StringBuilder();
            sb.append("<h2>Phase: ");
            if(node instanceof CIPhase1) {
                sb.append("1");
            } else if(node instanceof CIPhase2) {
                sb.append("2");
            } else {
                sb.append("unknown(").append(node.getClass().getSimpleName()).append(")");
            }
            sb.append("</h2>");
            try {
                String evalInfo = generateEvalInfo(node);
                sb.append(evalInfo);
            } catch(Exception ex) {
                sb.append("Error generating eval info. <br>").append(ex.getMessage());
            }
            try {
                String refInfo = generateRefinementInfo(node);
                sb.append(refInfo);
            } catch(Exception ex) {
                sb.append("Error generating refinement info. <br>").append(ex.getMessage());
            }
            return sb.toString();
        }

        private String generateEvalInfo(CIRoot node) {
            StringBuilder sb = new StringBuilder();
            if (node.hasBeenEvaluated()) {
                sb.append("<h2>Evaluated:</h2>");
                EvalReport evalReport = node.getEvalReport();
                Optional<Double> score = evalReport.getScore();
                if (score.isPresent()) {
                    sb.append("score: ").append(String.format("%.2f", score.get()));
                } else {
                    sb.append("error");
                }
                List<ComponentInstance> witnesses = evalReport.getWitnesses();
                sb.append(" (based on ").append(witnesses.size()).append(" samples)");
                sb.append("<ul>");
                for (int i = 0; i < witnesses.size(); i++) {
                    sb.append("<li>");
                    Optional<Double> witnessScore = evalReport.getWitnessScores().get(i);
                    if (witnessScore.isPresent()) {
                        sb.append("score: ").append(String.format("%.2f", witnessScore.get()));
                    } else {
                        sb.append("error");
                    }
                    sb.append("</li>");
                }
                sb.append("</ul>");
            } else {
                sb.append("<h2>Not evaluated yet.</h2>");
            }
            return sb.toString();
        }

        private String generateRefinementInfo(CIRoot node) {
            StringBuilder sb = new StringBuilder();
            if (node instanceof CIPhase1) {
                List<InterfaceRefinementRecord> refinementHistory = ((CIPhase1) node).getRefinementHistory();
                if (!refinementHistory.isEmpty()) {
                    InterfaceRefinementRecord lastRefinement = refinementHistory.get(refinementHistory.size() - 1);
                    sb.append("<h2>Interface refinement</h2>");
                    sb.append("Satisfied interface path: ").append(Arrays.toString(lastRefinement.getInterfaceRefinementPath())).append("<br>");
                    sb.append("Satisfied with instance of component: ").append(node.getComponentByPath(lastRefinement.getInterfaceRefinementPath()).getComponent().getName());
                }
            } else if (node instanceof CIPhase2) {
                Optional<ParamRefinementRecord> previousRecord = ((CIPhase2) node).getPreviousRecord();
                if (previousRecord.isPresent()) {
                    sb.append("<h2>Parameter refinement</h2>");
                    String[] componentPath = previousRecord.get().getComponentPath();
                    String paramName = previousRecord.get().getParamName();
                    sb.append("Parameter interface path: ").append(Arrays.toString(componentPath)).append("<br>");
                    sb.append("Parameter: ").append(paramName).append("<br>");
                    String parameterValue = node.getComponentByPath(componentPath).getParameterValue(paramName);
                    sb.append("Parameter value: ").append(parameterValue).append("<br>");
                }
            }
            return sb.toString();
        }
    }

}
