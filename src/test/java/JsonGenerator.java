import java.io.File;
import java.util.*;

public class JsonGenerator {

    public static void main(String[] args) {
        String prefix = "src/main/resources/resourcepacks/assets/minecraft/textures/";
        String itemPath = prefix + "item/";
        String blockPath = prefix + "block/";

        File itemFolder = new File(itemPath);
        File blockFolder = new File(blockPath);

        System.out.println("Items:");
        generateEntries(itemFolder, "item");

        System.out.println();
        System.out.println("Blocks:");
        generateEntries(blockFolder, "block");
    }

    private static void generateEntries(File folder, String type) {
        Set<String> uniqueEntries = new LinkedHashSet<>();
        Map<String, List<String>> allVariants = new LinkedHashMap<>();

        for (File file : Objects.requireNonNull(folder.listFiles())) {
            String fileName = file.getName();

            if (!fileName.endsWith(".png")) {
                continue;
            }

            String id = fileName.substring(0, fileName.lastIndexOf("."));

            String baseId;

            if (type.equals("block")) {
                baseId = normalizeBlockId(id);
            } else {
                baseId = normalizeItemId(id);
            }

            allVariants
                    .computeIfAbsent(baseId, key -> new ArrayList<>())
                    .add(id);

            if (uniqueEntries.add(baseId)) {
                String name = toTitleCase(baseId.replace("_", " "));
                String en_us = "\"" + type + ".game." + baseId + "\": \"" + name + "\",";
                System.out.println(en_us);
            }
        }

        System.out.println();
        System.out.println("Alle Varianten für " + type + "s:");

        for (Map.Entry<String, List<String>> entry : allVariants.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
    }

    private static String normalizeItemId(String id) {
        return id.replaceAll("\\d+$", "");
    }

    private static String normalizeBlockId(String id) {
        String baseId = id;

        // Zahlen am Ende entfernen:
        // acacia_leaves0 -> acacia_leaves
        // stone1 -> stone
        baseId = baseId.replaceAll("\\d+$", "");

        // Grobe Block-Varianten entfernen:
        // acacia_door_bottom -> acacia_door
        // acacia_door_top -> acacia_door
        baseId = baseId.replaceAll("_(top|bottom)$", "");

        return baseId;
    }

    private static String toTitleCase(String input) {
        String[] words = input.split(" ");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }

            result.append(Character.toUpperCase(word.charAt(0)));

            if (word.length() > 1) {
                result.append(word.substring(1).toLowerCase());
            }

            result.append(" ");
        }

        return result.toString().trim();
    }
}