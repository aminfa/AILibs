package ai.libs.hasco.simplified.std;

import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.model.Parameter;

import java.util.*;
import java.util.stream.Collectors;

public class CIPhase2 extends IndexedComponentInstance{


    private final CIPhase1 ciPhase1Parent; // TODO remove reference to parent if it isn't needed

    private List<ComponentParamRefinementRecord> paramOrder;

    private int nextParamIndex = 0;

    /**
     * Constructor for creating a <code>ComponentInstance</code> for a particular <code>Component</code>.
     *
     * @param component                        The component that is grounded.
     * @param parameterValues                  A map containing the parameter values of this grounding.
     * @param satisfactionOfRequiredInterfaces
     */
    private CIPhase2(Component component, Map<String, String> parameterValues, Map<String, ComponentInstance> satisfactionOfRequiredInterfaces, CIPhase1 ciPhase1Parent, Integer index) {
        super(component, parameterValues, satisfactionOfRequiredInterfaces, index);
        this.ciPhase1Parent = Objects.requireNonNull(ciPhase1Parent);
    }

    public CIPhase2(CIPhase1 ciPhase1Parent) {
        this(ciPhase1Parent.getComponent(),
                Collections.emptyMap(),
                Collections.unmodifiableMap(ciPhase1Parent.getSatisfactionOfRequiredInterfaces()),
                ciPhase1Parent,
                ciPhase1Parent.getIndex());
    }


    public CIPhase2(CIPhase2 ciPhase2Parent, int nextParamIndex) {
        this(ciPhase2Parent.getComponent(),
                new HashMap<>(ciPhase2Parent.getParameterValues()),
                ciPhase2Parent.getSatisfactionOfRequiredInterfaces(),
                ciPhase2Parent.ciPhase1Parent,
                ciPhase2Parent.getIndex());

        paramOrder = ciPhase2Parent.getParamOrder();
        this.nextParamIndex = nextParamIndex;
    }

    protected List<ComponentParamRefinementRecord> getParamOrder() {
        if(paramOrder == null) {
            createParamOrder();
        }
        return paramOrder;
    }

    private synchronized void createParamOrder() {
        Set<Integer> indexGuard = new HashSet<>();
        List<Parameter> params = this.getComponent()
                .getParameters()
                .getLinearization();
        List<CIChild> children = getChildren(this.getSatisfactionOfRequiredInterfaces());

        LinkedList<ComponentParamRefinementRecord> orderedParams = createParamOrder(new String[0],
                params, children, 0, indexGuard);

        this.paramOrder = Collections.unmodifiableList(new ArrayList<>(orderedParams)); // wrap for faster access and safety
    }

    private static List<CIChild> getChildren(Map<String, ComponentInstance> interfaces) {
        return interfaces.values()
                .stream()
                .map(c -> (CIChild) c)
                .collect(Collectors.toList());
    }


    private static LinkedList<ComponentParamRefinementRecord> createParamOrder(String[] path,
                                               List<Parameter> parameters,
                                               List<CIChild> children,
                                               Integer index,
                                               Set<Integer> indexGuard) {
        if(indexGuard.contains(index)) {
            return new LinkedList<>();
        }
        indexGuard.add(index);
        Deque<ComponentParamRefinementRecord> paramPathList = createParamQueue(parameters, path);
        List<Deque<ComponentParamRefinementRecord>> roundRobinList = new ArrayList<>();
        roundRobinList.add(paramPathList);
        children.stream().map(c -> recursiveSort(c, indexGuard)).forEachOrdered(roundRobinList::add);
        return drawParamRoundRobin(roundRobinList);
    }

    private static LinkedList<ComponentParamRefinementRecord> createParamQueue(List<Parameter> params, String[] path) {
        return params.stream()
                .map(p -> new ComponentParamRefinementRecord(path, p.getName()))
                .collect(Collectors.toCollection(LinkedList::new));
    }


    private static LinkedList<ComponentParamRefinementRecord> recursiveSort(CIChild child, Set<Integer> indexGuard) {
        String[] path = child.getPath();
        List<Parameter> params = child.getComponent().getParameters().getLinearization();
        Integer index = child.getIndex();
        List<CIChild> grandChildren = getChildren(child.getSatisfactionOfRequiredInterfaces());
        return createParamOrder(path,
                params,
                grandChildren,
                index,
                indexGuard);
    }

    private static LinkedList<ComponentParamRefinementRecord> drawParamRoundRobin(List<Deque<ComponentParamRefinementRecord>> roundRobinList){
        LinkedList<ComponentParamRefinementRecord> result = new LinkedList<>();
        for (Deque<ComponentParamRefinementRecord> queue : roundRobinList) {
            if (!queue.isEmpty())
                result.add(queue.pop());
        }
        return result;
    }

}
