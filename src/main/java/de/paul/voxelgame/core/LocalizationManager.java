package de.paul.voxelgame.core;

import de.paul.voxelgame.assets.ResourceManager;
import de.paul.voxelgame.objects.BlockItemComponent;
import de.paul.voxelgame.objects.GameObject;
import de.paul.voxelgame.objects.ObjectKind;
import de.paul.voxelgame.objects.ResourceId;

import java.util.HashMap;
import java.util.Map;
import java.util.Locale;

public final class LocalizationManager {
    private static final String LANG_ROOT = "assets/game/lang/";
    private static final Map<String, String> DE_WORDS = Map.ofEntries(
            Map.entry("acacia", "Akazien"),
            Map.entry("amethyst", "Amethyst"),
            Map.entry("arrow", "Pfeil"),
            Map.entry("axe", "Axt"),
            Map.entry("baked", "Gebackene"),
            Map.entry("beef", "Rindfleisch"),
            Map.entry("black", "Schwarz"),
            Map.entry("blue", "Blau"),
            Map.entry("boat", "Boot"),
            Map.entry("bone", "Knochen"),
            Map.entry("book", "Buch"),
            Map.entry("boots", "Stiefel"),
            Map.entry("bow", "Bogen"),
            Map.entry("bread", "Brot"),
            Map.entry("brown", "Braun"),
            Map.entry("bucket", "Eimer"),
            Map.entry("carrot", "Karotte"),
            Map.entry("chest", "Truhen"),
            Map.entry("chestplate", "Brustpanzer"),
            Map.entry("chicken", "Haehnchen"),
            Map.entry("clock", "Uhr"),
            Map.entry("coal", "Kohle"),
            Map.entry("cod", "Kabeljau"),
            Map.entry("compass", "Kompass"),
            Map.entry("cooked", "Gebratenes"),
            Map.entry("copper", "Kupfer"),
            Map.entry("crossbow", "Armbrust"),
            Map.entry("cyan", "Tuerkis"),
            Map.entry("diamond", "Diamant"),
            Map.entry("dye", "Farbstoff"),
            Map.entry("emerald", "Smaragd"),
            Map.entry("fishing", "Angel"),
            Map.entry("flint", "Feuerstein"),
            Map.entry("gold", "Gold"),
            Map.entry("golden", "Gold"),
            Map.entry("gray", "Grau"),
            Map.entry("green", "Gruen"),
            Map.entry("helmet", "Helm"),
            Map.entry("hoe", "Hacke"),
            Map.entry("horse", "Pferde"),
            Map.entry("ingot", "Barren"),
            Map.entry("iron", "Eisen"),
            Map.entry("lapis", "Lapis"),
            Map.entry("lava", "Lava"),
            Map.entry("leather", "Leder"),
            Map.entry("leggings", "Beinschutz"),
            Map.entry("light", "Hell"),
            Map.entry("lime", "Limette"),
            Map.entry("magenta", "Magenta"),
            Map.entry("map", "Karte"),
            Map.entry("melon", "Melone"),
            Map.entry("mutton", "Hammelfleisch"),
            Map.entry("netherite", "Netherit"),
            Map.entry("nugget", "Nugget"),
            Map.entry("orange", "Orange"),
            Map.entry("pickaxe", "Spitzhacke"),
            Map.entry("pink", "Rosa"),
            Map.entry("porkchop", "Schweinefleisch"),
            Map.entry("potato", "Kartoffel"),
            Map.entry("purple", "Lila"),
            Map.entry("rabbit", "Kaninchen"),
            Map.entry("raw", "Rohes"),
            Map.entry("recovery", "Bergungs"),
            Map.entry("red", "Rot"),
            Map.entry("salmon", "Lachs"),
            Map.entry("sapling", "Setzling"),
            Map.entry("seeds", "Samen"),
            Map.entry("shard", "Scherbe"),
            Map.entry("shears", "Schere"),
            Map.entry("shell", "Panzer"),
            Map.entry("shovel", "Schaufel"),
            Map.entry("soup", "Suppe"),
            Map.entry("spawn", "Spawn"),
            Map.entry("stick", "Stock"),
            Map.entry("stone", "Stein"),
            Map.entry("sword", "Schwert"),
            Map.entry("water", "Wasser"),
            Map.entry("white", "Weiss"),
            Map.entry("wooden", "Holz"),
            Map.entry("yellow", "Gelb")
    );

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
        final String englishFallback = english.get(key);
        if (englishFallback != null) {
            return englishFallback;
        }

        return generatedObjectName(key);
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

    private String generatedObjectName(final String key) {
        final String itemPrefix = "item.game.";
        final String blockPrefix = "block.game.";
        final String entityPrefix = "entity.game.";
        if (key.startsWith(itemPrefix)) {
            return generatedDisplayName(key.substring(itemPrefix.length()));
        }
        if (key.startsWith(blockPrefix)) {
            return generatedDisplayName(key.substring(blockPrefix.length()));
        }
        if (key.startsWith(entityPrefix)) {
            return generatedDisplayName(key.substring(entityPrefix.length()));
        }
        return key;
    }

    private String generatedDisplayName(final String path) {
        final String[] parts = path.replace('.', '_').split("_");
        final StringBuilder name = new StringBuilder();
        for (final String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (name.length() > 0) {
                name.append(' ');
            }
            name.append(currentLanguage == Language.DE_DE
                    ? DE_WORDS.getOrDefault(part, title(part))
                    : title(part));
        }
        return name.toString();
    }

    private String title(final String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT)
                + value.substring(1).toLowerCase(Locale.ROOT);
    }
}
