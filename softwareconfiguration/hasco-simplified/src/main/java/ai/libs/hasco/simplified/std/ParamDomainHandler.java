package ai.libs.hasco.simplified.std;

import ai.libs.hasco.model.CategoricalParameterDomain;
import ai.libs.hasco.model.IParameterDomain;
import ai.libs.hasco.model.NumericParameterDomain;
import ai.libs.jaicore.basic.sets.SetUtil;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Component
public class ParamDomainHandler {

    static IParameterDomain strToParamDomain(final IParameterDomain domain,
                                                     final String strValue) {
        if(strValue == null) {
            return domain;
        }

        if(domain instanceof NumericParameterDomain) {
            NumericParameterDomain numDom = (NumericParameterDomain) domain;
            NumericSplit split = new NumericSplit(strValue, numDom);
            if(split.isValueFixed()) {
                return new NumericParameterDomain(split.isInteger(),
                        split.getFixedVal().doubleValue(), split.getFixedVal().doubleValue());
            } else {
                return new NumericParameterDomain(split.isInteger(),
                        split.getMin(), split.getMax());
            }
        } else if(domain instanceof CategoricalParameterDomain) {
            CategoricalParameterDomain catDom = (CategoricalParameterDomain) domain;
            if(domain.contains(strValue)) {
                return new CategoricalParameterDomain(new String[]{strValue});
            } else {
                // assume its a list of cat values:
                try {
                    List<String> strings = SetUtil.unserializeList(strValue);
                    String[] catValues;
                    if(strings.size() == 1 && strings.get(0).isEmpty()) {
                        catValues = new String[0];
                    } else {
                        catValues = strings.toArray(new String[0]);
                    }
                    return new CategoricalParameterDomain(catValues);
                } catch(RuntimeException ex) {
                    throw new RuntimeException("The categorical value: " + strValue + " is not in parameter domain: " + domain,
                            ex);
                }
            }
        } else {
            throw domainNotRecognized(domain);
        }
    }


    static IParameterDomain intersection(final IParameterDomain domain1, final IParameterDomain domain2) {
        if(domain1 instanceof NumericParameterDomain &&
                domain2 instanceof NumericParameterDomain) {
            NumericParameterDomain numDom1 = (NumericParameterDomain) domain1;
            NumericParameterDomain numDom2 = (NumericParameterDomain) domain2;
            double newMin = Double.max(numDom1.getMin(), numDom2.getMin());
            double newMax = Double.min(numDom1.getMax(), numDom2.getMax());
            return new NumericParameterDomain(numDom1.isInteger(), newMin, newMax);

        } else if(domain1 instanceof  CategoricalParameterDomain &&
                domain2 instanceof CategoricalParameterDomain) {
            Collection<String> values;
            CategoricalParameterDomain catDom = (CategoricalParameterDomain) domain1;
            CategoricalParameterDomain catDom2 = (CategoricalParameterDomain) domain2;

            values = SetUtil.intersection(
                    Arrays.asList(catDom.getValues()), Arrays.asList(catDom2.getValues()));
            String[] valArr = values.toArray(new String[0]);
            return new CategoricalParameterDomain(valArr);
        } else {
            throw new IllegalArgumentException("Domain type mismatch: " + domain1 + " ≠ " + domain2);
        }
    }

    static String getStringValue(final IParameterDomain domain) {
        if(domain instanceof NumericParameterDomain) {
            NumericParameterDomain numDom = (NumericParameterDomain) domain;
            if(numDom.getMin() >= numDom.getMax()) {
                return NumericSplit.valueToString(numDom.getMin(), numDom.isInteger());
            } else {
                return NumericSplit.rangeToString(numDom.getMin(), numDom.getMax());
            }
        } else if(domain instanceof CategoricalParameterDomain) {
            CategoricalParameterDomain catDom = (CategoricalParameterDomain) domain;
            if (catDom.getValues().length == 0) {
                return "[]";
            } else if (catDom.getValues().length == 1) {
                return catDom.getValues()[0];
            } else {
                StringBuilder sb = new StringBuilder("[");
                String[] values = catDom.getValues();
                for (int i = 0; i < values.length; i++) {
                    String value = values[i];
                    sb.append(value);
                    if (i + 1 < values.length) {
                        sb.append(",");
                    }
                }
                return sb.append("]").toString();
            }
        } else {
            throw domainNotRecognized(domain);
        }

    }

    private static RuntimeException domainNotRecognized(IParameterDomain domain) {
        return new RuntimeException(String.format("Parameter Domain %s with class %s was not recognized", domain, domain.getClass().getName()));
    }

    public static String selectRandomValue(IParameterDomain domain, Supplier<Double> ng) {
        if(domain instanceof NumericParameterDomain) {
            NumericParameterDomain numDomain = (NumericParameterDomain) domain;
            if (numDomain.isInteger()) {
                if ((int) (numDomain.getMax() - numDomain.getMin()) > 0) {
                    return String.valueOf((int)
                            (ng.get() * ((int) (numDomain.getMax() - numDomain.getMin())) + numDomain.getMin()));
                } else {
                    return String.valueOf((long) numDomain.getMin());
                }
            } else {
                return String.valueOf(ng.get() * (numDomain.getMax() - numDomain.getMin()) + numDomain.getMin());
            }
        } else if(domain instanceof CategoricalParameterDomain) {
            CategoricalParameterDomain catDomain = (CategoricalParameterDomain) domain;
            String[] values = catDomain.getValues();
            return values[(int) (ng.get() * values.length)];
        } else {
            throw domainNotRecognized(domain);
        }
    }
}
