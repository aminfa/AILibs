package ai.libs.hasco.simplified.impl;

import ai.libs.hasco.model.CategoricalParameterDomain;
import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.model.NumericParameterDomain;
import ai.libs.hasco.simplified.ComponentRegistry;

import java.util.List;

public class StdRefinementAgent implements RefinementAgent {

    private final ComponentRegistry registry;

    public StdRefinementAgent(ComponentRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean refineCategoricalParameter(List<ComponentInstance> refinements, ComponentInstance component, CategoricalParameterDomain domain, String currentValue) {
        return false;
    }

    @Override
    public boolean refineNumericalParameter(List<ComponentInstance> refinements, ComponentInstance component, NumericParameterDomain domain, String currentValue) {
        return false;
    }

    @Override
    public boolean refineRequiredInterface(List<ComponentInstance> refinements, ComponentInstance component, String requiredInterface) {
        return false;
    }
}
