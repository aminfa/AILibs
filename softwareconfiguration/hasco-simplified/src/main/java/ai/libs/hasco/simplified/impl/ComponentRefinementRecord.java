package ai.libs.hasco.simplified.impl;

import java.util.Arrays;

/**
 * Objects of this type record a single refinement to a component.
 * The refinement can be either a change to a component parameter value,
 * or the fulfillment of a required interface  by the component.
 *
 */
public class ComponentRefinementRecord {

    /**
     * This array contains the path to the property that was refined.
     * The property can be either a parameter or a required interface.
     *
     * This array is never empty or null.
     * An array was used to save memory: reuse the String values and only reference it through the array.
     */
    private final String[] propertyRefinementPath;

    private final boolean propertyIsParam;

    private ComponentRefinementRecord(String[] propertyRefinementPath, boolean propertyIsParam) {
        this.propertyRefinementPath = propertyRefinementPath;
        this.propertyIsParam = propertyIsParam;
    }

    public static ComponentRefinementRecord refineRequiredInterface(String[] componentPath) {
        return new ComponentRefinementRecord(componentPath, false);
    }

    public static ComponentRefinementRecord refineRequiredInterface(String[] parentComponentPath, String requiredInterfaceName) {
        final int componentDepth = parentComponentPath.length;
        final String[] componentPath = Arrays.copyOf(parentComponentPath, componentDepth + 1);
        componentPath[componentDepth] = requiredInterfaceName;
        return new ComponentRefinementRecord(componentPath, false);
    }

    public static ComponentRefinementRecord refineRequiredInterface(String[] componentPath, String paramName, String paramValue) {
        final int componentDepth = componentPath.length;
        final String[] propertyRefinementPath = Arrays.copyOf(componentPath, componentDepth + 2);
        propertyRefinementPath[componentDepth] = paramName;
        propertyRefinementPath[componentDepth + 1] = paramValue;
        return new ComponentRefinementRecord(propertyRefinementPath, true);
    }

    public String[] getPropertyRefinementPath() {
        return propertyRefinementPath;
    }

    public boolean isPropertyIsParam() {
        return propertyIsParam;
    }

    public String getParamName() {
        if(propertyIsParam) {
            return propertyRefinementPath[propertyRefinementPath.length-2];
        } else {
            throw new IllegalStateException(toString());
        }
    }

    public String getParamValue() {
        if(propertyIsParam) {
            return propertyRefinementPath[propertyRefinementPath.length-1];
        } else {
            throw new IllegalStateException(toString());
        }
    }

    public String toString() {
        return String.format("refinement record: %s, %b", Arrays.toString(propertyRefinementPath), propertyIsParam);
    }
}
