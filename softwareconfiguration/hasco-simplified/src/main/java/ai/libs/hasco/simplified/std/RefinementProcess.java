package ai.libs.hasco.simplified.std;

import ai.libs.hasco.model.*;
import ai.libs.hasco.simplified.RefinementService;
import ai.libs.hasco.simplified.SimpleHASCOConfig;
import org.aeonbits.owner.ConfigCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RefinementProcess {

    private final static Logger logger = LoggerFactory.getLogger(RefinementProcess.class);

    private ComponentInstance base;

    private List<ComponentInstance> refinements;

    private String displayName;

    private ComponentInstance nextComponentToBeRefined = null;

    private final boolean publishNodes;

    {
        SimpleHASCOConfig config = ConfigCache.get(SimpleHASCOConfig.class);
        publishNodes = config.areNodesPublished();
    }

    public RefinementProcess(ComponentInstance base) {
        this.base = base;
        this.refinements = new ArrayList<>();
        displayName = "Component refinement";
    }

    public String displayText() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    // TODO refactor this method out of this class. Its not really the responsibility of this class to interpret and search along a path.
    public boolean setComponentToBeRefined(List<String> componentPath) {
        ComponentInstance componentInstance;
        try {
            componentInstance = searchSatisfyingComponent(componentPath);
            Objects.requireNonNull(componentInstance);
        } catch (IllegalArgumentException | NullPointerException ex) {
            logger.warn("{} - No component found for path: {}", displayName, componentPath, ex);
            return false;
        }
        logger.trace("The next component to be refined is on path: {}", componentPath);
        this.nextComponentToBeRefined = (componentInstance);
        setDisplayName(String.format("Refinement of component %s > [%s]", componentPath, nextComponentToBeRefined.getComponent().getName()));
        return true;
    }

    public boolean setComponentToBeRefined(ComponentInstance nextComponentToBeRefined) {
        this.nextComponentToBeRefined = nextComponentToBeRefined;
        logger.trace("The next component to be refined is a: {}", nextComponentToBeRefined.getComponent().getName());
        if(nextComponentToBeRefined instanceof CIIndexed)
            setDisplayName(
                    String.format("Refinement of %s", ((CIIndexed) nextComponentToBeRefined).displayText())
            );
        else
            setDisplayName(String.format("Refinement of component [%s]", nextComponentToBeRefined.getComponent().getName()));

        return true;
    }

    public ComponentInstance getNextComponentToBeRefined() {
        return nextComponentToBeRefined;
    }

    public boolean isInitialized() {
        return nextComponentToBeRefined != null;
    }

    private void assertInitialized() {
        if(!isInitialized()) {
            logger.error("{} - the next component instance to be refined has not been set. " +
                    "First call RefinementProcess::setComponentToBeRefined.", displayName);
            throw new IllegalStateException("Not initialized.");
        }
    }

    public boolean refineRequiredInterface(String requiredInterfaceId,
                                           RefinementService refiner) {
        assertInitialized();
        setDisplayName(String.format("%s required interface %s", displayText(), requiredInterfaceId));
        List<ComponentInstance> children = new ArrayList<>();
        Component baseComponent = nextComponentToBeRefined.getComponent();
        if(baseComponent.getRequiredInterfaces().containsKey(requiredInterfaceId)) {
            String requiredInterface = baseComponent.getRequiredInterfaces().get(requiredInterfaceId);
            if(requiredInterface == null) {
                logger.error("{} - Component {} has a null entry in the required interface map: {} -> {}",
                        displayName, baseComponent.getName(), requiredInterfaceId, null);
                throw new NullPointerException("The required interface is null: " + requiredInterfaceId);
            }
            boolean refinementDone = refiner.refineRequiredInterface(children, base,
                    nextComponentToBeRefined, requiredInterfaceId, requiredInterface);
            if(refinementDone && children.isEmpty()){
                logger.warn("{} - The refinement " +
                        "returned no candidates but signaled that it is finished.",
                        displayName);
            }
            this.refinements.addAll(children);
            if(refinementDone) {
                if(logger.isDebugEnabled())
                    logger.debug("{} - " +
                                    " The required interface {}:{} has been refined with {} many candidates.",
                            displayName,
                            requiredInterfaceId, requiredInterface, children.size());
                return true;
            } else {

                return false;
            }
        } else {
            logger.warn("{} - cannot refine the required interface {} as it is not declared by the component.",
                    displayName,
                    requiredInterfaceId);
            return false;
        }
    }

    public boolean refineParameter(String paramName,
                                   RefinementService refiner) {
        assertInitialized();
        setDisplayName(String.format("%s refine param %s", displayText(), paramName));
        List<ComponentInstance> children = new ArrayList<>();
        Component componentType = nextComponentToBeRefined.getComponent();
        Parameter paramToBeRefined = componentType.getParameterWithName(paramName);
        if (paramToBeRefined == null) {
            logger.error("{} - Parameter {}.{} was null.", displayName, componentType.getName(), paramName);
            throw new NullPointerException(String.format("Parameter is null: %s.%s", componentType.getName(), paramName));
        }
        String parameterValue = nextComponentToBeRefined.getParameterValue(paramToBeRefined);
        boolean refinementDone = false;
        if (paramToBeRefined.isNumeric()) {
            refinementDone = refiner.refineNumericalParameter(children, base,
                    nextComponentToBeRefined,
                    (NumericParameterDomain) paramToBeRefined.getDefaultDomain(),
                    paramName, parameterValue);
        } else if (paramToBeRefined.isCategorical()) {
            refinementDone = refiner.refineCategoricalParameter(children, base,
                    nextComponentToBeRefined,
                    (CategoricalParameterDomain) paramToBeRefined.getDefaultDomain(), paramName, parameterValue);
        } else {
            logger.error("Parameter {}.{} is neither numerical nor categorical. Ignoring parameter", componentType.getName(), paramName);
            return false;
        }
        refinements.addAll(children);
        if (refinementDone) {
            if (logger.isTraceEnabled())
                logger.trace("{} - " +
                                "The refinement of parameter {}.{} has yielded {} many candidates.",
                        displayName,
                        componentType.getName(), paramName, children.size());
        } else if(!children.isEmpty()) {
            logger.warn("{} - The refinement of parameter {}.{} returned no new candidates but signaled that it is did.",
                    displayName,
                    componentType.getName(), paramName);
        }
        return refinementDone;
    }
    private ComponentInstance searchSatisfyingComponent(List<String> componentPath) {
        if(componentPath == null || componentPath.isEmpty()) {
            return base;
        }
        ComponentInstance trg = base, prev = null;

        for(String requiredInterfaceName : componentPath) {
            prev = trg;
            trg = getSatisfyingComponent(prev, requiredInterfaceName);
            if(trg == null) {
                if(prev.getComponent().getRequiredInterfaces().containsKey(requiredInterfaceName))
                    throw new IllegalArgumentException("ComponentInstance with path" + componentPath + " not found.\n" +
                            "Component Instance " + prev.getComponent().getName()
                            + " doesn't have the required interface called: " + requiredInterfaceName +
                            " satisfied.");
                else
                    throw new IllegalArgumentException("ComponentInstance with path" + componentPath + " not found.\n" +
                            "Component Instance " + prev.getComponent().getName()
                            + " doesn't have a required interface called: " + requiredInterfaceName);
            }
        }
        return trg;
    }

    private ComponentInstance getSatisfyingComponent(ComponentInstance base, String requiredInterfaceName) {
        ComponentInstance component = base.getSatisfactionOfRequiredInterfaces().get(requiredInterfaceName);
        return component;
    }

    public List<ComponentInstance> getRefinements() {
        if(publishNodes) {
            refinements.forEach(ci -> ((CIRoot) ci).setParentNode((CIRoot) base));
        }
        return refinements;
    }
}
