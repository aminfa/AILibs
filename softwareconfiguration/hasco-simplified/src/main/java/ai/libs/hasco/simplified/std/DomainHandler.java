package ai.libs.hasco.simplified.std;

import ai.libs.hasco.model.BooleanParameterDomain;
import ai.libs.hasco.model.CategoricalParameterDomain;
import ai.libs.hasco.model.IParameterDomain;
import ai.libs.hasco.model.NumericParameterDomain;
import ai.libs.jaicore.basic.sets.SetUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public interface DomainHandler<T> {

    static final DomainHandler<String> STR_CONVERTER = DomainHandler.newHandler(
            numDom -> {
                if(numDom.getMin() >= numDom.getMax()) {
                    return NumericSplit.valueToString(numDom.getMin(), numDom.isInteger());
                } else {
                    return NumericSplit.rangeToString(numDom.getMin(), numDom.getMax());
                }
            },
            catDom -> {
                if(catDom.getValues().length == 0) {
                    return "[]";
                } else if(catDom.getValues().length == 1) {
                    return catDom.getValues()[0];
                } else {
                    StringBuilder sb = new StringBuilder("[");
                    String[] values = catDom.getValues();
                    for (int i = 0; i < values.length; i++) {
                        String value = values[i];
                        sb.append(value);
                        if(i + 1 < values.length) {
                            sb.append(",");
                        }
                    }
                    return sb.append("]").toString();
                }
            }
    );


    T handleNumericDomain(NumericParameterDomain numDom);

    T handleCatDomain(CategoricalParameterDomain catDom);

    default T handleBooleanDomain(BooleanParameterDomain boolDom) {
        return handleCatDomain(boolDom);
    }


    default T handle(IParameterDomain domain) {
        if(domain instanceof NumericParameterDomain) {
            return handleNumericDomain((NumericParameterDomain) domain);
        } else if(domain instanceof BooleanParameterDomain) {
            return handleBooleanDomain((BooleanParameterDomain) domain);
        } else if(domain instanceof CategoricalParameterDomain) {
            return handleCatDomain((CategoricalParameterDomain) domain);
        } else {
            throw new IllegalArgumentException("Unknown domain type: " + domain.getClass().getName() + ", " + domain);
        }
    }

    static <T> DomainHandler<T> newHandler(final Function<NumericParameterDomain, T> numHandler,
                            final Function<CategoricalParameterDomain, T> catHandler) {
        return new DomainHandler<T>() {

            @Override
            public T handleNumericDomain(NumericParameterDomain numDom) {
                return numHandler.apply(numDom);
            }

            @Override
            public T handleCatDomain(CategoricalParameterDomain catDom) {
                return catHandler.apply(catDom);
            }
        };
    }

    static DomainHandler<Void> newHandler(final Consumer<NumericParameterDomain> numHandler,
                                          final Consumer<CategoricalParameterDomain> catHandler) {
        return new DomainHandler<Void>() {
            @Override
            public Void handleNumericDomain(NumericParameterDomain numDom) {
                numHandler.accept(numDom);
                return null;
            }

            @Override
            public Void handleCatDomain(CategoricalParameterDomain catDom) {
                catHandler.accept(catDom);
                return null;
            }
        };
    }

    static IParameterDomain strToParamDomain(final IParameterDomain baseDomain,
                                                     final String strValue) {
        if(strValue == null) {
            return baseDomain;
        }
        return (IParameterDomain) DomainHandler.<IParameterDomain>newHandler(
                numDomain -> {
                    NumericSplit split = new NumericSplit(strValue, numDomain);
                    if(split.isValueFixed()) {
                        return new NumericParameterDomain(split.isInteger(),
                                split.getFixedVal().doubleValue(), split.getFixedVal().doubleValue());
                    } else {
                        return new NumericParameterDomain(split.isInteger(),
                                split.getMin(), split.getMax());
                    }
                },
                catDomain -> {
                    if(baseDomain.contains(strValue)) {
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
                            throw new RuntimeException("The categorical value: " + strValue + " is not in parameter domain: " + baseDomain,
                                    ex);
                        }
                    }
                }
        ).handle(baseDomain);
    }


    static IParameterDomain intersection(final IParameterDomain domain1, final IParameterDomain domain2) {
        return newHandler(
                numDom ->  (IParameterDomain) newHandler(
                    numDom2 -> {
                        double newMin = Double.max(numDom.getMin(), numDom2.getMin());
                        double newMax = Double.min(numDom.getMax(), numDom2.getMax());
                        return new NumericParameterDomain(numDom.isInteger(), newMin, newMax);
                    },
                    catDom2 -> {
                        throw new IllegalArgumentException("Domain type mismatch: " + domain1 + " ≠ " + domain2);
            }).handle(domain2),
                catDom -> newHandler(
                        numDom2 -> {
                            throw new IllegalArgumentException("Domain type mismatch: " + domain1 + " ≠ " + domain2);
                        },
                         catDom2 -> {
                            Collection<String> values;
                            values = SetUtil.intersection(
                                    Arrays.asList(catDom.getValues()), Arrays.asList(catDom2.getValues()));
                            String[] valArr = values.toArray(new String[0]);
                            return new CategoricalParameterDomain(valArr);
                        }
                ).handle(domain2)
        ).handle(domain1);
    }

    static String getStringValue(final IParameterDomain domain) {
        return STR_CONVERTER.handle(domain);
    }

}
