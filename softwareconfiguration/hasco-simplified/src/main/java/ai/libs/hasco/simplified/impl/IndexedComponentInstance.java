package ai.libs.hasco.simplified.impl;

import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.ComponentInstance;

import java.util.Map;

public class IndexedComponentInstance extends ComponentInstance {

    private final Integer index;

    /**
     * Constructor for creating a <code>ComponentInstance</code> for a particular <code>Component</code>.
     *  @param component                        The component that is grounded.
     * @param parameterValues                  A map containing the parameter values of this grounding.
     * @param satisfactionOfRequiredInterfaces
     * @param componentIndex
     */
    public IndexedComponentInstance(Component component, Map<String, String> parameterValues, Map<String, ComponentInstance> satisfactionOfRequiredInterfaces, Integer componentIndex) {
        super(component, parameterValues, satisfactionOfRequiredInterfaces);
        this.index = componentIndex;
    }
}
