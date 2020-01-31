package ai.libs.hasco.simplified.std;

import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.model.Parameter;

import java.util.*;
import java.util.stream.Collectors;

public class CIPhase2 extends CIIndexed {


    private final CIPhase1 ciPhase1Parent; // TODO remove reference to parent if it isn't needed

    private List<ParamRefinementRecord> paramOrder;

    private int nextParamIndex;

    /**
     * Constructor for creating a <code>ComponentInstance</code> for a particular <code>Component</code>.
     *  @param component                        The component that is grounded.
     * @param parameterValues                  A map containing the parameter values of this grounding.
     * @param satisfactionOfRequiredInterfaces
     * @param nextParamIndex
     */
    private CIPhase2(Component component, Map<String, String> parameterValues, Map<String, ComponentInstance> satisfactionOfRequiredInterfaces, CIPhase1 ciPhase1Parent, Integer index, int nextParamIndex) {
        super(component, parameterValues, satisfactionOfRequiredInterfaces, index);
        this.ciPhase1Parent = Objects.requireNonNull(ciPhase1Parent);
        this.nextParamIndex = nextParamIndex;
    }

    public CIPhase2(CIPhase1 ciPhase1Parent) {
        this(ciPhase1Parent.getComponent(),
                Collections.emptyMap(),
                new HashMap<>(ciPhase1Parent.getSatisfactionOfRequiredInterfaces()),
                ciPhase1Parent,
                ciPhase1Parent.getIndex(), 0);
        performDeepCopy();
    }


    public CIPhase2(CIPhase2 ciPhase2Parent) {
        this(ciPhase2Parent.getComponent(),
                new HashMap<>(ciPhase2Parent.getParameterValues()),
                new HashMap<>(ciPhase2Parent.getSatisfactionOfRequiredInterfaces()),
                ciPhase2Parent.ciPhase1Parent,
                ciPhase2Parent.getIndex(),
                (ciPhase2Parent.nextParamIndex + 1) % ciPhase2Parent.getParamOrder().size());
        this.paramOrder = ciPhase2Parent.getParamOrder();
        performDeepCopy();
    }

    protected List<ParamRefinementRecord> getParamOrder() {
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
        List<CIIndexed> children = getChildren(this.getSatisfactionOfRequiredInterfaces());

        LinkedList<ParamRefinementRecord> orderedParams = createParamOrder(new String[0],
                params, children, 0, indexGuard);

        this.paramOrder = Collections.unmodifiableList(new ArrayList<>(orderedParams)); // wrap for faster access and safety
    }

    private static List<CIIndexed> getChildren(Map<String, ComponentInstance> interfaces) {
        return interfaces.values()
                .stream()
                .map(c -> (CIIndexed) c)
                .collect(Collectors.toList());
    }


    private static LinkedList<ParamRefinementRecord> createParamOrder(String[] path,
                                                                      List<Parameter> parameters,
                                                                      List<CIIndexed> children,
                                                                      Integer index,
                                                                      Set<Integer> indexGuard) {
        if(indexGuard.contains(index)) {
            return new LinkedList<>();
        }
        indexGuard.add(index);
        Deque<ParamRefinementRecord> paramPathList = createParamQueue(parameters, path);
        List<Deque<ParamRefinementRecord>> roundRobinList = new ArrayList<>();
        roundRobinList.add(paramPathList);
        children.stream().map(c -> recursiveSort(c, indexGuard)).forEachOrdered(roundRobinList::add);
        return drawParamRoundRobin(roundRobinList);
    }

    private static LinkedList<ParamRefinementRecord> createParamQueue(List<Parameter> params, String[] path) {
        return params.stream()
                .map(p -> new ParamRefinementRecord(path, p.getName()))
                .collect(Collectors.toCollection(LinkedList::new));
    }


    private static LinkedList<ParamRefinementRecord> recursiveSort(CIIndexed child, Set<Integer> indexGuard) {
        String[] path = child.getPath();
        List<Parameter> params = child.getComponent().getParameters().getLinearization();
        Integer index = child.getIndex();
        List<CIIndexed> grandChildren = getChildren(child.getSatisfactionOfRequiredInterfaces());
        return createParamOrder(path,
                params,
                grandChildren,
                index,
                indexGuard);
    }

    private static LinkedList<ParamRefinementRecord> drawParamRoundRobin(List<Deque<ParamRefinementRecord>> roundRobinList){
        LinkedList<ParamRefinementRecord> result = new LinkedList<>();
        for (Deque<ParamRefinementRecord> queue : roundRobinList) {
            if (!queue.isEmpty())
                result.add(queue.pop());
        }
        return result;
    }

    public ParamRefinementRecord getNextParamToBeRefined() {
        return getParamOrder().get(nextParamIndex);
    }

    public void nextParameter() {
        nextParamIndex = (nextParamIndex + 1)  % getParamOrder().size();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(super.equals(obj)) {
            if(obj instanceof CIPhase2) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    public int getParamIndex() {
        return nextParamIndex;
    }
}
