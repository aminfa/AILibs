package ai.libs.hasco.simplified.std;

import java.util.Objects;

public class ComponentParamRefinementRecord {

    private final String[] componentPath;

    private final String paramName;

    public ComponentParamRefinementRecord(String[] componentPath, String paramName) {
        this.componentPath = Objects.requireNonNull(componentPath);
        this.paramName = Objects.requireNonNull(paramName);
    }

    public String[] getComponentPath() {
        return componentPath;
    }

    public String getParamName() {
        return paramName;
    }
}
