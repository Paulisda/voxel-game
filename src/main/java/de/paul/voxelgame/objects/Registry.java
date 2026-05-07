package de.paul.voxelgame.objects;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class Registry<T> {
    private final Map<ResourceId, T> entries = new LinkedHashMap<>();

    public void register(final ResourceId id, final T value) {
        if (entries.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate id: " + id);
        }
        entries.put(id, value);
    }

    public T get(final ResourceId id) {
        T value = entries.get(id);
        if (value == null) {
            throw new IllegalArgumentException("Unknown id: " + id);
        }
        return value;
    }

    public Optional<T> find(final ResourceId id) {
        return Optional.ofNullable(entries.get(id));
    }

    public boolean contains(final ResourceId id) {
        return entries.containsKey(id);
    }

    public Collection<T> values() {
        return Collections.unmodifiableCollection(entries.values());
    }

    public Map<ResourceId, T> entries() {
        return Collections.unmodifiableMap(entries);
    }

    public int size() {
        return entries.size();
    }
}
