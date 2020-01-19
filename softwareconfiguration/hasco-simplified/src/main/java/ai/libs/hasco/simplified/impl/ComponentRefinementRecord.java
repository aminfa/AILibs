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
    private String[] propertyRefinementPath;

    private boolean propertyIsParam;

    private ComponentRefinementRecord(String[] propertyRefinementPath, boolean propertyIsParam) {
        this.propertyRefinementPath = propertyRefinementPath;
        this.propertyIsParam = propertyIsParam;
    }

    public static ComponentRefinementRecord refineRequiredInterface(String[] componentPath) {
        return new ComponentRefinementRecord(componentPath, false);
    }

    public static ComponentRefinementRecord refineRequiredInterface(String[] componentPath, String paramName, String paramValue) {
        String[] propertyRefinementPath = Arrays.copyOf(componentPath, componentPath.length + 2);
        propertyRefinementPath[componentPath.length - 2] = paramName;
        propertyRefinementPath[componentPath.length - 1] = paramValue;
        return new ComponentRefinementRecord(propertyRefinementPath, true);
    }

    public String[] getPropertyRefinementPath() {
        return propertyRefinementPath;
    }

    public boolean isPropertyIsParam() {
        return propertyIsParam;
    }
}
