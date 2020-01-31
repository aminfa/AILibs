package ai.libs.hasco.simplified.std;

import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.simplified.ComponentRefiner;
import ai.libs.hasco.simplified.ComponentRegistry;
import ai.libs.hasco.simplified.RefinementProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Supplier;

import static ai.libs.hasco.simplified.std.ImplUtil.decideBetween;

public class StdRefiner implements ComponentRefiner {

    private final static Logger logger = LoggerFactory.getLogger(StdRefiner.class);

    private final ComponentRegistry registry;

    private final StdRefinementAgent agent;

    private final Supplier<Double> refinementNumberGenerator;

    public StdRefiner(ComponentRegistry registry, Supplier<Double> refinementNumberGenerator) {
        this.registry = registry;
        this.refinementNumberGenerator = refinementNumberGenerator;
        this.agent = new StdRefinementAgent(registry);
    }

    public StdRefiner(ComponentRegistry registry) {
        this(registry, new Random()::nextDouble);
    }

    public StdRefiner(ComponentRegistry registry, Long seed) {
        this(registry, new Random(seed)::nextDouble);
    }

    @Override
    public List<ComponentInstance> refine(ComponentInstance componentInstance) {
        /*
         * First make a coin-flip if parameters are set.
         * Or required instances are satisfied.
         */
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
        int startingIndex = componentInstance.getParamIndex();
        while(!done) {
            ParamRefinementRecord paramRefinement = componentInstance.getNextParamToBeRefined();
            process.setComponentToBeRefined(componentInstance.getComponentByPath(paramRefinement.getComponentPath()));
            done =  process.refineParameter(paramRefinement.getParamName(), agent);
            componentInstance.nextParameter();
            if(!done && startingIndex == componentInstance.getParamIndex()) {
                return Collections.emptyList();
            }
        }
        boolean nonPhaseTwoRefinementsPresent = process.getRefinements().stream()
                .anyMatch(r -> !(r instanceof CIPhase2));
        if(nonPhaseTwoRefinementsPresent) {
            logger.error("Parameter refinements done by the agent did contain non Phase 2 bases.");
            return Collections.emptyList();
        }
        return process.getRefinements();
    }


    private List<ComponentInstance> refineComponents(CIPhase1 base) {
        List<String> path = dfsUnprovidedInterfaces(base);
        List<ComponentInstance> refinements = new ArrayList<>();
        if(path.isEmpty()) {
            CIPhase2 phase2 = new CIPhase2(base);
            refinements.add(phase2);
            return refinements;
        }
        String[] refinedComponentPath = path.toArray(new String[0]);
        String interfaceName = path.remove(path.size() - 1);
        String[] pathArr = path.toArray(new String[0]);
        ComponentInstance componentToBeRefined = base.getComponentByPath(pathArr);
        String requiredInterface = componentToBeRefined.getComponent().getRequiredInterfaces().get(interfaceName);
        if(requiredInterface == null) {
            return Collections.emptyList();
        }
        List<Component> providersOf = registry.getProvidersOf(requiredInterface);
        if(providersOf.isEmpty()) {
            return Collections.emptyList();
        }
        for (Component provider : providersOf) {
            CIPhase1 newBase = new CIPhase1(base);
            Integer index = newBase.allocateNextIndex();
            componentToBeRefined = newBase.getComponentByPath(pathArr);
            Map<String, ComponentInstance> satIMap = componentToBeRefined.getSatisfactionOfRequiredInterfaces();
            CIIndexed newRefinement = new CIIndexed(provider, new HashMap<>(), new HashMap<>(), index, refinedComponentPath);
            satIMap.put(interfaceName, newRefinement);
            InterfaceRefinementRecord refinementRecord = InterfaceRefinementRecord.refineRequiredInterface(refinedComponentPath);
            newBase.addRecord(refinementRecord);

            refinements.add(newBase);
        }
        CIPhase2 phase2 = new CIPhase2(base);
        refinements.add(phase2);
        return refinements;
    }

    private List<String> dfsUnprovidedInterfaces(CIIndexed cursor) {
        Map<String, ComponentInstance> providedIs = cursor.getSatisfactionOfRequiredInterfaces();
        Map<String, String> requiredIs = cursor.getComponent().getRequiredInterfaces();
        for (String interfaceName : requiredIs.keySet()) {
            if(providedIs.containsKey(interfaceName) &&
            providedIs.get(interfaceName) != null) {
                continue;
            }
            // found a required interface:
            List<String> path = new ArrayList<>(Arrays.asList(cursor.getPath()));
            path.add(interfaceName);
            return path;
        }
        // all interfaces are fullfilled:
        for (ComponentInstance provider : providedIs.values()) {
            List<String> path = dfsUnprovidedInterfaces((CIIndexed) provider);
            if(!path.isEmpty()) {
                return path;
            }
        }
        return Collections.emptyList();
    }


}
