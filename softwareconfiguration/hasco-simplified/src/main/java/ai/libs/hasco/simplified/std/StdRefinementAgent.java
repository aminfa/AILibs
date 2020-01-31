package ai.libs.hasco.simplified.std;

import ai.libs.hasco.model.CategoricalParameterDomain;
import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.model.NumericParameterDomain;
import ai.libs.hasco.simplified.ComponentRegistry;
import ai.libs.hasco.simplified.RefinementAgent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;

public class StdRefinementAgent implements RefinementAgent {

    private final ComponentRegistry registry;

    private final static int NUM_SPLITS = 2;
    private final static int MIN_RANGE_SIZE = 2;

    public StdRefinementAgent(ComponentRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean refineCategoricalParameter(List<ComponentInstance> refinements, ComponentInstance base, ComponentInstance component, CategoricalParameterDomain domain, String name, String currentValue) {
        if(!( component instanceof CIIndexed) ) {
            throw new IllegalArgumentException("Component is not indexed");
        }
        if(!(base instanceof CIPhase2)) {
            throw new IllegalArgumentException("Base component is not of phase 2 type: " + base.getClass().getName());
        }
        if(currentValue != null) {
            return false;
        }
        if(domain.getValues() == null || domain.getValues().length == 0) {
            return false;
        }
        for (String category : domain.getValues()) {
            CIPhase2 newBase = new CIPhase2((CIPhase2) base);
            ComponentInstance refinedInstance = newBase.getComponentByPath(((CIIndexed) component).getPath());
            refinedInstance.getParameterValues().put(name, category);
            refinements.add(newBase);
        }
        return true;
    }

    @Override
    public boolean refineNumericalParameter(List<ComponentInstance> refinements,
                                            ComponentInstance base, ComponentInstance component,
                                            NumericParameterDomain domain,  String name, String currentValue) {
        // TODO split this method up
        if(!( component instanceof CIIndexed) ) {
            throw new IllegalArgumentException("Component is not indexed");
        }
        if(!(base instanceof CIPhase2)) {
            throw new IllegalArgumentException("Base component is not of phase 2 type: " + base.getClass().getName());
        }
        double min;
        double max;
        if(currentValue == null) {
            min = domain.getMin();
            max = domain.getMax();
        } else {
            Matcher matcher = StdSampler.PATTERN_NUMERIC_RANGE.matcher(currentValue);
            if (matcher.matches()) {
                // we have a range as value:
                String newValue;
                min = Double.parseDouble(matcher.group("num1"));
                max = Double.parseDouble(matcher.group("num2"));
            } else {
                // assume value is set:
                return false;
            }
        }
        double diff = max - min;
        if(diff < 0) {
            return false;
        }
        List<String> newRanges = new ArrayList<>();
        if(diff < MIN_RANGE_SIZE) {
            if(domain.isInteger())
                newRanges.add(String.valueOf((int) ((max + min) / 2.)));
            else
                newRanges.add(String.valueOf((max + min) / 2.));
        }
        int splitCount = NUM_SPLITS;
        if(splitCount > (diff/MIN_RANGE_SIZE)) {
            splitCount = (int) (diff/MIN_RANGE_SIZE);
        }
        while(diff / splitCount < 1.) {
            splitCount--;
            if(splitCount == 0) {
                return false;
            }
        }
        double splitPart = diff / splitCount;
        for (int i = 0; i < splitCount; i++) {
            double newMin = i * splitPart;
            double newMax = (i +1) * splitPart;
            if( newMax - newMin < MIN_RANGE_SIZE) {
                newRanges.add(String.valueOf((int)newMin));
            } else {
                newRanges.add(String.format("[%f, %f]", newMin, newMax));
            }
        }
        if(newRanges.isEmpty()) {
            return false;
        }
        for (String newVal : newRanges) {
            CIPhase2 newBase = new CIPhase2((CIPhase2) base);
            ComponentInstance refinedInstance = newBase.getComponentByPath(((CIIndexed) component).getPath());
            refinedInstance.getParameterValues().put(name, newVal);
            refinements.add(newBase);
        }
        return true;
    }



    @Override
    public boolean refineRequiredInterface(List<ComponentInstance> refinements, ComponentInstance base, ComponentInstance component,  String name, String requiredInterface) {
        if(!( component instanceof CIIndexed) ) {
            throw new IllegalArgumentException("Component is not indexed");
        }
        if(!(base instanceof CIPhase1)) {
            throw new IllegalArgumentException("Base component is not of phase 1 type: " + base.getClass().getName());
        }
        List<Component> providersOf = registry.getProvidersOf(requiredInterface);
        if(providersOf.isEmpty()) {
            return false;
        }
        for (Component provider : providersOf) {
            CIPhase1 newBase = new CIPhase1((CIPhase1) base);
            Integer index = newBase.allocateNextIndex();


            String[] parentPath = ((CIIndexed) component).getPath();
            ComponentInstance parent = newBase.getComponentByPath(parentPath);

            InterfaceRefinementRecord refinementRecord =
                    InterfaceRefinementRecord.refineRequiredInterface(parentPath, name);
            CIIndexed newInstance = new CIIndexed(provider,
                    new HashMap<>(), new HashMap<>(),
                    index, refinementRecord.getInterfaceRefinementPath());
            parent.getSatisfactionOfRequiredInterfaces().put(name, newInstance);
            newBase.addRecord(refinementRecord);
            refinements.add(newBase);
        }
        return true;
    }
}
