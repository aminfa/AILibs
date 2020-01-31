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
public class CIPhase1 extends CIIndexed {

    private final static Logger logger = LoggerFactory.getLogger(CIPhase1.class);

    private final List<InterfaceRefinementRecord> refinementHistory;

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

        performDeepCopy();
    }

    public List<InterfaceRefinementRecord> getRefinementHistory() {
        return refinementHistory;
    }

    public Integer allocateNextIndex() {
        return nextFreeId.getAndIncrement();
    }

    public void addRecord(InterfaceRefinementRecord refinementRecord) {
        refinementHistory.add(refinementRecord);
    }


    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(super.equals(obj)) {
            if(obj instanceof CIPhase1) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }
}
