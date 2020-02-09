package ai.libs.hasco.simplified.std;

import ai.libs.hasco.model.ComponentInstance;

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
