package ai.libs.hasco.simplified;

import ai.libs.hasco.model.Component;
import ai.libs.hasco.model.Parameter;
import ai.libs.hasco.model.ParameterRefinementConfiguration;
import ai.libs.hasco.serialization.ComponentLoader;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class ComponentRegistry {


    private static ComponentRegistry EMPTY_REGISTRY = new ComponentRegistry(Collections.emptyList());

    private final List<Component> componentList;

    private final Map<Component, Map<Parameter, ParameterRefinementConfiguration>> paramConfigs;

    // TODO add config

    private final Map<String, List<Component>> providerCache = new ConcurrentHashMap<>();

    private ComponentRegistry(List<Component> componentList) {
        this.componentList = componentList;
        this.paramConfigs = new HashMap<>();
    }

    public static ComponentRegistry fromComponentLoader(ComponentLoader loader) {
        ComponentRegistry registry = new ComponentRegistry(new ArrayList<>(loader.getComponents()));
        registry.paramConfigs.putAll(loader.getParamConfigs());
        return registry;
    }

    public static ComponentRegistry emptyRegistry() {
        return EMPTY_REGISTRY;
    }

    public List<Component> getProvidersOf(String requiredInterface) {
        Objects.requireNonNull(requiredInterface);
        return providerCache.computeIfAbsent(requiredInterface, this::lookupProvidersOf);
    }

    public Optional<ParameterRefinementConfiguration> getParamRefConfig(Component component, Parameter param) {
        return Optional.ofNullable(paramConfigs.get(component)).map(innerMap -> innerMap.get(param));
    }

    private List<Component> lookupProvidersOf(String requiredInterface) {
        return componentList.stream()
                .filter(component -> component.getProvidedInterfaces().contains(requiredInterface))
                .collect(Collectors.toList());
    }

}
