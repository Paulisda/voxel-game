package de.paul.voxelgame.objects;

import java.util.Map;

public record ModelComponent(
        String[] textureCandidates,
        String[] sideTextureCandidates,
        String[] topTextureCandidates,
        String[] bottomTextureCandidates,
        String tint,
        String shape,
        Map<String, Integer> fallbackColors
) implements Component {
    public ModelComponent(final String texturePath) {
        this(new String[]{texturePath}, null, null, null, "", "cube", Map.of());
    }

    public ModelComponent {
        textureCandidates = normalize(textureCandidates);
        sideTextureCandidates = normalize(sideTextureCandidates);
        topTextureCandidates = normalize(topTextureCandidates);
        bottomTextureCandidates = normalize(bottomTextureCandidates);
        tint = tint == null ? "" : tint.trim().toLowerCase();
        shape = shape == null || shape.isBlank() ? "cube" : shape.trim().toLowerCase();
        fallbackColors = fallbackColors == null ? Map.of() : Map.copyOf(fallbackColors);
    }

    public String[] sideCandidates() {
        return firstNonEmpty(sideTextureCandidates, textureCandidates);
    }

    public String[] topCandidates() {
        return firstNonEmpty(topTextureCandidates, textureCandidates);
    }

    public String[] bottomCandidates() {
        return firstNonEmpty(bottomTextureCandidates, textureCandidates);
    }

    public boolean hasTint(final String expectedTint) {
        return expectedTint != null && tint.equalsIgnoreCase(expectedTint);
    }

    public int fallbackColor(final String key, final int defaultRgba) {
        final Integer keyed = fallbackColors.get(key);
        if (keyed != null) {
            return keyed;
        }
        return fallbackColors.getOrDefault("default", defaultRgba);
    }

    private static String[] firstNonEmpty(final String[] preferred, final String[] fallback) {
        return preferred.length > 0 ? preferred : fallback;
    }

    private static String[] normalize(final String[] values) {
        if (values == null || values.length == 0) {
            return new String[0];
        }

        int count = 0;
        for (final String value : values) {
            if (value != null && !value.isBlank()) {
                count++;
            }
        }

        final String[] normalized = new String[count];
        int index = 0;
        for (final String value : values) {
            if (value != null && !value.isBlank()) {
                normalized[index++] = value.trim();
            }
        }
        return normalized;
    }
}
