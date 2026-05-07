package de.paul.voxelgame.objects;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class GameObject {
    private final ResourceId id;
    private final ObjectKind kind;
    private final Map<Class<? extends Component>, Component> components = new HashMap<>();
    private final Set<String> tags = new HashSet<>();

    public GameObject(final ResourceId id, final ObjectKind kind) {
        this.id = id;
        this.kind = kind;
    }

    public ResourceId id() {
        return id;
    }

    public ObjectKind kind() {
        return kind;
    }

    public <T extends Component> boolean has(final Class<T> type) {
        return components.containsKey(type);
    }

    public <T extends Component> T get(final Class<T> type) {
        Component component = components.get(type);
        if (component == null) {
            throw new IllegalStateException("Missing component: " + type.getSimpleName());
        }
        return type.cast(components.get(type));
    }

    public void add(final Component component) {
        components.put(component.getClass(), component);
    }

    public void addTag(final ResourceId tag) {
        addTag(tag.toString());
    }

    public void addTag(final String tag) {
        if (tag != null && !tag.isBlank()) {
            tags.add(tag.trim());
        }
    }

    public boolean hasTag(final String tag) {
        if (tag == null || tag.isBlank()) {
            return false;
        }
        final String normalized = tag.trim();
        return tags.contains(normalized) || tags.contains(ResourceId.of(normalized).toString());
    }

    public Map<Class<? extends Component>, Component> components() {
        return Collections.unmodifiableMap(components);
    }

    public Set<String> tags() {
        return Collections.unmodifiableSet(tags);
    }

    @Override
    public String toString() {
        return kind + "[" + id + "]";
    }
}
