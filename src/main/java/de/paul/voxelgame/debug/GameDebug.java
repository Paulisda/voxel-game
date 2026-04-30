package de.paul.voxelgame.debug;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public final class GameDebug {
    private static final Logger LOGGER = Logger.getLogger("de.paul.voxelgame.debug");
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final boolean ENABLED = Boolean.parseBoolean(System.getProperty("voxel.debug", "true"));
    private static boolean initialized;
    private static Path logFilePath;

    private GameDebug() {
    }

    public static synchronized void init() {
        if (initialized || !ENABLED) {
            return;
        }

        try {
            final Path logDir = Paths.get(System.getProperty("user.dir"), "logs");
            Files.createDirectories(logDir);
            logFilePath = logDir.resolve("voxel-debug.log");

            LOGGER.setUseParentHandlers(false);
            LOGGER.setLevel(Level.INFO);

            for (final Handler oldHandler : LOGGER.getHandlers()) {
                LOGGER.removeHandler(oldHandler);
                try {
                    oldHandler.close();
                } catch (Exception ignored) {
                }
            }

            final Formatter formatter = new Formatter() {
                @Override
                public String format(final LogRecord record) {
                    final String timestamp = LocalDateTime.now().format(TS_FORMAT);
                    return String.format("%s [%s] %s%n", timestamp, record.getLevel().getName(), record.getMessage());
                }
            };

            final FileHandler fileHandler = new FileHandler(logFilePath.toString(), true);
            fileHandler.setLevel(Level.INFO);
            fileHandler.setFormatter(formatter);
            LOGGER.addHandler(fileHandler);

            final ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(Level.INFO);
            consoleHandler.setFormatter(formatter);
            LOGGER.addHandler(consoleHandler);

            initialized = true;
            info("Debug logging initialized. File: " + logFilePath.toAbsolutePath());
        } catch (IOException e) {
            System.err.println("[Debug] Logging init failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean isEnabled() {
        return ENABLED;
    }

    public static void info(final String message) {
        if (!ENABLED) {
            return;
        }
        if (!initialized) {
            init();
        }
        LOGGER.info(message);
    }
}
