package ai.libs.hasco.simplified;

import ai.libs.hasco.model.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ComponentRegistry {

    private final List<Component> componentList;

    private final Map<String, List<Component>> providerCache = new ConcurrentHashMap<>();

    public ComponentRegistry(List<Component> componentList) {
        this.componentList = componentList;
    }

    public List<Component> getProvidersOf(String requiredInterface) {
        return providerCache.computeIfAbsent(requiredInterface, this::lookupProvidersOf);
    }

    private List<Component> lookupProvidersOf(String requiredInterface) {
        return componentList.stream()
                .filter(component -> component.getProvidedInterfaces().contains(requiredInterface))
                .collect(Collectors.toList());
    }

}
