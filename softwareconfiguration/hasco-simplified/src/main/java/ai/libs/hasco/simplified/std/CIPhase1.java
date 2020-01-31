package ai.libs.hasco.simplified.std;

import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.ComponentInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public class CIPhase1 extends IndexedComponentInstance implements RoutableCI {

    private static final String[] ROOT_PATH = new String[0];

    private final static Logger logger = LoggerFactory.getLogger(CIPhase1.class);

    private final List<ComponentInterfaceRefinementRecord> refinementHistory;

    private AtomicInteger nextFreeId;

    private CIPhase1(Component component, Map<String, String> parameterValues, Map<String, ComponentInstance> satisfactionOfRequiredInterfaces) {
        super(component,
                Collections.emptyMap(),
                satisfactionOfRequiredInterfaces, 0);
        nextFreeId = new AtomicInteger(1);
        refinementHistory = Collections.EMPTY_LIST;
    }

    public static CIPhase1 createRoot(Component component) {
        logger.info("Creating root component instance of {}.", component.getName());
        return new CIPhase1(component, new HashMap<>(), new HashMap<>());
    }

    public CIPhase1(CIPhase1 baseComponent) {
        super(baseComponent.getComponent(),
                new HashMap<>(baseComponent.getParameterValues()),
                new HashMap<>(baseComponent.getSatisfactionOfRequiredInterfaces()), // deep copy comes later
                baseComponent.getIndex());

        refinementHistory = new ArrayList<>(baseComponent.getRefinementHistory());
        nextFreeId = new AtomicInteger(baseComponent.nextFreeId.get());

        performDeepCopy(baseComponent);
    }

    private void performDeepCopy(CIPhase1 baseComponent) {
        Deque<IndexedComponentInstance> copyWaitingList = new LinkedList<>();
        Map<Integer, IndexedComponentInstance> allCopies = new HashMap<>();
        allCopies.put(getIndex(), this);
        copyWaitingList.add(this);
        while(!copyWaitingList.isEmpty()) {
            IndexedComponentInstance next = copyWaitingList.pop();
            /*
             * Replace all satisfying inner component instances with a copy of them:
             */
            Map<String, ComponentInstance> satMap = next.getSatisfactionOfRequiredInterfaces();
            for(String requiredInterface : satMap.keySet()) {
                ComponentInstance componentInstance = satMap.get(requiredInterface);
                Objects.requireNonNull(componentInstance,
                        String.format("Component of required interface %s was null.", requiredInterface));
                IndexedComponentInstance copiedInstance;
                if(!(componentInstance instanceof IndexedComponentInstance)) {
                    throw new IllegalStateException("A component instance has an unrecognized type: "
                            + componentInstance.getComponent().getName()
                            + ". type: " + componentInstance.getClass().getName());
                }
                Integer index = ((IndexedComponentInstance) componentInstance).getIndex();
                copiedInstance = allCopies.get(index);
                if(copiedInstance == null) {
                    if(componentInstance instanceof CIChild)
                        copiedInstance = new CIChild(componentInstance.getComponent(),
                                new HashMap<>(componentInstance.getParameterValues()),
                                new HashMap<>(componentInstance.getSatisfactionOfRequiredInterfaces()),
                                index,
                                ((CIChild) componentInstance).getPath()
                                );
                    else
                        throw new IllegalStateException("A component instance has an unrecognized type: "
                                + componentInstance.getComponent().getName()
                                + ". type: " + componentInstance.getClass().getName());
                    allCopies.put(index, copiedInstance);
                    copyWaitingList.add(copiedInstance);
                }
                satMap.put(requiredInterface, copiedInstance);
            }
        }

    }

    public List<ComponentInterfaceRefinementRecord> getRefinementHistory() {
        return refinementHistory;
    }

    @Override
    public String[] getPath() {
        return ROOT_PATH;
    }
}
