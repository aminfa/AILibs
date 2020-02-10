package ai.libs.hasco.simplified.std;

import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.model.IParameterDomain;
import ai.libs.hasco.model.Parameter;
import ai.libs.jaicore.basic.sets.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class CIRoot extends CIIndexed implements Comparable<CIRoot> {


    private final static Logger logger = LoggerFactory.getLogger(CIRoot.class);

    private EvalReport evalReport = null;

    public CIRoot(Component component,
                  Map<String, String> parameterValues,
                  Map<String, ComponentInstance> satisfactionOfRequiredInterfaces,
                  Integer componentIndex) {
        super(component, parameterValues, satisfactionOfRequiredInterfaces, componentIndex);
    }

    public void copyWitnessEvaluations(CIRoot parentRoot) {
        if(!parentRoot.hasBeenEvaluated()) {
            return;
        }
        EvalReport parentReport = parentRoot.getEvalReport();
        List<ComponentInstance> witnesses = new ArrayList<>();
        List<Optional<Double>> witnessScores = new ArrayList<>();

        for (int i = 0; i < parentReport.getWitnesses().size(); i++) {
            ComponentInstance witness = parentReport.getWitnesses().get(i);
            if(isWitness(witness)) {
                witnesses.add(witness);
                witnessScores.add(parentReport.getWitnessScores().get(i));
            }
        }
        if(!witnesses.isEmpty()) {
            logger.debug("Refinement {} reused {} many evaluations from parent.", displayText(), witnesses.size());
            EvalReport newReport = new EvalReport(witnesses, witnessScores);
            setEvalReport(newReport);
        } else {
            logger.debug("Refinement {} couldn't resue any previous evaluation.", displayText());
        }
    }

    private boolean isWitness(ComponentInstance witnessRoot) {
        for (Pair<ComponentInstance, Optional<ComponentInstance>> instanceWitnessPair :
                ComponentIterator.bfsPair(this, witnessRoot)) {
            /*
             * Check if the component exists in the witness:
             */
            if(!instanceWitnessPair.getY().isPresent()) {
                return false;
            }
            ComponentInstance instance = instanceWitnessPair.getX();
            ComponentInstance witness = instanceWitnessPair.getY().get();
            /*
             * Check if the witness is the same component:
             */
            if(!instance.getComponent().getName().equals(witness.getComponent().getName())) {
                return false;
            }
            /*
             * Check if parameter values are a sub-set:
             */
            for (Map.Entry<String, String> paramEntry : instance.getParameterValues().entrySet()) {
                String paramName = paramEntry.getKey();
                String paramValue = paramEntry.getValue();
                String witnessParamValue = witness.getParameterValue(paramName);
                Parameter paramType = instance.getComponent().getParameterWithName(paramName);
                IParameterDomain domain = paramType.getDefaultDomain();
                IParameterDomain instanceParamDom = DomainHandler.strToParamDomain(domain, paramValue);
                IParameterDomain witnessParamDom = DomainHandler.strToParamDomain(domain, witnessParamValue);
                if(!instanceParamDom.subsumes(witnessParamDom)){
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Only to be used in a root node
     */
    protected void performDeepCopy() {
        Deque<CIIndexed> copyWaitingList = new LinkedList<>();
        Map<Integer, CIIndexed> allCopies = new HashMap<>();
        allCopies.put(getIndex(), this);
        copyWaitingList.add(this);
        while(!copyWaitingList.isEmpty()) {
            CIIndexed next = copyWaitingList.pop();
            /*
             * Replace all satisfying inner component instances with a copy of them:
             */
            Map<String, ComponentInstance> satMap = next.getSatisfactionOfRequiredInterfaces();
            for(String requiredInterface : satMap.keySet()) {
                ComponentInstance componentInstance = satMap.get(requiredInterface);
                Objects.requireNonNull(componentInstance,
                        String.format("Component of required interface %s was null.", requiredInterface));
                CIIndexed copiedInstance;
                if(!(componentInstance instanceof CIIndexed)) {
                    throw new IllegalStateException("A component instance has an unrecognized type: "
                            + componentInstance.getComponent().getName()
                            + ". type: " + componentInstance.getClass().getName());
                }
                Integer index = ((CIIndexed) componentInstance).getIndex();
                copiedInstance = allCopies.get(index);
                if(copiedInstance == null) {
                    copiedInstance = new CIIndexed(componentInstance.getComponent(),
                            new HashMap<>(componentInstance.getParameterValues()),
                            new HashMap<>(componentInstance.getSatisfactionOfRequiredInterfaces()),
                            index,
                            ((CIIndexed) componentInstance).getPath());
                    allCopies.put(index, copiedInstance);
                    copyWaitingList.add(copiedInstance);
                }
                satMap.put(requiredInterface, copiedInstance);
            }
        }
    }

    public ComponentInstance getComponentByPath(String[] pathArr) {
        return getComponentByPath(Arrays.stream(pathArr).iterator());
    }

    public ComponentInstance getComponentByPath(Iterable<String> componentPath) {
        return getComponentByPath(componentPath.iterator());
    }

    public ComponentInstance getComponentByPath(Iterator<String> componentPath) {
        if(componentPath == null) {
            return this;
        }
        ComponentInstance current = this, prev = null;
        while(componentPath.hasNext()) {
            String requiredInterfaceName = componentPath.next();
            prev = current;
            current = getSatisfyingComponent(prev, requiredInterfaceName);
            if(current == null) {
                if(prev.getComponent().getRequiredInterfaces().containsKey(requiredInterfaceName))
                    throw new IllegalArgumentException("ComponentInstance with path" + componentPath + " not found.\n" +
                            "Component Instance " + prev.getComponent().getName()
                            + " doesn't have the required interface called: " + requiredInterfaceName +
                            " satisfied.");
                else
                    throw new IllegalArgumentException("ComponentInstance with path" + componentPath + " not found.\n" +
                            "Component Instance " + prev.getComponent().getName()
                            + " doesn't have a required interface called: " + requiredInterfaceName);
            }
        }
        return current;
    }

//    public ComponentInstance getComponentByPath(String[] path) {
//        Map<String, ComponentInstance> providers = getSatisfactionOfRequiredInterfaces();
//        ComponentInstance result = this;
//        for (int i = 0; i < path.length; i++) {
//            String interfaceName = path[i];
//            ComponentInstance componentInstance = providers.get(interfaceName);
//            if(componentInstance == null) {
//                throw new IllegalArgumentException("Couldn't find component with path: " + Arrays.toString(path) +
//                        ". reached " + i);
//            }
//            providers = componentInstance.getSatisfactionOfRequiredInterfaces();
//            result = componentInstance;
//        }
//        return result;
//    }


    private ComponentInstance getSatisfyingComponent(ComponentInstance base, String requiredInterfaceName) {
        ComponentInstance component = base.getSatisfactionOfRequiredInterfaces().get(requiredInterfaceName);
        return component;
    }

    public boolean hasBeenEvaluated() {
        return evalReport != null;
    }

    public EvalReport getEvalReport() {
        return evalReport;
    }

    public void setEvalReport(EvalReport evalReport) {
        this.evalReport = evalReport;
    }

    @Override
    public int compareTo(final CIRoot o) {
        if(o == null) {
            return -1;
        }
        if(hasBeenEvaluated()) {
            if(o.hasBeenEvaluated()) {
                EvalReport thisEval = this.getEvalReport();
                EvalReport otherEval = o.getEvalReport();
                return thisEval.compareTo(otherEval);
            } else {
                // r1 has result but r2 doesn't.
                return -1;
            }
        } else {
            if(o.hasBeenEvaluated()) {
                // r2 has result but r1 doesn't.
                return 1;
            } else {
                // Both results are not present
                return 0;
            }
        }
    }

}
