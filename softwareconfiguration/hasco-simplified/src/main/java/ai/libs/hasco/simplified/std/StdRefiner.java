package ai.libs.hasco.simplified.std;

import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.simplified.ComponentRefiner;
import ai.libs.hasco.simplified.ComponentRegistry;
import ai.libs.hasco.simplified.RefinementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Supplier;

import static ai.libs.hasco.simplified.std.ImplUtil.decideBetween;

/**
 * This class is there to decide what refinement operation should be done.
 * It differentiates between phase 1 and phase 2 component instances:
 *  -   Phase 2 component instances have a explicitly ordered list of parameters.
 *      This refiner simply chooses parameters based on round robin.
 *
 *  -   Phase 1 component instances are searched for required interfaces that are not yet set.
 *      This search is done in a BFS manner and the result is refined with component instances that are providers.
 *      If all required interfaces are set it will instead create a phase 2 component instance.
 *
 */
public class StdRefiner implements ComponentRefiner {

    private final static Logger logger = LoggerFactory.getLogger(StdRefiner.class);

    private final RefinementService refService;

    public StdRefiner(ComponentRegistry registry) {
        this.refService = new StdRefinementService(registry);
    }

    // Used for testing
    public StdRefiner(RefinementService refService) {
        this.refService = refService;
    }

    @Override
    public List<ComponentInstance> refine(ComponentInstance componentInstance) {
        if(componentInstance instanceof CIPhase1) {
            return refineComponents((CIPhase1) componentInstance);
        } else if(componentInstance instanceof CIPhase2) {
            return refineParameters((CIPhase2) componentInstance);
        } else {
            logger.error("Component instance type not recognized: {}" +
                    "\n {}",componentInstance.getClass().getName(), componentInstance);
            return Collections.emptyList();
        }
    }

    private List<ComponentInstance> refineParameters(CIPhase2 componentInstance) {
        boolean done = false;
        RefinementProcess process = new RefinementProcess(componentInstance);
        for(ParamRefinementRecord paramRefinement : componentInstance) {
            ComponentInstance trg = componentInstance.getComponentByPath(paramRefinement.getComponentPath());
            process.setComponentToBeRefined(trg);
            logger.debug("Refining parameter {} of component {} at path: {}",
                    paramRefinement.getParamName(),
                    trg.getComponent().getName(),
                    Arrays.toString(paramRefinement.getComponentPath()));

            done =  process.refineParameter(paramRefinement.getParamName(),
                    refService);

            if(done) {
                break;
            } else {
                logger.trace("The refinement of component {} continues. (Refinement service said so)", componentInstance.displayText());
            }
        }
        List<ComponentInstance> refinements = process.getRefinements();
        if(refinements.isEmpty()) {
            logger.info("Component {} wasn't refined any further and will be dropped.", componentInstance.displayText());
        } else if(!done) {
            logger.warn("The refinement process for component {} did not finish," +
                    " however refinements were created.\nHave a look at the refinement service used: {}", componentInstance.displayText(), refService.getClass().getSimpleName());
        } else {
            logger.info("Refinement of component {} yielded {} many refinements.",
                    componentInstance.displayText(), refinements.size());
        }
        boolean nonPhaseTwoRefinementsPresent = refinements
                .stream()
                .anyMatch(r -> !(r instanceof CIPhase2));
        if(nonPhaseTwoRefinementsPresent) {
            logger.error("Parameter refinements done by the agent contains non-Phase 2 roots.");
            return Collections.emptyList();
        }
        return refinements;
    }


    private List<ComponentInstance> refineComponents(CIPhase1 base) {
        List<String> path = BFSUnSatInterface(base); // The last element is the interface name
        if(path.isEmpty()) {
            /*
             * All the required interfaces of all component instances have been satisfied.
             */
            CIPhase2 phase2 = new CIPhase2(base);
            logger.info("All required interfaces of {} has been set." +
                    " It will go over into phase two: {}",
                    base.displayText(),
                    phase2.displayText());
            return refineParameters(phase2);
        }
        String interfaceName = path.remove(path.size() - 1);
        logger.debug("Refining root {} - refining required inteface `{}` of component along path: {}.",
                base.displayText(), interfaceName, path);
        ComponentInstance trg = base.getComponentByPath(path);
        RefinementProcess process = new RefinementProcess(base);
        process.setComponentToBeRefined(trg);
        boolean done = process.refineRequiredInterface(interfaceName, refService);
        List<ComponentInstance> refinements = new ArrayList<>(process.getRefinements());
        if(!done) {
            logger.error("Refinement service returned that is not done with component {}", base.displayText());
        }
        if(refinements.isEmpty()) {
            logger.info("No refinements made to {}, dropping it", base.displayText());
            return refinements;
        }
        CIPhase2 phase2 = new CIPhase2(base);
        logger.debug("Adding a Phase2 (param refinement) as a child: {}", phase2.displayText());
        refinements.add(phase2);
        return refinements;
    }

    // package private for testing purposes.

    /**
     * Searches the given component and all connected components for unsatisfied required interfaces.
     * @param root The component instance to be searched from.
     * @return the path to the required interface. If empty, all the required intefaces of all components have been satisfied.
     */
    static List<String> BFSUnSatInterface(CIIndexed root) {
        Set<Integer> visitedNodes = new HashSet<>();
        Deque<CIIndexed> queue = new LinkedList<>();
        queue.add(root);

        while(!queue.isEmpty()) {
            CIIndexed cursor = queue.pop();
            /*
             * Check if the node has been visited yet, and if not mark it.
             */
            if(visitedNodes.contains(cursor.getIndex())) {
                continue;
            } else  {
                visitedNodes.add(cursor.getIndex());
            }
            Map<String, ComponentInstance> providedIs = cursor.getSatisfactionOfRequiredInterfaces();
            Map<String, String> requiredIs = cursor.getComponent().getRequiredInterfaces();
            /*
             * Iterate over all required intefaces of the cursor
             */
            for (String interfaceName : requiredIs.keySet()) {
                if(providedIs.containsKey(interfaceName) &&
                        providedIs.get(interfaceName) != null) {
                    /*
                     * The required interface has been satisfied by a component instance.
                     * Add the node to the queue:
                     */
                    queue.add((CIIndexed) providedIs.get(interfaceName));
                } else {
                    // found a required interface, that hasn't been satisfied yet.
                    List<String> path = new ArrayList<>(Arrays.asList(cursor.getPath()));
                    path.add(interfaceName);
                    return path;
                }
            }
        }
        /*
         * All required interfaces have been satisfied.
         * The empty list that signals this state:
         */
        return Collections.emptyList();
    }


}