package ai.libs.hasco.simplified.std;

import ai.libs.hasco.model.CategoricalParameterDomain;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.model.Parameter;

import java.util.*;

public class ComponentParamRefinementContext {

    private ComponentInstance component;

    private List<ComponentInterfaceRefinementRecord> list = new ArrayList<>();

    public ComponentParamRefinementContext(ComponentInstance component) {
        this.component = component;
    }

    public ComponentInstance getComponent() {
        return component;
    }

    public void selectCategory(String paramName, String category) {

    }

    public void selectCategory(String paramName, int index) {
        Parameter param = getComponent().getComponent().getParameterWithName(paramName);
        if(!param.isCategorical()) {
            throw new IllegalArgumentException(String.format("Parameter %s is not a category", paramName));
        }
        CategoricalParameterDomain catParamDom = (CategoricalParameterDomain) param.getDefaultDomain();
        String[] categories = catParamDom.getValues();
        if(index < 0 || categories.length <= index) {
            throw new IllegalArgumentException(
                    String.format("Index %d is out of bound of categories: %s", index, Arrays.toString(categories)));
        }
        HashSet set = null;
    }
}
