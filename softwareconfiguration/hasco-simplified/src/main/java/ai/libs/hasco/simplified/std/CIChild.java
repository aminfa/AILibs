package ai.libs.hasco.simplified.std;

import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.ComponentInstance;

import java.util.Map;

public class CIChild extends IndexedComponentInstance implements RoutableCI {

    private final String[] path;

    /**
     * Constructor for creating a <code>ComponentInstance</code> for a particular <code>Component</code>.
     *  @param component                        The component that is grounded.
     * @param parameterValues                  A map containing the parameter values of this grounding.
     * @param satisfactionOfRequiredInterfaces
     * @param componentIndex
     * @param path
     */
    public CIChild(Component component, Map<String, String> parameterValues, Map<String, ComponentInstance> satisfactionOfRequiredInterfaces, Integer componentIndex, String[] path) {
        super(component, parameterValues, satisfactionOfRequiredInterfaces, componentIndex);
        this.path = path;
    }

    public String[] getPath() {
        return path;
    }

}