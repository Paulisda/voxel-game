package de.paul.voxelgame.assets;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public class ResourceManager {
    private static final Path SOURCE_RESOURCES = Paths.get("src", "main", "resources");
    private static final Path TARGET_RESOURCES = Paths.get("target", "classes");

    public InputStream open(final String path) {
        final String normalized = normalize(path);
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream stream = classLoader == null ? null : classLoader.getResourceAsStream(normalized);
        if (stream == null) {
            stream = ResourceManager.class.getClassLoader().getResourceAsStream(normalized);
        }
        if (stream == null) {
            final Path sourcePath = SOURCE_RESOURCES.resolve(normalized);
            if (Files.isRegularFile(sourcePath)) {
                try {
                    return Files.newInputStream(sourcePath);
                } catch (IOException e) {
                    throw new IllegalArgumentException("Resource not readable: " + path, e);
                }
            }

            final Path targetPath = TARGET_RESOURCES.resolve(normalized);
            if (Files.isRegularFile(targetPath)) {
                try {
                    return Files.newInputStream(targetPath);
                } catch (IOException e) {
                    throw new IllegalArgumentException("Resource not readable: " + path, e);
                }
            }

            final Path directPath = Paths.get(normalized);
            if (Files.isRegularFile(directPath)) {
                try {
                    return Files.newInputStream(directPath);
                } catch (IOException e) {
                    throw new IllegalArgumentException("Resource not readable: " + path, e);
                }
            }

            throw new IllegalArgumentException("Resource not found: " + path);
        }

        return stream;
    }

    public byte[] readBytes(final String path) {
        try (InputStream input = open(path)) {
            return input.readAllBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Resource not readable: " + path, e);
        }
    }

    public String readString(final String path) {
        return new String(readBytes(path), StandardCharsets.UTF_8);
    }

    public List<String> listJsonResources(final String directory) {
        final String normalized = normalize(directory);
        final Set<String> resources = new LinkedHashSet<>();

        collectFromDirectory(SOURCE_RESOURCES.resolve(normalized), normalized, resources);
        collectFromDirectory(TARGET_RESOURCES.resolve(normalized), normalized, resources);
        collectFromClassLoader(normalized, resources);

        final List<String> result = new ArrayList<>(resources);
        Collections.sort(result);
        return result;
    }

    private void collectFromDirectory(final Path directory, final String resourcePrefix, final Set<String> resources) {
        if (!Files.isDirectory(directory)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(directory)) {
            paths.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".json"))
                    .map(path -> resourcePrefix + "/" + directory.relativize(path).toString().replace('\\', '/'))
                    .forEach(resources::add);
        } catch (IOException ignored) {
        }
    }

    private void collectFromClassLoader(final String directory, final Set<String> resources) {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            return;
        }

        try {
            final Enumeration<URL> urls = classLoader.getResources(directory);
            while (urls.hasMoreElements()) {
                collectFromUrl(urls.nextElement(), directory, resources);
            }
        } catch (IOException ignored) {
        }
    }

    private void collectFromUrl(final URL url, final String directory, final Set<String> resources) {
        if ("file".equals(url.getProtocol())) {
            try {
                collectFromDirectory(Paths.get(url.toURI()), directory, resources);
            } catch (URISyntaxException ignored) {
            }
            return;
        }

        if (!"jar".equals(url.getProtocol())) {
            return;
        }

        try {
            final JarURLConnection connection = (JarURLConnection) url.openConnection();
            try (JarFile jarFile = connection.getJarFile()) {
                final Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    final JarEntry entry = entries.nextElement();
                    final String name = entry.getName();
                    if (!entry.isDirectory() && name.startsWith(directory + "/") && name.endsWith(".json")) {
                        resources.add(name);
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    private String normalize(final String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("Resource path must not be blank");
        }

        String normalized = path.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
