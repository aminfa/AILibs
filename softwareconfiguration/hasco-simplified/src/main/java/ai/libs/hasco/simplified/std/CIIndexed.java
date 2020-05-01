package ai.libs.hasco.simplified.std;

import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.ComponentInstance;

import java.util.*;

public class CIIndexed extends ComponentInstance {

    private final static String[] ROOT_PATH = new String[0];
    private final String[] path;

    private final Integer index;

    /**
     * Constructor for creating a <code>ComponentInstance</code> for a particular <code>Component</code>.
     *  @param component                        The component that is grounded.
     * @param parameterValues                  A map containing the parameter values of this grounding.
     * @param satisfactionOfRequiredInterfaces
     * @param componentIndex
     */
    public CIIndexed(Component component, Map<String, String> parameterValues, Map<String, ComponentInstance> satisfactionOfRequiredInterfaces, Integer componentIndex) {
        super(component, parameterValues, satisfactionOfRequiredInterfaces);
        this.index = componentIndex;
        this.path = ROOT_PATH;
    }

    public CIIndexed(Component component, Map<String, String> parameterValues, Map<String, ComponentInstance> satisfactionOfRequiredInterfaces, Integer index, String[] path) {
        super(component, parameterValues, satisfactionOfRequiredInterfaces);
        this.path = path;
        this.index = index;
    }

    public Integer getIndex() {
        return index;
    }

    public String[] getPath() {
        return path;
    }


    public String displayText() {
        return String.format("%s[id: %d] :: %s",
                Arrays.toString(path),
                index,
                getComponent().getName());
    }

    public String toString() {
        return getClass().getSimpleName() + " :: " + displayText();
    }

    /*
     * TODO move the following methods to a CIBase class which is a parent of the two phases
     */

}
