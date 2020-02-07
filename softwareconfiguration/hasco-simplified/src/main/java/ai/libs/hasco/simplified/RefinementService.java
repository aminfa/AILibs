package ai.libs.hasco.simplified;

import ai.libs.hasco.model.CategoricalParameterDomain;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.model.NumericParameterDomain;
import ai.libs.hasco.model.ParameterRefinementConfiguration;

import java.util.List;

public interface RefinementService {

    boolean refineCategoricalParameter(List<ComponentInstance> refinements,
                                       ComponentInstance base, ComponentInstance component,
                                       CategoricalParameterDomain domain,
                                       String name, String currentValue);

    boolean refineNumericalParameter(List<ComponentInstance> refinements,
                                     ComponentInstance base, ComponentInstance component,
                                     NumericParameterDomain domain,
                                     String name, String currentValue);

    boolean refineRequiredInterface(List<ComponentInstance> refinements,
                                    ComponentInstance base, ComponentInstance component,
                                    String name, String requiredInterface);

}
