package ai.libs.hasco.simplified.std;

import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.ComponentInstance;
import ai.libs.jaicore.basic.sets.Pair;

import java.util.*;

public class ComponentIterator {

    //TODO BFS iterator

    public static Iterable<ComponentInstance> bfs(ComponentInstance root) {
        return () -> new Iterator<ComponentInstance>() {

            Set<Integer> indexGuard = new HashSet<>();

            Deque<ComponentInstance> queue = new LinkedList<>();
            {
                queue.add(root);
                checkAndUpdateGuard(indexGuard, root);
            }

            @Override
            public boolean hasNext() {
                return !queue.isEmpty();
            }

            @Override
            public ComponentInstance next() {
                ComponentInstance next = queue.pop();
                next.getSatisfactionOfRequiredInterfaces()
                        .values()
                        .stream()
                        .filter(child -> !checkAndUpdateGuard(indexGuard, child))
                        .forEach(queue::add);
                return next;
            }

        };
    }

    public static Iterable<Pair<ComponentInstance, Optional<ComponentInstance>>> bfsPair
            (ComponentInstance root, ComponentInstance witnessRoot) {
        return () -> new Iterator<Pair<ComponentInstance, Optional<ComponentInstance>>>() {


            Set<Integer> indexGuard = new HashSet<>();

            Deque<Pair<ComponentInstance, Optional<ComponentInstance>>> queue = new LinkedList<>();

            {
                enqueue(root, witnessRoot);
            }

            private void enqueue(ComponentInstance i1, ComponentInstance i1Witness) {
                if(checkAndUpdateGuard(indexGuard, i1)) {
                    return;
                }
                queue.add(new Pair<>(i1, Optional.ofNullable(i1Witness)));
            }

            @Override
            public boolean hasNext() {
                return !queue.isEmpty();
            }

            @Override
            public Pair<ComponentInstance, Optional<ComponentInstance>> next() {
                Pair<ComponentInstance, Optional<ComponentInstance>> nextPair = queue.pop();
                for (Map.Entry<String, ComponentInstance> componentInterface :
                        nextPair.getX().getSatisfactionOfRequiredInterfaces().entrySet()) {
                    ComponentInstance child = componentInterface.getValue();
                    ComponentInstance childWitness;
                    if(nextPair.getY().isPresent()) {
                        childWitness = nextPair.getY().get()
                                .getSatisfactionOfRequiredInterfaces()
                                .get(componentInterface.getKey());
                    } else {
                        childWitness = null;
                    }
                    enqueue(child, childWitness);
                }
                return nextPair;
            }
        };
    }

    private static boolean checkAndUpdateGuard(Set<Integer> guard, ComponentInstance ci) {
        if(ci == null) {
            return true;
        } else if(ci instanceof CIIndexed) {
            Integer index = ((CIIndexed) ci).getIndex();
            if(guard.contains(index)) {
                return true;
            }
            guard.add(index);
            return false;
        } else {
            return false;
        }

    }
}
