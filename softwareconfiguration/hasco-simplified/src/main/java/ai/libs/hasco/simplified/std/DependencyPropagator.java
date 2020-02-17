package ai.libs.hasco.simplified.std;

import ai.libs.hasco.model.*;
import ai.libs.jaicore.basic.sets.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DependencyPropagator {

    private final static Logger logger = LoggerFactory.getLogger(DependencyPropagator.class);

    private final ComponentInstance instance;

    private List<Dependency> unsatisfiableDependencies;

    private Map<Parameter, IParameterDomain> paramValues;

    private String displayText;

    public DependencyPropagator(CIPhase2 root) {
        Optional<ParamRefinementRecord> previousRecord = root.getPreviousRecord();
        if(!previousRecord.isPresent()) {
            throw new IllegalArgumentException("No parameters are present: " + root.displayText());
        }
        ParamRefinementRecord record = previousRecord.get();
        instance = root.getComponentByPath(record.getComponentPath());
    }

    private String getDisplayText() {
        if(displayText == null) {
            displayText = String.format("Dependency propagation (::%s)", instance.getComponent().getName());
        }
        return displayText;
    }

    public boolean hasUnsatisfiableDependencies() {
        if(unsatisfiableDependencies == null) {
            return false;
        }
        return !unsatisfiableDependencies.isEmpty();
    }

    public ComponentInstance getInstance() {
        return instance;
    }

    public List<Dependency> getUnsatisfiableDependencies() {
        if(unsatisfiableDependencies == null) {
            unsatisfiableDependencies = new ArrayList<>();
        }
        return unsatisfiableDependencies;
    }

    private IParameterDomain getCurrentParamValue(String paramName) {
        Parameter param = instance.getComponent().getParameterWithName(paramName);
        if(param == null) {
            throw new IllegalStateException(getDisplayText() + ". Component doesn't define parameter: " + paramName);
        }
        return getCurrentParamValue(param);
    }

    private IParameterDomain getCurrentParamValue(Parameter param) {
        Map<Parameter, IParameterDomain> paramValues = getParamValues();
        if(paramValues.containsKey(param) && paramValues.get(param) != null) {
            return paramValues.get(param);
        }
        IParameterDomain valueDomain = ParamDomainHandler.strToParamDomain(param.getDefaultDomain(),
                instance.getParameterValue(param));
        paramValues.put(param, valueDomain);
        return valueDomain;
    }

    private void setParameterValue(String paramName, String newValue) {
        Parameter param = instance.getComponent().getParameterWithName(paramName);
        if(param  == null) {
            throw new IllegalStateException(getDisplayText() + ". Component doesn't define parameter: " + paramName);
        }
        IParameterDomain newValueDomain = ParamDomainHandler.strToParamDomain(param.getDefaultDomain(), newValue);
        setParameterValue(param, newValueDomain);
    }

    private void setParameterValue(Parameter param, IParameterDomain intersection) {
        getParamValues().put(param, intersection);
    }

    private Map<Parameter, IParameterDomain> getParamValues() {
        if(paramValues == null)
            paramValues = new HashMap<>();
        return paramValues;
    }

    public void propagateNewParameterValues() {
        if(paramValues == null) {
            return;
        }
        paramValues.forEach((param, newDomain) -> {
            String stringValue = ParamDomainHandler.getStringValue(newDomain);
            instance.getParameterValues().put(param.getName(), stringValue);
        });
    }

    public void propagateDependencies() {
        Collection<Dependency> dependencies = instance.getComponent().getDependencies();
        for (Dependency dependency : dependencies) {
            boolean premiseFulfilled = isPremiseFulfilled(dependency.getPremise());
            if(premiseFulfilled) {
                if(!isConclusionSatisfiable(dependency.getConclusion())) {
                    getUnsatisfiableDependencies().add(dependency);
                }
                if(hasUnsatisfiableDependencies()) {
                    continue;
                }
                for (Pair<Parameter, IParameterDomain> condition : dependency.getConclusion()) {
                    // propagate satisfiable
                    boolean condSat = isSatisfiable(condition.getX(), condition.getY());
                    if(condSat) {
                        propagateCondition(condition.getX(), condition.getY());
                    } else {
                        logger.error("{}: There seems to be a parameter dependency conflict: {}. \nParameter values:{}", getDisplayText(), dependencies, instance.getParameterValues());
                    }
                }
            }
        }
    }

    private void propagateCondition(Parameter param, IParameterDomain newDomain) {
        IParameterDomain value = getCurrentParamValue(param);
        IParameterDomain intersection = ParamDomainHandler.intersection(newDomain, value);
        setParameterValue(param, intersection);
    }

    private boolean isPremiseFulfilled(Collection<Collection<Pair<Parameter, IParameterDomain>>> disjunction) {
        for (Collection<Pair<Parameter, IParameterDomain>> conjunction : disjunction) {
            boolean conjTrue = true;
            for (Pair<Parameter, IParameterDomain> condition : conjunction) {
                if(!isFulfilled(condition.getX(), condition.getY())) {
                    conjTrue = false;
                    break;
                }
            }
            if(conjTrue) {
                return true;
            }
        }
        return false;
    }

    private boolean isConclusionSatisfiable(Collection<Pair<Parameter, IParameterDomain>> conj) {
        for (Pair<Parameter, IParameterDomain> condition : conj) {
            if(!isSatisfiable(condition.getX(), condition.getY())) {
                return false;
            }
        }
        return true;
    }

    private boolean isFulfilled(Parameter param, IParameterDomain requiredDom) {
        IParameterDomain value = getCurrentParamValue(param);
        return requiredDom.subsumes(value);
    }

    private boolean isSatisfiable(Parameter param, IParameterDomain requiredDom) {
        return ImplUtil.isSatisfiable(requiredDom, getCurrentParamValue(param));
    }

}
