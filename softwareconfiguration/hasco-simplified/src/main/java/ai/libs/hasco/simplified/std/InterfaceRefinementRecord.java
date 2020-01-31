package ai.libs.hasco.simplified.std;

import java.util.Arrays;

/**
 * Objects of this type record a single refinement to a component.
 * The refinement can be either a change to a component parameter value,
 * or the fulfillment of a required interface  by the component.
 *
 */
public class InterfaceRefinementRecord {

    /**
     * This array contains the path to the property that was refined.
     * The property can be either a parameter or a required interface.
     *
     * This array is never empty or null.
     * An array was used to save memory: reuse the String values and only reference it through the array.
     */
    private final String[] interfaceRefinementPath;

    private InterfaceRefinementRecord(String[] interfaceRefinementPath) {
        this.interfaceRefinementPath = interfaceRefinementPath;
    }

    public static InterfaceRefinementRecord refineRequiredInterface(String[] componentPath) {
        return new InterfaceRefinementRecord(componentPath);
    }

    public static InterfaceRefinementRecord refineRequiredInterface(String[] parentComponentPath, String requiredInterfaceName) {
        final int componentDepth = parentComponentPath.length;
        final String[] componentPath = Arrays.copyOf(parentComponentPath, componentDepth + 1);
        componentPath[componentDepth] = requiredInterfaceName;
        return new InterfaceRefinementRecord(componentPath);
    }

    public String[] getInterfaceRefinementPath() {
        return interfaceRefinementPath;
    }

    public String toString() {
        return String.format("refinement record: %s", Arrays.toString(interfaceRefinementPath));
    }
}
