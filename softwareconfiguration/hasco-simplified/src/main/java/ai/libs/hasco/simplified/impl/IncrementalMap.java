package ai.libs.hasco.simplified.impl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class IncrementalMap<K, V> implements Map<K,V> {

    private final Map<K, V> baseMap;

    private final Map<K, V> newEntries;

    public IncrementalMap(Map<K, V> baseMap, Map<K, V> newEntries) {
        this.baseMap = baseMap;
        this.newEntries = newEntries;
    }

    public IncrementalMap(Map<K, V> baseMap) {
        this(baseMap, new HashMap<>());
    }

    @Override
    public int size() {
        return baseMap.size() + newEntries.size();
    }

    @Override
    public boolean isEmpty() {
        return baseMap.isEmpty() || newEntries.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return baseMap.containsKey(key) || newEntries.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return baseMap.containsValue(value) || newEntries.containsValue(value);
    }

    @Override
    public V get(Object key) {
        if(newEntries.containsKey(key)) {
            return newEntries.get(key);
        } else {
            return baseMap.get(key);
        }
    }

    @Nullable
    @Override
    public V put(K key, V value) {
        if(baseMap.containsKey(key)) {
            throw new IllegalStateException("The key was already present in the base map.");
        }
        return newEntries.put(key, value);
    }

    @Override
    public V remove(Object key) {
        return newEntries.remove(key);
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        m.forEach(this::put);
    }

    @Override
    public void clear() {
        throw new IllegalStateException("Cannot clear this implementation of the map.");
    }

    @NotNull
    @Override
    public Set<K> keySet() {
        Set<K> union = new HashSet<>(baseMap.keySet());
        union.addAll(newEntries.keySet());
        return union;
    }

    @NotNull
    @Override
    public Collection<V> values() {
        Collection<V> collection = new ArrayList<>(baseMap.values());
        collection.addAll(baseMap.values());
        return collection;
    }

    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Entry<K, V>> union = new HashSet<>(baseMap.entrySet());
        union.addAll(newEntries.entrySet());
        return union;
    }
}
