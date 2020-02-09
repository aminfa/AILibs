package ai.libs.hasco.simplified.std;

import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.ComponentInstance;

import java.util.*;

public class CIRoot extends CIIndexed {

    private EvalReport evalReport = null;

    public CIRoot(Component component,
                  Map<String, String> parameterValues,
                  Map<String, ComponentInstance> satisfactionOfRequiredInterfaces,
                  Integer componentIndex) {
        super(component, parameterValues, satisfactionOfRequiredInterfaces, componentIndex);
    }

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

    public ComponentInstance getComponentByPath(String[] pathArr) {
        return getComponentByPath(Arrays.stream(pathArr).iterator());
    }

    public ComponentInstance getComponentByPath(Iterable<String> componentPath) {
        return getComponentByPath(componentPath.iterator());
    }

    public ComponentInstance getComponentByPath(Iterator<String> componentPath) {
        if(componentPath == null) {
            return this;
        }
        ComponentInstance current = this, prev = null;
        while(componentPath.hasNext()) {
            String requiredInterfaceName = componentPath.next();
            prev = current;
            current = getSatisfyingComponent(prev, requiredInterfaceName);
            if(current == null) {
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
        return current;
    }

//    public ComponentInstance getComponentByPath(String[] path) {
//        Map<String, ComponentInstance> providers = getSatisfactionOfRequiredInterfaces();
//        ComponentInstance result = this;
//        for (int i = 0; i < path.length; i++) {
//            String interfaceName = path[i];
//            ComponentInstance componentInstance = providers.get(interfaceName);
//            if(componentInstance == null) {
//                throw new IllegalArgumentException("Couldn't find component with path: " + Arrays.toString(path) +
//                        ". reached " + i);
//            }
//            providers = componentInstance.getSatisfactionOfRequiredInterfaces();
//            result = componentInstance;
//        }
//        return result;
//    }


    private ComponentInstance getSatisfyingComponent(ComponentInstance base, String requiredInterfaceName) {
        ComponentInstance component = base.getSatisfactionOfRequiredInterfaces().get(requiredInterfaceName);
        return component;
    }

    public boolean hasBeenEvaluated() {
        return evalReport != null;
    }

    public EvalReport getEvalReport() {
        return evalReport;
    }

    public void setEvalReport(EvalReport evalReport) {
        this.evalReport = evalReport;
    }
}
