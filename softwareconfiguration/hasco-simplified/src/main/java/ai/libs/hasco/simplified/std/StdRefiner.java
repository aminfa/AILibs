package ai.libs.hasco.simplified.std;

import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.simplified.ComponentRefiner;
import ai.libs.hasco.simplified.ComponentRegistry;
import ai.libs.hasco.simplified.RefinementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * This class is there to decide what refinement operation should be done.
 * It differentiates between phase 1 and phase 2 component instances:
 *  - Phase 2 component instances have a explicitly ordered list of parameters.
 *      This refiner simply chooses parameters based on round robin.
 *
 *  - Phase 1 component instances are searched for required interfaces that are not yet set.
 *      This search is done in a BFS manner, and the result is refined with component instances that are providers.
 *      If all required interfaces are set it will instead create a phase 2 component instance.
 *
 */
@Component
public class StdRefiner implements ComponentRefiner {

    private final static Logger logger = LoggerFactory.getLogger(StdRefiner.class);

    private final RefinementService refService;

    public StdRefiner(ComponentRegistry registry) {
        this.refService = new StdRefinementService(registry);
    }

    @Autowired
    public StdRefiner(@Qualifier("stdRefinementService") RefinementService refService) {
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
            if(logger.isTraceEnabled())
                logger.debug("Refining parameter {} of component {} at the path: {}",
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
            logger.info("{}: yielded {} many refinements.",
                    process.displayText(), refinements.size());
        }
        boolean nonPhaseTwoRefinementsPresent = refinements
                .stream()
                .anyMatch(r -> !(r instanceof CIPhase2));
        if(nonPhaseTwoRefinementsPresent) {
            logger.error("Parameter refinements done by the agent contains non-Phase 2 roots.");
            return Collections.emptyList();
        }
        /*
         * Filter all refinements that invalidate parameter dependencies.
         * Propagate remaining parameter dependencies:
         */
        List<ComponentInstance> inValidComponentInstances = new ArrayList<>();
        for (ComponentInstance refinement : refinements) {
            CIPhase2 ciPhase2 = (CIPhase2) refinement;
            DependencyPropagator propagator = new DependencyPropagator(ciPhase2);
            propagator.propagateDependencies();
            if(propagator.hasUnsatisfiableDependencies()) {
                inValidComponentInstances.add(refinement);
                if(logger.isDebugEnabled())
                    logger.debug("Dependencies of refinement of {} are not satisfiable. " +
                            "\nComponent {} with param values: {}\n has unsatisfiable param dependencies: {}", ciPhase2,
                            propagator.getInstance().getComponent().getName(), propagator.getInstance().getParameterValues(),
                            propagator.getUnsatisfiableDependencies());
            } else {
                propagator.propagateNewParameterValues();
            }
        }
        if(!inValidComponentInstances.isEmpty()) {
            logger.info("Removed {} many refinements because they violate parameter dependencies.", inValidComponentInstances.size());
            refinements.removeAll(inValidComponentInstances);
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
            return Collections.singletonList(phase2);
        }
        String interfaceName = path.remove(path.size() - 1);
        if(logger.isTraceEnabled())
            logger.trace("Refining root {} - refining required inteface `{}` of component along path: {}.",
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
        } else {
            logger.info("{}: yielded {} many refinements.",
                    process.displayText(), refinements.size());
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

        for(ComponentInstance cursor : ComponentIterator.bfs(root)) {
            Map<String, ComponentInstance> providedIs = cursor.getSatisfactionOfRequiredInterfaces();
            Map<String, String> requiredIs = cursor.getComponent().getRequiredInterfaces();
            /*
             * Iterate over all required intefaces of the cursor
             */
            for (String interfaceName : requiredIs.keySet()) {
                if (!providedIs.containsKey(interfaceName) ||
                        providedIs.get(interfaceName) == null) {
                            // found a required interface, that hasn't been satisfied yet.
                    if(cursor instanceof CIIndexed) {
                        List<String> path = new ArrayList<>(Arrays.asList(((CIIndexed) cursor).getPath()));
                        path.add(interfaceName);
                        return path;
                    } else {
                        throw new IllegalStateException("Component type is not recognized: " + cursor);
                    }
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
