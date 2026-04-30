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
    private static final String[] RESOURCE_PACK_DIRECTORIES = {
            "resourcepacks",
            "src/main/resources/resourcepacks"
    };

    private final List<Path> packFiles;

    public ResourcePackLoader() {
        this.packFiles = discoverPacks();
    }

    public byte[] loadBlockTexture(String... textureCandidates) {
        return loadTextureWithPrefix(BLOCK_TEXTURE_PREFIX, textureCandidates);
    }

    public byte[] loadColorMapTexture(String... textureCandidates) {
        return loadTextureWithPrefix(COLORMAP_TEXTURE_PREFIX, textureCandidates);
    }

    public byte[] loadGuiTexture(String... textureCandidates) {
        return loadTextureWithPrefix(GUI_TEXTURE_PREFIX, textureCandidates);
    }

    private byte[] loadTextureWithPrefix(String prefix, String... textureCandidates) {
        if (textureCandidates == null || textureCandidates.length == 0) {
            return null;
        }
        for (Path pack : packFiles) {
            byte[] data = tryLoadTextureFromPack(pack, prefix, textureCandidates);
            if (data != null) {
                return data;
            }
        }
        return null;
    }

    private List<Path> discoverPacks() {
        List<Path> result = new ArrayList<>();
        for (String directory : RESOURCE_PACK_DIRECTORIES) {
            Path packDir = Paths.get(directory);
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

    private long safeLastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return Long.MIN_VALUE;
        }
    }

    private byte[] tryLoadTextureFromPack(Path packFile, String prefix, String... textureCandidates) {
        try (ZipFile zipFile = new ZipFile(packFile.toFile())) {
            for (String candidate : textureCandidates) {
                if (candidate == null || candidate.isBlank()) {
                    continue;
                }
                String entryName = prefix + candidate + ".png";
                ZipEntry entry = zipFile.getEntry(entryName);
                if (entry == null) {
                    continue;
                }
                try (InputStream input = zipFile.getInputStream(entry)) {
                    return input.readAllBytes();
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }
}
