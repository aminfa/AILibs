package ai.libs.hasco.simplified.std;

import ai.libs.hasco.model.CategoricalParameterDomain;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.model.NumericParameterDomain;

import java.util.List;

public interface RefinementAgent {

    boolean refineCategoricalParameter(List<ComponentInstance> refinements,
                                       ComponentInstance component,
                                       CategoricalParameterDomain domain,
                                       String currentValue);

    boolean refineNumericalParameter(List<ComponentInstance> refinements,
                                     ComponentInstance component,
                                     NumericParameterDomain domain,
                                     String currentValue);

    boolean refineRequiredInterface(List<ComponentInstance> refinements,
                                    ComponentInstance component,
                                    String requiredInterface);

}
