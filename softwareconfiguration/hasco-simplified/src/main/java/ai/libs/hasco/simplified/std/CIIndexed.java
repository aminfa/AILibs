package ai.libs.hasco.simplified.std;

import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.ComponentInstance;

import java.util.*;

public class CIIndexed extends ComponentInstance {

    private final static String[] ROOT_PATH = new String[0];
    private final String[] path;

    private final Integer index;

    /**
     * Constructor for creating a <code>ComponentInstance</code> for a particular <code>Component</code>.
     *  @param component                        The component that is grounded.
     * @param parameterValues                  A map containing the parameter values of this grounding.
     * @param satisfactionOfRequiredInterfaces
     * @param componentIndex
     */
    public CIIndexed(Component component, Map<String, String> parameterValues, Map<String, ComponentInstance> satisfactionOfRequiredInterfaces, Integer componentIndex) {
        super(component, parameterValues, satisfactionOfRequiredInterfaces);
        this.index = componentIndex;
        this.path = ROOT_PATH;
    }

    public CIIndexed(Component component, Map<String, String> parameterValues, Map<String, ComponentInstance> satisfactionOfRequiredInterfaces, Integer index, String[] path) {
        super(component, parameterValues, satisfactionOfRequiredInterfaces);
        this.path = path;
        this.index = index;
    }

    public Integer getIndex() {
        return index;
    }

    public String[] getPath() {
        return path;
    }
    /*
     * TODO move the following methods to a CIBase class which is a parent of the two phases
     */

    /**
     * Only to be used in a root node
     */
    protected void performDeepCopy() {
        Deque<CIIndexed> copyWaitingList = new LinkedList<>();
        Map<Integer, CIIndexed> allCopies = new HashMap<>();
        allCopies.put(getIndex(), this);
        copyWaitingList.add(this);
        while(!copyWaitingList.isEmpty()) {
            CIIndexed next = copyWaitingList.pop();
            /*
             * Replace all satisfying inner component instances with a copy of them:
             */
            Map<String, ComponentInstance> satMap = next.getSatisfactionOfRequiredInterfaces();
            for(String requiredInterface : satMap.keySet()) {
                ComponentInstance componentInstance = satMap.get(requiredInterface);
                Objects.requireNonNull(componentInstance,
                        String.format("Component of required interface %s was null.", requiredInterface));
                CIIndexed copiedInstance;
                if(!(componentInstance instanceof CIIndexed)) {
                    throw new IllegalStateException("A component instance has an unrecognized type: "
                            + componentInstance.getComponent().getName()
                            + ". type: " + componentInstance.getClass().getName());
                }
                Integer index = ((CIIndexed) componentInstance).getIndex();
                copiedInstance = allCopies.get(index);
                if(copiedInstance == null) {
                    copiedInstance = new CIIndexed(componentInstance.getComponent(),
                            new HashMap<>(componentInstance.getParameterValues()),
                            new HashMap<>(componentInstance.getSatisfactionOfRequiredInterfaces()),
                            index,
                            ((CIIndexed) componentInstance).getPath());
                    allCopies.put(index, copiedInstance);
                    copyWaitingList.add(copiedInstance);
                }
                satMap.put(requiredInterface, copiedInstance);
            }
        }

    }

    public ComponentInstance getComponentByPath(String[] path) {
        Map<String, ComponentInstance> providers = getSatisfactionOfRequiredInterfaces();
        ComponentInstance result = this;
        for (int i = 0; i < path.length; i++) {
            String interfaceName = path[i];
            ComponentInstance componentInstance = providers.get(interfaceName);
            if(componentInstance == null) {
                throw new IllegalArgumentException("Couldn't find component with path: " + Arrays.toString(path) +
                        ". reached " + i);
            }
            providers = componentInstance.getSatisfactionOfRequiredInterfaces();
            result = componentInstance;
        }
        return result;
    }
}
