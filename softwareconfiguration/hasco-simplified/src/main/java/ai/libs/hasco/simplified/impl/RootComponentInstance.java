package ai.libs.hasco.simplified.impl;

import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.ComponentInstance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class RootComponentInstance extends ComponentInstance {

    private List<ComponentRefinementRecord> refinementHistory;

    public RootComponentInstance(Component component,
                                 Map<String, String> parameterValues,
                                 Map<String, ComponentInstance> satisfactionOfRequiredInterfaces) {
        super(component, parameterValues, satisfactionOfRequiredInterfaces);
    }

    public RootComponentInstance(RootComponentInstance prevComponent, List<ComponentRefinementRecord> refinements) {
        super(prevComponent.getComponent(),
                new HashMap<>(prevComponent.getParameterValues()),
                new HashMap<>(prevComponent.getSatisfactionOfRequiredInterfaces()));
        refinementHistory = new ArrayList<>(prevComponent.getRefinementHistory());
    }

    public boolean refineParameter(List<String> componentPath, String propertyName, RefinementCallback refiner) {
        ComponentInstance refinedComponent = searchSatisfyingComponent(componentPath);
        
    }

    private ComponentInstance searchSatisfyingComponent(List<String> componentPath) {
        if(componentPath == null || componentPath.isEmpty()) {
            return this;
        }
        ComponentInstance trg = this, prev = this;

        for(String requiredInterfaceName : componentPath) {
            prev = trg;
            trg = getSatisfyingComponent(prev, requiredInterfaceName);
            if(trg == null) {
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
        return trg;
    }

    private ComponentInstance getSatisfyingComponent(ComponentInstance base, String requiredInterfaceName) {
        ComponentInstance component = base.getSatisfactionOfRequiredInterfaces().get(requiredInterfaceName);

        return component;
    }


    public List<ComponentRefinementRecord> getRefinementHistory() {
        return refinementHistory;
    }

}
