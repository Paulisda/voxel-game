package de.paul.voxelgame.core;

import de.paul.voxelgame.assets.ResourceManager;
import de.paul.voxelgame.objects.BlockItemComponent;
import de.paul.voxelgame.objects.GameObject;
import de.paul.voxelgame.objects.ObjectKind;
import de.paul.voxelgame.objects.ResourceId;

import java.util.HashMap;
import java.util.Map;

public final class LocalizationManager {
    private static final String LANG_ROOT = "assets/game/lang/";

    private final ResourceManager resources;
    private final Map<Language, Map<String, String>> translations = new HashMap<>();
    private Language currentLanguage = Language.DE_DE;

    public LocalizationManager(final ResourceManager resources) {
        this.resources = resources;
        for (final Language language : Language.values()) {
            translations.put(language, loadLanguage(language));
        }
    }

    public String translate(final String key) {
        final Map<String, String> current = translations.getOrDefault(currentLanguage, Map.of());
        final String translated = current.get(key);
        if (translated != null) {
            return translated;
        }

        final Map<String, String> english = translations.getOrDefault(Language.EN_US, Map.of());
        return english.getOrDefault(key, key);
    }

    public String objectName(final GameObject object) {
        if (object == null) {
            return "";
        }
        if (object.has(BlockItemComponent.class)) {
            return translate(key("block", object.get(BlockItemComponent.class).blockId()));
        }
        if (object.kind() == ObjectKind.BLOCK) {
            return translate(key("block", object.id()));
        }
        if (object.kind() == ObjectKind.ENTITY) {
            return translate(key("entity", object.id()));
        }
        return translate(key("item", object.id()));
    }

    public Language currentLanguage() {
        return currentLanguage;
    }

    public void setLanguage(final Language language) {
        if (language != null) {
            currentLanguage = language;
        }
    }

    public void toggleLanguage() {
        setLanguage(currentLanguage.next());
    }

    public String currentLanguageName() {
        return translate("ui.language." + currentLanguage.code());
    }

    private Map<String, String> loadLanguage(final Language language) {
        final String path = LANG_ROOT + language.code() + ".json";
        final Map<String, String> loaded = new HashMap<>();
        try {
            final Map<String, Object> root = JsonParser.parseObject(resources.readString(path));
            for (final Map.Entry<String, Object> entry : root.entrySet()) {
                loaded.put(entry.getKey(), String.valueOf(entry.getValue()));
            }
        } catch (RuntimeException ignored) {
        }
        return Map.copyOf(loaded);
    }

    private String key(final String prefix, final ResourceId id) {
        return prefix + "." + id.namespace() + "." + id.path().replace('/', '.');
    }
}
