package ai.libs.hasco.simplified.std;

import ai.libs.hasco.model.*;
import ai.libs.hasco.simplified.ComponentInstanceSampler;
import ai.libs.hasco.simplified.ComponentRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StdSampler implements ComponentInstanceSampler {

    private final static Logger logger = LoggerFactory.getLogger(StdSampler.class);

    private static final String REGEX_NUMERIC_RANGE = "\\[\\s*(?<num1>[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?)" +
            "\\s*,\\s*" +
        "(?<num2>[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?)\\s*]"; // E.g. matches: "[ -10 : 210.1 ]"

    public static final Pattern PATTERN_NUMERIC_RANGE = Pattern.compile(REGEX_NUMERIC_RANGE);

    private final ComponentRegistry registry;

    private final Supplier<Double> ng;

    private static int SAMPLES_PER_DRAW = 1;

    public StdSampler(ComponentRegistry registry, Supplier<Double> ng) {
        this.ng = ng;
        this.registry = registry;
    }

    public StdSampler(ComponentRegistry registry, Long seed) {
        this(registry, new Random(seed)::nextDouble);
    }

    private ComponentInstance copyParams(ComponentInstance source) {
        return new ComponentInstance(source.getComponent(), new HashMap<>(source.getParameterValues()), new HashMap<>());
    }

    @Override
    public List<ComponentInstance> drawSamples(ComponentInstance source) {
        List<ComponentInstance> cis = new ArrayList<>();
        for (int i = 0; i < SAMPLES_PER_DRAW; i++) {
            ComponentInstance target = copyParams(source);
            // set all required interfaces, using an index guard
            groundRequiredInterfaces(source, target, new HashSet<>());

            // parameterize all remaining parameters, iteratively using an index guard
            // remaining parameter ~ not completely set
            groundParameters(target);
            cis.add(target);
        }
        return cis;
    }

    private boolean checkGuardAndUpdate(Set<Integer> guard, ComponentInstance componentInstance) {
        if(componentInstance instanceof CIIndexed) {
            Integer index = ((CIIndexed) componentInstance).getIndex();
            if(guard.contains(index)) {
                return true;
            }
            guard.add(index);
        }
        return false;
    }

    private void groundRequiredInterfaces(ComponentInstance source, ComponentInstance target, Set<Integer> guard) {
        if(checkGuardAndUpdate(guard, source)) {
            // TODO - important: If we ever have component reuse, then we need to copy the hierarchy.
            // Use the deep copy in CIPhase1 before setting the remaining required interfaces.
            return;
        }
        Component component = source.getComponent();
        for(Map.Entry<String, String> requiredInterface : component.getRequiredInterfaces().entrySet()) {
            String name = requiredInterface.getKey();
            String interfaceName = requiredInterface.getValue();
            ComponentInstance provider = source.getSatisfactionOfRequiredInterfaces().get(name);
            ComponentInstance providerCp;
            if(provider == null) {
                providerCp = getRandomProvider(interfaceName);
                provider = providerCp; // this is on purpose.
            } else {
                providerCp = copyParams(source);
            }
            groundRequiredInterfaces(provider, providerCp, guard);
            target.getSatisfactionOfRequiredInterfaces().put(name, providerCp);
        }
    }

    private void groundParameters(ComponentInstance comp) {
        Set<Integer> guard = new HashSet<>();

        Deque<ComponentInstance> nonGrounded = new ArrayDeque<>();

        nonGrounded.add(comp);
        while(!nonGrounded.isEmpty()) {
            comp = nonGrounded.pop();
            /*
             * Add all children to the queue
             */
            for (ComponentInstance child : comp.getSatisfactionOfRequiredInterfaces().values()) {
                boolean alreadyProcessed = checkGuardAndUpdate(guard, child);
                if(!alreadyProcessed) {
                    nonGrounded.add(child);
                }
            }
            /*
             * Parameterize remaining:
             */
            parameterizeRemainingParameters(comp);
        }

    }

    private void parameterizeRemainingParameters(ComponentInstance instance) {
        Map<String, String> values = instance.getParameterValues();
        Component component = instance.getComponent();
        for (Parameter param : component.getParameters()) {
            String name = param.getName();
            String val = values.get(name);
            String newVal;
            if(val == null) {
                newVal = selectRandomValue(param);
            } else {
                newVal = selectFromValue(param, val);
            }
            values.put(name, newVal);
        }
    }

    private String selectRandomValue(Parameter param) {
        if(param.isNumeric()) {
            NumericParameterDomain numDomain = (NumericParameterDomain) param.getDefaultDomain();
            if (numDomain.isInteger()) {
                if ((int) (numDomain.getMax() - numDomain.getMin()) > 0) {
                    return ((int) (ng.get() * ((int) (numDomain.getMax() - numDomain.getMin())) + numDomain.getMin())) + "";
                } else {
                    return (int) param.getDefaultValue() + "";
                }
            } else {
                return ng.get() * (numDomain.getMax() - numDomain.getMin()) + numDomain.getMin() + "";
            }
        } else {
            String[] values = ((CategoricalParameterDomain) param.getDefaultDomain()).getValues();
            return values[(int) (ng.get() * values.length)];
        }
    }

    private String selectFromValue(Parameter param, String val) {
        if(param.isNumeric()) {
            Matcher matcher = PATTERN_NUMERIC_RANGE.matcher(val);
            if(matcher.matches()) {
                String num1 = matcher.group("num1");
                String num2 = matcher.group("num2");

                double min;
                double max;
                try{
                    min = Double.parseDouble(num1);
                    max = Double.parseDouble(num2);
                } catch(NumberFormatException exception) {
                    logger.error("Couldn't parse param {} range from {} ", param.getName(), val);
                    return param.getDefaultValue().toString();
                }
                NumericParameterDomain numDomain = (NumericParameterDomain) param.getDefaultDomain();
                if(numDomain.isInteger()) {
                    if ((int) (max - min) > 0) {
                        return ((int) (ng.get() * (numDomain.getMax() - numDomain.getMin())) + numDomain.getMin()) + "";
                    } else {
                        logger.error("Param range error {}", val);
                        return param.getDefaultValue().toString();
                    }
                } else {
                    return ng.get() * (numDomain.getMax() - numDomain.getMin()) + numDomain.getMin() + "";
                }
            } else {
                try {
                    return String.valueOf(Double.parseDouble(val)); // check if number
                } catch(NumberFormatException e) {
                    logger.error("Illegal numeric param value: {}", val);
                    return param.getDefaultValue().toString();
                }
            }
        } else {
            // assume the value is a specific categorical value:
            return val;
        }
    }

    private ComponentInstance getRandomProvider(String requiredInteface) {
        // TODO add loop prevention
        List<Component> providers = registry.getProvidersOf(requiredInteface);
        Component component =  ImplUtil.decideBetween(ng, providers);
        ComponentInstance ci = new ComponentInstance(component, new HashMap<>(), new HashMap<>());
        return ci;
    }

}
