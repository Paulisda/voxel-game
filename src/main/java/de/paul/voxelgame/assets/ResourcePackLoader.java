package de.paul.voxelgame.assets;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ResourcePackLoader {
    private static final String BLOCK_TEXTURE_PREFIX = "assets/minecraft/textures/block/";
    private static final String COLORMAP_TEXTURE_PREFIX = "assets/minecraft/textures/colormap/";
    private static final String ENVIRONMENT_TEXTURE_PREFIX = "assets/minecraft/textures/environment/";
    private static final String FONT_PREFIX = "assets/minecraft/font/";
    private static final String GUI_TEXTURE_PREFIX = "assets/minecraft/textures/gui/";
    private static final String ITEM_TEXTURE_PREFIX = "assets/minecraft/textures/item/";
    private static final String MINECRAFT_TEXTURE_PREFIX = "assets/minecraft/textures/";
    private static final String BLOCK_MODEL_PREFIX = "assets/minecraft/models/block/";
    private static final String ITEM_MODEL_PREFIX = "assets/minecraft/models/item/";
    private static final String BLOCK_STATE_PREFIX = "assets/minecraft/blockstates/";
    private static final String[] RESOURCE_PACK_DIRECTORIES = {
            "resourcepacks",
            "src/main/resources/resourcepacks"
    };

    private final List<Path> packFiles;

    public ResourcePackLoader() {
        this.packFiles = discoverPacks();
    }

    public byte[] loadBlockTexture(final String... textureCandidates) {
        return loadTextureWithPrefix(BLOCK_TEXTURE_PREFIX, textureCandidates);
    }

    public byte[] loadColorMapTexture(final String... textureCandidates) {
        return loadTextureWithPrefix(COLORMAP_TEXTURE_PREFIX, textureCandidates);
    }

    public byte[] loadGuiTexture(final String... textureCandidates) {
        return loadTextureWithPrefix(GUI_TEXTURE_PREFIX, textureCandidates);
    }

    public byte[] loadEnvironmentTexture(final String... textureCandidates) {
        return loadTextureWithPrefix(ENVIRONMENT_TEXTURE_PREFIX, textureCandidates);
    }

    public byte[] loadFont(final String... fontCandidates) {
        return loadResourceWithPrefix(FONT_PREFIX, ".ttf", fontCandidates);
    }

    public byte[] loadItemTexture(final String... textureCandidates) {
        return loadTextureWithPrefix(ITEM_TEXTURE_PREFIX, textureCandidates);
    }

    public byte[] loadMinecraftTexture(final String... textureCandidates) {
        return loadTextureWithPrefix(MINECRAFT_TEXTURE_PREFIX, textureCandidates);
    }

    public String loadBlockModel(final String... modelCandidates) {
        final byte[] data = loadResourceWithPrefix(BLOCK_MODEL_PREFIX, ".json", modelCandidates);
        return data == null ? null : new String(data, java.nio.charset.StandardCharsets.UTF_8);
    }

    public String loadItemModel(final String... modelCandidates) {
        final byte[] data = loadResourceWithPrefix(ITEM_MODEL_PREFIX, ".json", modelCandidates);
        return data == null ? null : new String(data, java.nio.charset.StandardCharsets.UTF_8);
    }

    public String loadBlockState(final String... blockStateCandidates) {
        final byte[] data = loadResourceWithPrefix(BLOCK_STATE_PREFIX, ".json", blockStateCandidates);
        return data == null ? null : new String(data, java.nio.charset.StandardCharsets.UTF_8);
    }

    private byte[] loadTextureWithPrefix(final String prefix, final String... textureCandidates) {
        return loadResourceWithPrefix(prefix, ".png", textureCandidates);
    }

    private byte[] loadResourceWithPrefix(final String prefix, final String suffix, final String... textureCandidates) {
        if (textureCandidates == null || textureCandidates.length == 0) {
            return null;
        }
        for (final Path pack : packFiles) {
            final byte[] data = tryLoadResourceFromPack(pack, prefix, suffix, textureCandidates);
            if (data != null) {
                return data;
            }
        }
        return null;
    }

    private List<Path> discoverPacks() {
        final List<Path> result = new ArrayList<>();
        for (final String directory : RESOURCE_PACK_DIRECTORIES) {
            final Path packDir = Paths.get(directory);
            if (!Files.isDirectory(packDir)) {
                continue;
            }
            try (var paths = Files.list(packDir)) {
                paths.filter(this::isResourcePack)
                        .sorted(Comparator.comparingInt(this::packPriority)
                                .thenComparingLong(this::safeLastModified)
                                .reversed())
                        .forEach(result::add);
            } catch (IOException ignored) {
            }
        }
        return result;
    }

    private boolean isResourcePack(final Path path) {
        if (Files.isRegularFile(path) && path.getFileName().toString().toLowerCase().endsWith(".zip")) {
            return true;
        }
        return Files.isDirectory(path)
                && (Files.isRegularFile(path.resolve("pack.mcmeta")) || Files.isDirectory(path.resolve("assets")));
    }

    private int packPriority(final Path path) {
        return Files.isDirectory(path) ? 1 : 0;
    }

    private long safeLastModified(final Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return Long.MIN_VALUE;
        }
    }

    private byte[] tryLoadResourceFromPack(final Path packFile, final String prefix, final String suffix, final String... textureCandidates) {
        if (Files.isDirectory(packFile)) {
            return tryLoadResourceFromDirectory(packFile, prefix, suffix, textureCandidates);
        }

        try (final ZipFile zipFile = new ZipFile(packFile.toFile())) {
            for (final String candidate : textureCandidates) {
                if (candidate == null || candidate.isBlank()) {
                    continue;
                }
                final String entryName = prefix + candidate + suffix;
                final ZipEntry entry = zipFile.getEntry(entryName);
                if (entry == null) {
                    continue;
                }
                try (final InputStream input = zipFile.getInputStream(entry)) {
                    return input.readAllBytes();
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    private byte[] tryLoadResourceFromDirectory(final Path packDirectory, final String prefix, final String suffix, final String... textureCandidates) {
        for (final String candidate : textureCandidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }

            final Path resourcePath = packDirectory.resolve(prefix + candidate + suffix);
            if (!Files.isRegularFile(resourcePath)) {
                continue;
            }

            try {
                return Files.readAllBytes(resourcePath);
            } catch (IOException ignored) {
            }
        }
        return null;
    }
}
