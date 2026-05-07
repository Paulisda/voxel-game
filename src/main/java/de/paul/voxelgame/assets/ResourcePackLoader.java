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
    private static final String GUI_TEXTURE_PREFIX = "assets/minecraft/textures/gui/";
    private static final String ITEM_TEXTURE_PREFIX = "assets/minecraft/textures/item/";
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

    public byte[] loadItemTexture(final String... textureCandidates) {
        return loadTextureWithPrefix(ITEM_TEXTURE_PREFIX, textureCandidates);
    }

    private byte[] loadTextureWithPrefix(final String prefix, final String... textureCandidates) {
        if (textureCandidates == null || textureCandidates.length == 0) {
            return null;
        }
        for (final Path pack : packFiles) {
            final byte[] data = tryLoadTextureFromPack(pack, prefix, textureCandidates);
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
                paths.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().toLowerCase().endsWith(".zip"))
                        .sorted(Comparator.comparingLong(this::safeLastModified).reversed())
                        .forEach(result::add);
            } catch (IOException ignored) {
            }
        }
        return result;
    }

    private long safeLastModified(final Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return Long.MIN_VALUE;
        }
    }

    private byte[] tryLoadTextureFromPack(final Path packFile, final String prefix, final String... textureCandidates) {
        try (final ZipFile zipFile = new ZipFile(packFile.toFile())) {
            for (final String candidate : textureCandidates) {
                if (candidate == null || candidate.isBlank()) {
                    continue;
                }
                final String entryName = prefix + candidate + ".png";
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
}
