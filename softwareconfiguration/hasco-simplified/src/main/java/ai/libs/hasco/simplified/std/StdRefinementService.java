package ai.libs.hasco.simplified.std;

import ai.libs.hasco.model.*;
import ai.libs.hasco.simplified.ComponentRegistry;
import ai.libs.hasco.simplified.RefinementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.ComponentScan;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;

@org.springframework.stereotype.Component
public class StdRefinementService implements RefinementService {

    private final static Logger logger = LoggerFactory.getLogger(StdRefinementService.class);

    private final ComponentRegistry registry;

    private final static DecimalFormat DECIMAL_FORMATTER;
    static  {
        DECIMAL_FORMATTER = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);
    }
    // new DecimalFormat("###.####");

    private final static int NUM_SPLITS = 2;
    private final static int MIN_RANGE_SIZE = 2;

    public StdRefinementService(ComponentRegistry registry) {
        this.registry = registry;
    }

    @Override
    public boolean refineCategoricalParameter(List<ComponentInstance> refinements, ComponentInstance base,
                                              ComponentInstance component, CategoricalParameterDomain domain,
                                              String name, String currentValue) {
        if(!( component instanceof CIIndexed) ) {
            throw new IllegalArgumentException("Component is not indexed");
        }
        if(!(base instanceof CIPhase2)) {
            throw new IllegalArgumentException("Base component is not of phase 2 type: " + base.getClass().getName());
        }
        CIPhase2 phase2Base = (CIPhase2) base;
        if(currentValue != null) {
            return false;
        }
        if(domain.getValues() == null || domain.getValues().length == 0) {
            return false;
        }
        for (String category : domain.getValues()) {
            CIPhase2 newBase = new CIPhase2(phase2Base);
            ComponentInstance refinedInstance = newBase.getComponentByPath(((CIIndexed) component).getPath());
            refinedInstance.getParameterValues().put(name, category);
            newBase.copyWitnessEvaluations(phase2Base);
            refinements.add(newBase);
        }
        return true;
    }

    @Override
    public boolean refineNumericalParameter(List<ComponentInstance> refinements,
                                            ComponentInstance base, ComponentInstance component,
                                            NumericParameterDomain domain,  String name, String currentValue) {
        // TODO split this method up
        if(!( component instanceof CIIndexed) ) {
            throw new IllegalArgumentException("Component is not indexed");
        }
        if(!(base instanceof CIPhase2)) {
            throw new IllegalArgumentException("Base component is not of phase 2 type: " + base.getClass().getName());
        }
        NumericSplit numericSplit = new NumericSplit(currentValue, domain);
        numericSplit.setComponentName(component.getComponent().getName());
        numericSplit.setParamName(name);
        if(numericSplit.isValueFixed()) {
            /*
             * The int parameter already has a fixed value
             */
            if(logger.isTraceEnabled())
                logger.trace("Parameter {} in component {} already has a fixed value: {}", name,
                        component.getComponent().getName(), numericSplit.getFixedVal());
            return false;
        }
        /*
         * Get the refinement configuration
         */
        Optional<ParameterRefinementConfiguration> paramRefConfigOpt;
        paramRefConfigOpt = registry.getParamRefConfig(component.getComponent(),
                component.getComponent().getParameterWithName(name));

        double minSplitSize;
        int splitCount;
        if(paramRefConfigOpt.isPresent()) {
            minSplitSize = paramRefConfigOpt.get().getIntervalLength();
            splitCount = paramRefConfigOpt.get().getRefinementsPerStep();
        } else {
            // TODO defaults are hardcoded:
            minSplitSize = MIN_RANGE_SIZE;
            splitCount = NUM_SPLITS;
        }
        numericSplit.configureSplits(splitCount, minSplitSize);
        numericSplit.createSplits();
        List<String> splits = numericSplit.getSplits();
        if(splits.isEmpty()){
            if(logger.isTraceEnabled())
                logger.trace("Couldn't split range:{} into split-count:{} with min-size:{}",
                    numericSplit.getPreSplitRange(), splitCount, minSplitSize);
            return false;
        }
        for (String newVal : splits) {
            CIPhase2 newBase = new CIPhase2((CIPhase2) base);
            ComponentInstance refinedInstance = newBase.getComponentByPath(((CIIndexed) component).getPath());
            refinedInstance.getParameterValues().put(name, newVal);
            newBase.copyWitnessEvaluations((CIRoot) base);
            refinements.add(newBase);
        }
        return true;
    }



    @Override
    public boolean refineRequiredInterface(List<ComponentInstance> refinements,
                                           ComponentInstance base,
                                           ComponentInstance component,
                                           String requiredInterfaceName,
                                           String requiredInterface) {
        if(!( component instanceof CIIndexed) ) {
            throw new IllegalArgumentException("Component is not indexed");
        }
        if(!(base instanceof CIPhase1)) {
            throw new IllegalArgumentException("Base component is not of phase 1 type: " + base.getClass().getName());
        }
        List<Component> providersOf = registry.getProvidersOf(requiredInterface);
        if(providersOf.isEmpty()) {
            logger.warn("No component provides interface {}", requiredInterface);
            return true;
        }
        for (Component provider : providersOf) {
            /*
             * Create a refinement for each provider:
             */
            CIPhase1 newBase = new CIPhase1((CIPhase1) base);
            Integer index = newBase.allocateNextIndex();

            String[] parentPath = ((CIIndexed) component).getPath();
            ComponentInstance parent = newBase.getComponentByPath(parentPath);

            InterfaceRefinementRecord refinementRecord =
                    InterfaceRefinementRecord.refineRequiredInterface(parentPath, requiredInterfaceName);
            CIIndexed newInstance = new CIIndexed(provider,
                    new HashMap<>(), new HashMap<>(),
                    index, refinementRecord.getInterfaceRefinementPath());
            parent.getSatisfactionOfRequiredInterfaces().put(requiredInterfaceName, newInstance);
            newBase.addRecord(refinementRecord);
            newBase.copyWitnessEvaluations((CIRoot) base);
            refinements.add(newBase);
        }
        return true;
    }
}
