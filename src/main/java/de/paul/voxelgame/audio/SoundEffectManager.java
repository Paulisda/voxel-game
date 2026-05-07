package de.paul.voxelgame.audio;

import de.paul.voxelgame.assets.ResourceManager;
import de.paul.voxelgame.debug.GameDebug;
import de.paul.voxelgame.objects.ResourceId;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class SoundEffectManager {
    private final ResourceManager resources;
    private final Map<String, DecodedAudio> cache = new HashMap<>();
    private final Set<Clip> activeClips = new HashSet<>();
    private float volume = 0.75f;

    public SoundEffectManager(final ResourceManager resources) {
        this.resources = resources;
    }

    public void play(final ResourceId id) {
        playResource(pathFor(id));
    }

    public void playResource(final String resourcePath) {
        try {
            final DecodedAudio audio = cache.computeIfAbsent(resourcePath, this::decodeSafely);
            if (audio == null) {
                return;
            }
            final Clip clip = AudioSystem.getClip();
            clip.open(audio.format(), audio.pcm(), 0, audio.pcm().length);
            AudioVolume.apply(clip, volume);
            synchronized (activeClips) {
                activeClips.add(clip);
            }
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    synchronized (activeClips) {
                        activeClips.remove(clip);
                    }
                    clip.close();
                } else if (event.getType() == LineEvent.Type.CLOSE) {
                    synchronized (activeClips) {
                        activeClips.remove(clip);
                    }
                }
            });
            clip.start();
        } catch (Exception e) {
            GameDebug.info("Could not play effect " + resourcePath + ": " + e.getMessage());
        }
    }

    public void destroy() {
        synchronized (activeClips) {
            for (final Clip clip : activeClips) {
                clip.stop();
                clip.close();
            }
            activeClips.clear();
        }
        cache.clear();
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(final float volume) {
        this.volume = AudioVolume.clamp(volume);
        synchronized (activeClips) {
            for (final Clip clip : activeClips) {
                AudioVolume.apply(clip, this.volume);
            }
        }
    }

    public void adjustVolume(final float delta) {
        setVolume(volume + delta);
    }

    private DecodedAudio decodeSafely(final String resourcePath) {
        try {
            return AudioDecoder.decode(resources, resourcePath);
        } catch (Exception e) {
            GameDebug.info("Could not decode effect " + resourcePath + ": " + e.getMessage());
            return null;
        }
    }

    private String pathFor(final ResourceId id) {
        return "assets/" + id.namespace() + "/sounds/effects/" + id.path() + ".wav";
    }
}
