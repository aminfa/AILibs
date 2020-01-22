package ai.libs.hasco.simplified.impl;

import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.ComponentInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public class RootComponentInstance extends IndexedComponentInstance {

    private final static Logger logger = LoggerFactory.getLogger(RootComponentInstance.class);

    private final List<ComponentRefinementRecord> refinementHistory;

    private AtomicInteger nextFreeId;

    private RootComponentInstance(Component component, Map<String, String> parameterValues, Map<String, ComponentInstance> satisfactionOfRequiredInterfaces) {
        super(component, parameterValues, satisfactionOfRequiredInterfaces, 0);
        nextFreeId = new AtomicInteger(1);
        refinementHistory = Collections.EMPTY_LIST;
    }

    public static RootComponentInstance createRoot(Component component) {
        logger.info("Creating root component instance of {}.", component.getName());
        return new RootComponentInstance(component, new HashMap<>(), new HashMap<>());
    }



    public RootComponentInstance(RootComponentInstance baseComponent) {
        super(baseComponent.getComponent(),
                new HashMap<>(baseComponent.getParameterValues()),
                new HashMap<>(baseComponent.getSatisfactionOfRequiredInterfaces()), // deep copy comes later
                baseComponent.getIndex());

        refinementHistory = new ArrayList<>(baseComponent.getRefinementHistory());
        nextFreeId = new AtomicInteger(baseComponent.nextFreeId.get());

        performDeepCopy(baseComponent);
    }

    private void performDeepCopy(RootComponentInstance baseComponent) {
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
                    if(componentInstance instanceof ChildComponentInstance)
                        copiedInstance = new ChildComponentInstance(componentInstance.getComponent(),
                                new HashMap<>(componentInstance.getParameterValues()),
                                new HashMap<>(componentInstance.getSatisfactionOfRequiredInterfaces()),
                                index,
                                ((ChildComponentInstance) componentInstance).getPath()
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



    public List<ComponentRefinementRecord> getRefinementHistory() {
        return refinementHistory;
    }



}
