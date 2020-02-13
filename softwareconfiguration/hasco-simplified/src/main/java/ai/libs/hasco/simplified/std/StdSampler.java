package ai.libs.hasco.simplified.std;

import ai.libs.hasco.model.*;
import ai.libs.hasco.simplified.ComponentInstanceSampler;
import ai.libs.hasco.simplified.ComponentRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StdSampler implements ComponentInstanceSampler {

    private final static Logger logger = LoggerFactory.getLogger(StdSampler.class);

    private static boolean DRAW_NUMERIC_SPLIT_MEANS = false;

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
        CIRoot root = (CIRoot) source;
        int alreadySampleSize = 0;
        if(root.hasBeenEvaluated()) {
            alreadySampleSize = root.getEvalReport().getWitnessScores().size();
        }
        List<ComponentInstance> cis = new ArrayList<>();
        for (int i = 0; i < SAMPLES_PER_DRAW - alreadySampleSize; i++) {
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
                providerCp = copyParams(provider);
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
            if(param.isNumeric()) {
                /*
                 * Draw numeric value from splits
                 */
                newVal = drawRandomValueFromSplits(instance.getComponent(), param, val);
            } else{
                newVal = selectRandomCategory(param, val);
            }
            values.put(name, newVal);
        }
    }

    private String drawRandomValueFromSplits(Component component, Parameter param, String currentVal) {
        NumericSplit ns = new NumericSplit(currentVal, (NumericParameterDomain) param.getDefaultDomain());
        if(ns.isValueFixed()) {
            // already fixed:
            return String.valueOf(ns.getFixedVal());
        } else if(DRAW_NUMERIC_SPLIT_MEANS){
            Optional<ParameterRefinementConfiguration> paramRefConfigOpt = registry.getParamRefConfig(component, param);
            double minSplitSize;
            int splitCount;
            if(paramRefConfigOpt.isPresent()) {
                minSplitSize = paramRefConfigOpt.get().getIntervalLength();
                splitCount = paramRefConfigOpt.get().getRefinementsPerStep();
            } else {
                // TODO defaults are hardcoded:
                minSplitSize = 2.0;
                splitCount = 2;
            }
            ns.configureSplits(splitCount, minSplitSize);
            ns.setComponentName(component.getName());
            ns.setParamName(param.getName());
            ns.createValues();
            List<String> splits = ns.getSplits();
            if(splits.isEmpty()) {
                logger.warn("Couldn't create split random draws: {}/{}/{}. " +
                        "\nFalling back to random draw.", ns.getPreSplitRange(), splitCount, minSplitSize);
                return selectRandomValue(DomainHandler.strToParamDomain(param.getDefaultDomain(), currentVal));
            }

            String randomParamValue = splits.get((int) (splits.size() * ng.get()));
            return randomParamValue;
        } else {
            return selectRandomValue(DomainHandler.strToParamDomain(param.getDefaultDomain(), currentVal));
        }
    }

    private String selectRandomCategory(Parameter param, String val) {
        CategoricalParameterDomain dom = (CategoricalParameterDomain)
                DomainHandler.strToParamDomain(param.getDefaultDomain(), val);
        if(dom.getValues().length == 0) {
            throw new IllegalArgumentException("Parameter " + param + " with value "
                    + val + " has not categories left to choose from.");
        }
        int index = ImplUtil.decideBetween(ng, dom.getValues().length);
        return dom.getValues()[index];
    }

    private String selectRandomValue(IParameterDomain domain) {
        return DomainHandler.newHandler(
                (NumericParameterDomain numDomain) -> {
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
                },
                (CategoricalParameterDomain catDom) -> {
                    String[] values = catDom.getValues();
                    return values[(int) (ng.get() * values.length)];
                }
        ).handle(domain);
    }

    private ComponentInstance getRandomProvider(String requiredInteface) {
        // TODO add loop prevention
        List<Component> providers = registry.getProvidersOf(requiredInteface);
        Component component =  ImplUtil.decideBetween(ng, providers);
        ComponentInstance ci = new ComponentInstance(component, new HashMap<>(), new HashMap<>());
        return ci;
    }

}
