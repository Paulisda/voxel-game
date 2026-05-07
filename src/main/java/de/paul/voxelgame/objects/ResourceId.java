package de.paul.voxelgame.objects;

public record ResourceId(String namespace, String path) {
    public ResourceId {
        if (namespace == null || namespace.isBlank()) {
            throw new IllegalArgumentException("Resource namespace must not be blank");
        }
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Resource path must not be blank");
        }
        namespace = namespace.trim().toLowerCase();
        path = path.trim().toLowerCase();
    }

    public static ResourceId of(final String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Resource id must not be blank");
        }

        final String normalized = raw.trim();
        final int separator = normalized.indexOf(':');
        if (separator < 0) {
            return new ResourceId("game", normalized);
        }
        if (separator == 0 || separator == normalized.length() - 1) {
            throw new IllegalArgumentException("Invalid resource id: " + raw);
        }
        if (normalized.indexOf(':', separator + 1) >= 0) {
            throw new IllegalArgumentException("Invalid resource id: " + raw);
        }
        return new ResourceId(normalized.substring(0, separator), normalized.substring(separator + 1));
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }
}
