package de.paul.voxelgame.audio;

import de.paul.voxelgame.assets.ResourceManager;
import de.paul.voxelgame.debug.GameDebug;
import de.paul.voxelgame.objects.ResourceId;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.List;

public final class MusicManager {
    private static final String MUSIC_ROOT = "assets/game/sounds/music";
    private static final int BUFFER_SIZE = 8192;

    private final ResourceManager resources;
    private volatile boolean stopRequested;
    private volatile SourceDataLine currentLine;
    private Thread playbackThread;
    private float volume = 0.05f;

    public MusicManager(final ResourceManager resources) {
        this.resources = resources;
    }

    public void play(final ResourceId id, final boolean loop) {
        playResource(pathFor(id), loop);
    }

    public void playFirstAvailableLooping() {
        final List<String> tracks = resources.listResources(MUSIC_ROOT, ".mp3");
        if (!tracks.isEmpty()) {
            playResource(tracks.get(0), true);
        }
    }

    public synchronized void playResource(final String resourcePath, final boolean loop) {
        stop();
        stopRequested = false;
        playbackThread = new Thread(() -> playbackLoop(resourcePath, loop), "voxel-music");
        playbackThread.setDaemon(true);
        playbackThread.start();
    }

    public synchronized void stop() {
        stopRequested = true;
        final SourceDataLine line = currentLine;
        if (line != null) {
            line.stop();
            line.close();
        }
    }

    public void destroy() {
        stop();
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(final float volume) {
        this.volume = AudioVolume.clamp(volume);
        AudioVolume.apply(currentLine, this.volume);
    }

    public void adjustVolume(final float delta) {
        setVolume(volume + delta);
    }

    private void playbackLoop(final String resourcePath, final boolean loop) {
        do {
            try {
                streamOnce(resourcePath);
            } catch (Exception e) {
                if (stopRequested) {
                    return;
                }
                GameDebug.info("Could not play music " + resourcePath + ": " + e.getMessage());
                return;
            }
        } while (loop && !stopRequested);
    }

    private void streamOnce(final String resourcePath) throws Exception {
        try (AudioInputStream pcm = AudioDecoder.openPcmStream(resources, resourcePath)) {
            final SourceDataLine line = AudioSystem.getSourceDataLine(pcm.getFormat());
            currentLine = line;
            line.open(pcm.getFormat());
            AudioVolume.apply(line, volume);
            line.start();

            final byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while (!stopRequested && (read = pcm.read(buffer, 0, buffer.length)) >= 0) {
                line.write(buffer, 0, read);
            }
            if (!stopRequested) {
                line.drain();
            }
            line.stop();
            line.close();
        } finally {
            currentLine = null;
        }
    }

    private String pathFor(final ResourceId id) {
        return "assets/" + id.namespace() + "/sounds/music/" + id.path() + ".mp3";
    }
}
