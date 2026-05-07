package de.paul.voxelgame.core;

import de.paul.voxelgame.assets.ResourceManager;
import de.paul.voxelgame.debug.GameDebug;
import de.paul.voxelgame.objects.BlockComponent;
import de.paul.voxelgame.objects.BlockItemComponent;
import de.paul.voxelgame.objects.DurabilityComponent;
import de.paul.voxelgame.objects.EntityComponent;
import de.paul.voxelgame.objects.FoodComponent;
import de.paul.voxelgame.objects.GameObject;
import de.paul.voxelgame.objects.ModelComponent;
import de.paul.voxelgame.objects.ObjectKind;
import de.paul.voxelgame.objects.Registry;
import de.paul.voxelgame.objects.RegistryManager;
import de.paul.voxelgame.objects.ResourceId;
import de.paul.voxelgame.objects.SpawnEntityComponent;
import de.paul.voxelgame.objects.StackComponent;
import de.paul.voxelgame.objects.WeaponComponent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ContentLoader {
    private static final String ASSET_ROOT = "assets/game";

    private final RegistryManager registries;
    private final ResourceManager resources;

    public ContentLoader(final RegistryManager registries, final ResourceManager resources) {
        this.registries = registries;
        this.resources = resources;
    }

    public void loadAll() {
        loadBlocks();
        loadItems();
        createMissingBlockItems();
        loadEntities();
        loadTags("blocks", registries.blocks());
        loadTags("items", registries.items());
        loadTags("entities", registries.entities());

        GameDebug.info("Content loaded: "
                + registries.blocks().size() + " blocks, "
                + registries.items().size() + " items, "
                + registries.entities().size() + " entities");
    }

    private void loadBlocks() {
        loadObjects("blocks", ObjectKind.BLOCK, registries.blocks());
    }

    private void loadItems() {
        loadObjects("items", ObjectKind.ITEM, registries.items());
    }

    private void loadEntities() {
        loadObjects("entities", ObjectKind.ENTITY, registries.entities());
    }

    private void loadObjects(final String folder, final ObjectKind kind, final Registry<GameObject> registry) {
        for (final String resourcePath : resources.listJsonResources(ASSET_ROOT + "/" + folder)) {
            final String json = resources.readString(resourcePath).trim();
            if (json.isEmpty()) {
                continue;
            }

            final Map<String, Object> root = JsonParser.parseObject(json);
            final ResourceId id = ResourceId.of(string(root, "id", inferId(resourcePath, folder)));
            final GameObject gameObject = new GameObject(id, kind);

            final Map<String, Object> components = object(root.get("components"));
            loadComponents(gameObject, components);

            registry.register(id, gameObject);
        }
    }

    private void loadComponents(final GameObject gameObject, final Map<String, Object> components) {
        if (components.containsKey("block")) {
            final Map<String, Object> block = object(components.get("block"));
            gameObject.add(new BlockComponent(
                    bool(block, "solid", true),
                    number(block, "hardness", 1.0f),
                    number(block, "resistance", 1.0f),
                    bool(block, "breakable", true)
            ));
        }

        if (components.containsKey("stack")) {
            final Map<String, Object> stack = object(components.get("stack"));
            gameObject.add(new StackComponent(integer(stack, "max", integer(stack, "maxStackSize", 64))));
        }

        if (components.containsKey("food")) {
            final Map<String, Object> food = object(components.get("food"));
            gameObject.add(new FoodComponent(
                    integer(food, "hunger", 0),
                    number(food, "saturation", 0.0f)
            ));
        }

        final Object blockItemComponent = firstPresent(components, "blockItem", "block_item");
        if (blockItemComponent != null) {
            final Map<String, Object> blockItem = object(blockItemComponent);
            gameObject.add(new BlockItemComponent(ResourceId.of(string(blockItem, "block", gameObject.id().toString()))));
        }

        final Object spawnEntityComponent = firstPresent(components, "spawnEntity", "spawn_entity");
        if (spawnEntityComponent != null) {
            final Map<String, Object> spawnEntity = object(spawnEntityComponent);
            gameObject.add(new SpawnEntityComponent(ResourceId.of(string(spawnEntity, "entity", "game:zombie"))));
        }

        if (components.containsKey("weapon")) {
            final Map<String, Object> weapon = object(components.get("weapon"));
            gameObject.add(new WeaponComponent(
                    number(weapon, "damage", 1.0f),
                    number(weapon, "attackSpeed", 1.0f)
            ));
        }

        if (components.containsKey("durability")) {
            final Map<String, Object> durability = object(components.get("durability"));
            gameObject.add(new DurabilityComponent(integer(durability, "max", integer(durability, "maxDurability", 1))));
        }

        if (components.containsKey("entity")) {
            final Map<String, Object> entity = object(components.get("entity"));
            gameObject.add(new EntityComponent(
                    integer(entity, "maxHealth", 20),
                    number(entity, "width", 0.6f),
                    number(entity, "height", 1.8f)
            ));
        }

        if (components.containsKey("model")) {
            gameObject.add(readModel(gameObject.id(), object(components.get("model"))));
        }
    }

    private void createMissingBlockItems() {
        for (final GameObject block : registries.blocks().values()) {
            final GameObject existingItem = registries.items().find(block.id()).orElse(null);
            if (existingItem != null) {
                if (!existingItem.has(BlockItemComponent.class)) {
                    existingItem.add(new BlockItemComponent(block.id()));
                }
                if (!existingItem.has(StackComponent.class)) {
                    existingItem.add(new StackComponent(64));
                }
                if (!existingItem.has(ModelComponent.class) && block.has(ModelComponent.class)) {
                    existingItem.add(block.get(ModelComponent.class));
                }
                existingItem.addTag("game:block_item");
                continue;
            }

            final GameObject item = new GameObject(block.id(), ObjectKind.ITEM);
            item.add(new StackComponent(64));
            item.add(new BlockItemComponent(block.id()));
            if (block.has(ModelComponent.class)) {
                item.add(block.get(ModelComponent.class));
            } else {
                item.add(new ModelComponent(block.id().path()));
            }
            item.addTag("game:block_item");
            registries.items().register(item.id(), item);
        }
    }

    private ModelComponent readModel(final ResourceId id, final Map<String, Object> model) {
        final Map<String, Object> textures = object(model.get("textures"));
        final Map<String, Object> fallback = object(model.get("fallback"));
        final Map<String, Integer> fallbackColors = new LinkedHashMap<>();
        for (final Map.Entry<String, Object> entry : fallback.entrySet()) {
            fallbackColors.put(entry.getKey(), color(entry.getValue()));
        }

        return new ModelComponent(
                stringArray(model.get("texture")),
                stringArray(textures.get("side")),
                stringArray(textures.get("top")),
                stringArray(textures.get("bottom")),
                string(model, "tint", ""),
                string(model, "shape", inferMinecraftShape(id)),
                fallbackColors
        );
    }

    private String inferMinecraftShape(final ResourceId id) {
        final String path = id.path();
        if (path.equals("redstone_wire")) {
            return "redstone_wire";
        }
        if (path.equals("amethyst_cluster") || path.equals("small_amethyst_bud")
                || path.equals("medium_amethyst_bud") || path.equals("large_amethyst_bud")) {
            return "cross";
        }
        if (path.endsWith("_torch") || path.equals("torch") || path.equals("redstone_torch") || path.equals("soul_torch")) {
            return "torch";
        }
        if (path.equals("wheat_stage") || path.equals("carrot") || path.equals("potato") || path.equals("beetroot_stage")
                || path.equals("pitcher_crop") || path.equals("melon_stem") || path.equals("pumpkin_stem")
                || path.equals("attached_melon_stem") || path.equals("attached_pumpkin_stem")) {
            return "crop";
        }
        if (path.contains("sapling") || path.equals("grass") || path.equals("tall_grass")
                || path.equals("fern") || path.equals("large_fern") || path.endsWith("_tulip")
                || path.equals("poppy") || path.equals("blue_orchid") || path.equals("allium")
                || path.equals("azure_bluet") || path.equals("oxeye_daisy") || path.equals("cornflower")
                || path.equals("lily_of_the_valley") || path.equals("brown_mushroom") || path.equals("red_mushroom")
                || path.equals("crimson_roots") || path.equals("warped_roots") || path.equals("hanging_roots")
                || path.equals("sugar_cane") || path.equals("sunflower") || path.equals("flowering_trail")) {
            return "cross";
        }
        return "cube";
    }

    private void loadTags(final String folder, final Registry<GameObject> registry) {
        for (final String resourcePath : resources.listJsonResources(ASSET_ROOT + "/tags/" + folder)) {
            final String json = resources.readString(resourcePath).trim();
            if (json.isEmpty()) {
                continue;
            }

            final Map<String, Object> root = JsonParser.parseObject(json);
            final ResourceId tagId = ResourceId.of(inferTagId(resourcePath, folder));
            for (final String rawValue : stringList(root.get("values"))) {
                if (rawValue.startsWith("#")) {
                    continue;
                }

                final ResourceId valueId = ResourceId.of(rawValue);
                registry.find(valueId).ifPresentOrElse(
                        object -> object.addTag(tagId),
                        () -> GameDebug.info("Ignoring unknown tag value " + valueId + " in " + tagId)
                );
            }
        }
    }

    private String inferId(final String resourcePath, final String folder) {
        final String prefix = ASSET_ROOT + "/" + folder + "/";
        String path = resourcePath.substring(prefix.length(), resourcePath.length() - ".json".length());
        path = path.replace('\\', '/');
        return "game:" + path;
    }

    private String inferTagId(final String resourcePath, final String folder) {
        final String prefix = ASSET_ROOT + "/tags/" + folder + "/";
        String path = resourcePath.substring(prefix.length(), resourcePath.length() - ".json".length());
        path = path.replace('\\', '/');
        return "game:" + path;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> object(final Object value) {
        if (value == null) {
            return Map.of();
        }
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Expected JSON object but got " + value.getClass().getSimpleName());
        }
        return (Map<String, Object>) map;
    }

    private List<String> stringList(final Object value) {
        final List<String> result = new ArrayList<>();
        if (value == null) {
            return result;
        }
        if (!(value instanceof List<?> list)) {
            result.add(String.valueOf(value));
            return result;
        }
        for (final Object item : list) {
            result.add(String.valueOf(item));
        }
        return result;
    }

    private String[] stringArray(final Object value) {
        final List<String> values = stringList(value);
        return values.toArray(String[]::new);
    }

    private String string(final Map<String, Object> object, final String key, final String defaultValue) {
        final Object value = object.get(key);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private Object firstPresent(final Map<String, Object> object, final String... keys) {
        for (final String key : keys) {
            if (object.containsKey(key)) {
                return object.get(key);
            }
        }
        return null;
    }

    private int integer(final Map<String, Object> object, final String key, final int defaultValue) {
        final Object value = object.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private float number(final Map<String, Object> object, final String key, final float defaultValue) {
        final Object value = object.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.floatValue();
        }
        return Float.parseFloat(String.valueOf(value));
    }

    private boolean bool(final Map<String, Object> object, final String key, final boolean defaultValue) {
        final Object value = object.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private int color(final Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }

        String raw = String.valueOf(value).trim();
        if (raw.startsWith("#")) {
            raw = raw.substring(1);
        }
        if (raw.length() == 6) {
            raw += "ff";
        }
        if (raw.length() != 8) {
            throw new IllegalArgumentException("Expected #RRGGBB or #RRGGBBAA color but got " + value);
        }

        final int r = Integer.parseInt(raw.substring(0, 2), 16);
        final int g = Integer.parseInt(raw.substring(2, 4), 16);
        final int b = Integer.parseInt(raw.substring(4, 6), 16);
        final int a = Integer.parseInt(raw.substring(6, 8), 16);
        return ((r & 0xff) << 24) | ((g & 0xff) << 16) | ((b & 0xff) << 8) | (a & 0xff);
    }
}
