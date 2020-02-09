package ai.libs.hasco.simplified.std;

import ai.libs.hasco.core.Util;
import ai.libs.hasco.model.*;
import ai.libs.jaicore.basic.sets.Pair;

import java.util.*;
import java.util.function.Supplier;

class ImplUtil {

    static boolean binaryDecision(Supplier<Double> numberGenerator) {
        return numberGenerator.get() > 0.5d;
    }

    static int decideBetween(Supplier<Double> numberGenerator, int choiceCount) {
        return (int)(choiceCount*numberGenerator.get());
    }
    static <E> E decideBetween(Supplier<Double> numberGenerator, List<E> list) {
        int i = decideBetween(numberGenerator, list.size());
        return list.get(i);
    }
//
    /**
     * Checks whether a component instance adheres to the defined inter-parameter dependencies defined in the component.
     * @param ci The component instance to be verified.
     * @return Returns true iff all dependency conditions hold.
     */
    static boolean isValidComponentPrototype(final ComponentInstance ci) {
        Map<Parameter, IParameterDomain> refinedDomainMap = new HashMap<>();

        for (Parameter param : ci.getComponent().getParameters()) {
            String paramVal = ci.getParameterValue(param);
            IParameterDomain domain;
            if(paramVal == null)  {
                domain = param.getDefaultDomain();
            } else if (param.getDefaultDomain() instanceof NumericParameterDomain) {
                NumericSplit split = new NumericSplit(paramVal, (NumericParameterDomain) param.getDefaultDomain());
                if (split.isValueFixed()) {
                    double parameterValue = split.getFixedVal().doubleValue();
                    domain = new NumericParameterDomain(split.isInteger(),
                            parameterValue, parameterValue);
                } else {
                    domain = new NumericParameterDomain(split.isInteger(),
                            split.getMin(), split.getMax());
                }
            } else if (param.getDefaultDomain() instanceof CategoricalParameterDomain) {
                // assume that param value is a single categorical value, not a subset of the categories
                domain = new CategoricalParameterDomain(Collections.singletonList(paramVal));
            } else {
                throw new IllegalArgumentException("Domain not recognized: "
                        + param.getName() + "->" + param.getDefaultDomain());
            }
            refinedDomainMap.put(param, domain);
        }
//
//        for (Dependency dependency : ci.getComponent().getDependencies()) {
//            if (Util.isDependencyPremiseSatisfied(dependency, refinedDomainMap)
//                    && !Util.isDependencyConditionSatisfied(dependency.getConclusion(), refinedDomainMap)) {
//                return false;
//            }
//        }

        for (Dependency dependency : ci.getComponent().getDependencies()) {
            if(isPremiseTautology(dependency, refinedDomainMap)) {
                if(!isConclusionSatisfiable(dependency, refinedDomainMap)) {
                    return false;
                }
            }
        }
        return true;
    }


    private static boolean isPremiseTautology(final Dependency dependency, final Map<Parameter, IParameterDomain> values) {
        /*
         * The dependency premise is in DNF, so if one entry is satisfied, its enough:
         */
        for (Collection<Pair<Parameter, IParameterDomain>> condition : dependency.getPremise()) {
            boolean check = Util.isDependencyConditionSatisfied(condition, values);
            if (check) {
                return true;
            }
        }
        return false;
    }

    private static boolean isConclusionSatisfiable(final Dependency dependency, final Map<Parameter, IParameterDomain> values) {
        Collection<Pair<Parameter, IParameterDomain>> conclusion = dependency.getConclusion();
        for (Pair<Parameter, IParameterDomain> pair : conclusion) {
            Parameter param = pair.getX();
            IParameterDomain requiredDom = pair.getY();
            if (!values.containsKey(param)) {
                throw new IllegalArgumentException("Cannot check condition "
                        + pair + " as the value for parameter "
                        + param.getName() + " is not defined in " + values);
            }
            IParameterDomain actualDom = values.get(param);
            if (actualDom == null) {
                throw new IllegalArgumentException("Cannot check condition "
                        + pair + " as the value for parameter "
                        + param.getName() + " is NULL in " + values);
            }
            if(!isSatisfiable(requiredDom, actualDom)) {
                return false;
            }
        }
        return true;
    }

    public static boolean isSatisfiable(IParameterDomain requiredDom, IParameterDomain actualDom) {
        if(requiredDom instanceof NumericParameterDomain) {
            if(!(actualDom instanceof NumericParameterDomain)) {
                throw new IllegalArgumentException("Domain type mismatch: " + requiredDom + " ≠ " + actualDom);
            }
            double requiredMin = ((NumericParameterDomain) requiredDom).getMin();
            double actualMin = ((NumericParameterDomain) actualDom).getMin();
            double requiredMax = ((NumericParameterDomain) requiredDom).getMax();
            double actualMax = ((NumericParameterDomain) actualDom).getMax();

            if(requiredMin <= actualMin) {
                if(requiredMax >= actualMin) {
                    return true;
                }
            } else if(requiredMin <= actualMax){
                return true;
            }
            return false;
        } else if(requiredDom instanceof CategoricalParameterDomain) {
            if(!(actualDom instanceof CategoricalParameterDomain)) {
                throw new IllegalArgumentException("Domain type mismatch: " + requiredDom + " ≠ " + actualDom);
            }
            List<String> actualCatValues = Arrays.asList(((CategoricalParameterDomain) actualDom).getValues());
            Optional<String> any = Arrays.stream(((CategoricalParameterDomain) requiredDom).getValues())
                    .filter(actualCatValues::contains)
                    .findAny();
            return any.isPresent();
        } else {
            throw new IllegalArgumentException("Domain type not recognized: " + requiredDom);
        }
    }


}
