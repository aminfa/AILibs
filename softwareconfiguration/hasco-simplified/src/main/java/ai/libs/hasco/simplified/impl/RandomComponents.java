package ai.libs.hasco.simplified.impl;

import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.model.NumericParameterDomain;
import ai.libs.hasco.model.Parameter;
import ai.libs.hasco.simplified.ComponentInstanceSampler;
import ai.libs.hasco.simplified.ComponentRefiner;
import ai.libs.hasco.simplified.ComponentRegistry;
import scala.xml.PrettyPrinter;

import java.lang.reflect.MalformedParameterizedTypeException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import static ai.libs.hasco.simplified.impl.RandomUtil.*;

public class RandomComponents implements ComponentInstanceSampler, ComponentRefiner {

    private final ComponentRegistry registry;

    private final Supplier<Double> refinementNumberGenerator;

    private final Supplier<Double> sampleNumberGenerator;

    private final static Integer MAX_INT_PARAM_REFINEMENTS = 3;

    public RandomComponents(ComponentRegistry registry, Supplier<Double> refinementNumberGenerator, Supplier<Double> sampleNumberGenerator) {
        this.registry = registry;
        this.refinementNumberGenerator = refinementNumberGenerator;
        this.sampleNumberGenerator = sampleNumberGenerator;
    }

    public RandomComponents(ComponentRegistry registry) {
        this(registry, new Random()::nextDouble, new Random()::nextDouble);
    }

    public RandomComponents(ComponentRegistry registry, Long seed1, Long seed2) {
        this(registry, new Random(seed1)::nextDouble, new Random(seed2)::nextDouble);
    }

    @Override
    public List<ComponentInstance> refine(ComponentInstance componentInstance) {
        /*
         * First make a coin-flip if parameters are set.
         * Or required instances are satisfied.
         */
        if(RandomUtil.binaryDecision(refinementNumberGenerator)) {
            return refineComponents(componentInstance);
        } else {
            return refineParameters(componentInstance);
        }
    }

    private List<ComponentInstance> refineParameters(ComponentInstance componentInstance) {
        List<Parameter> remainingParams =
                (List<Parameter>) componentInstance
                        .getParametersThatHaveNotBeenSetExplicitly();
        if(remainingParams.isEmpty()) {
            return refineComponents(componentInstance);
        } else {
            int paramIndex = decideBetween(refinementNumberGenerator, remainingParams.size());
            return refineParameter(componentInstance, remainingParams.get(paramIndex));
        }
    }

    private List<ComponentInstance> refineParameter(ComponentInstance componentInstance, Parameter parameter) {
        if(parameter.isNumeric()) {
            NumericParameterDomain domain = (NumericParameterDomain) parameter.getDefaultDomain();
            double min = domain.getMin();
            double domainLength = domain.getMax() - min;
            if(domainLength <= 0) {
//                ComponentInstance newInstance = new ComponentInstance()
            }
            int refinements = decideBetween(refinementNumberGenerator, MAX_INT_PARAM_REFINEMENTS + 1);
            if(domain.isInteger()) {

            }
        }
    }

    private ComponentInstance setNumParam(ComponentInstance ci, Parameter parameter, Double paramVal) {
        IncrementalMap<String, String> paramVals = new IncrementalMap<>(ci.getParameterValues());
//        IncrementalMap<String, ComponentInstance> requiredComps = new IncrementalMap<>(ci.getSatisfactionOfRequiredInterfaces());

        String paramValStr = ((NumericParameterDomain) parameter.getDefaultDomain()).isInteger()?
                "" + paramVal.intValue() : paramVal.toString();
        paramVals.put(parameter.getName(), paramValStr);
        return new ComponentInstance(ci.getComponent(),
                paramVals,
                ci.getSatisfactionOfRequiredInterfaces());
    }

    private List<ComponentInstance> refineComponents(ComponentInstance componentInstance) {


    }


    @Override
    public List<ComponentInstance> drawSamples(ComponentInstance refined) {
        return null;
    }

}
