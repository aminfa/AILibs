package ai.libs.hasco.simplified.std;

import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.hasco.model.Parameter;

import java.util.*;
import java.util.stream.Collectors;

public class CIPhase2 extends CIIndexed implements Iterable<ParamRefinementRecord> {


    private final CIPhase1 ciPhase1Parent; // TODO remove reference to parent if it isn't needed

    private List<ParamRefinementRecord> paramOrder;

    private final int nextParamIndex;

    /**
     * Constructor for creating a <code>ComponentInstance</code> for a particular <code>Component</code>.
     *  @param component                        The component that is grounded.
     * @param parameterValues                  A map containing the parameter values of this grounding.
     * @param satisfactionOfRequiredInterfaces
     * @param nextParamIndex
     */
    private CIPhase2(Component component,
                     Map<String, String> parameterValues, Map<String, ComponentInstance> satisfactionOfRequiredInterfaces,
                     CIPhase1 ciPhase1Parent, Integer index,
                     List<ParamRefinementRecord> paramOrder, int nextParamIndex) {
        super(component, parameterValues, satisfactionOfRequiredInterfaces, index);
        this.paramOrder = paramOrder;
        this.ciPhase1Parent = Objects.requireNonNull(ciPhase1Parent);
        if(hasParameters()) {
            this.nextParamIndex = nextParamIndex % getParamOrder().size();
        }  else {
            this.nextParamIndex = 0;
        }

    }

    public CIPhase2(CIPhase1 ciPhase1Parent) {
        this(ciPhase1Parent.getComponent(),
                Collections.emptyMap(),
                new HashMap<>(ciPhase1Parent.getSatisfactionOfRequiredInterfaces()),
                ciPhase1Parent,
                ciPhase1Parent.getIndex(),
                null,
                0);
        performDeepCopy();
    }


    public CIPhase2(CIPhase2 ciPhase2Parent) {
        this(ciPhase2Parent.getComponent(),
                new HashMap<>(ciPhase2Parent.getParameterValues()),
                new HashMap<>(ciPhase2Parent.getSatisfactionOfRequiredInterfaces()),
                ciPhase2Parent.ciPhase1Parent,
                ciPhase2Parent.getIndex(),
                ciPhase2Parent.getParamOrder(),
                (ciPhase2Parent.nextParamIndex + 1));
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
        boolean newEntry = true;
        while(newEntry) {
            newEntry = false;
            for (Deque<ParamRefinementRecord> queue : roundRobinList) {
                if (!queue.isEmpty()) {
                    newEntry = true;
                    result.add(queue.pop());
                }
            }
        }
        return result;
    }

    public ParamRefinementRecord getNextParamToBeRefined() {
        return getParamOrder().get(nextParamIndex);
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

    public Optional<ParamRefinementRecord> getNextRecord() {
        if(hasParameters()) {
            return Optional.of(iterator().next());
        } else {
            return Optional.empty();
        }
    }

    public boolean hasParameters() {
        return !getParamOrder().isEmpty();
    }

    @Override
    public String displayText() {
        return String.format("Phase2::%s[param count: %d]",
                getComponent().getName(),
                getParamOrder().size());
    }

    @Override
    public Iterator<ParamRefinementRecord> iterator() {
        return new Iterator<ParamRefinementRecord>() {
            boolean startedLoop = false;

            int paramCursor = nextParamIndex;

            @Override
            public boolean hasNext() {
                if(!hasParameters()) {
                    return false;
                }
                if(startedLoop) {
                    // loop prevention
                    return paramCursor != nextParamIndex;
                }
                return true;
            }

            @Override
            public ParamRefinementRecord next() {
                if(!hasNext()) {
                    throw new IllegalStateException();
                }
                startedLoop = true;
                ParamRefinementRecord record = getParamOrder().get(paramCursor);
                paramCursor = (paramCursor + 1)  % getParamOrder().size();
                return record;
            }
        };
    }
}
