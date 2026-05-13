package de.paul.voxelgame.audio;

import de.paul.voxelgame.assets.ResourceManager;
import de.paul.voxelgame.debug.GameDebug;
import de.paul.voxelgame.objects.ResourceId;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SoundEffectManager {
    private static final int MAX_CLIPS_PER_SOUND = 4;

    private final ResourceManager resources;
    private final Map<String, DecodedAudio> cache = new HashMap<>();
    private final Map<String, ClipPool> clipPools = new HashMap<>();
    private float volume = 0.05f;

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
            final Clip clip = acquireClip(resourcePath, audio);
            AudioVolume.apply(clip, volume);
            if (clip.isRunning()) {
                clip.stop();
            }
            clip.setFramePosition(0);
            clip.start();
        } catch (Exception e) {
            GameDebug.info("Could not play effect " + resourcePath + ": " + e.getMessage());
        }
    }

    public void preload(final ResourceId id) {
        final String resourcePath = pathFor(id);
        try {
            final DecodedAudio audio = cache.computeIfAbsent(resourcePath, this::decodeSafely);
            if (audio != null) {
                acquireClip(resourcePath, audio);
            }
        } catch (Exception e) {
            GameDebug.info("Could not preload effect " + resourcePath + ": " + e.getMessage());
        }
    }

    public void destroy() {
        synchronized (clipPools) {
            for (final ClipPool pool : clipPools.values()) {
                pool.close();
            }
            clipPools.clear();
        }
        cache.clear();
    }

    public float getVolume() {
        return volume;
    }

    public void setVolume(final float volume) {
        this.volume = AudioVolume.clamp(volume);
        synchronized (clipPools) {
            for (final ClipPool pool : clipPools.values()) {
                pool.applyVolume(this.volume);
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

    private Clip acquireClip(final String resourcePath, final DecodedAudio audio) throws Exception {
        synchronized (clipPools) {
            ClipPool pool = clipPools.get(resourcePath);
            if (pool == null) {
                pool = new ClipPool(audio);
                clipPools.put(resourcePath, pool);
            }
            return pool.acquire();
        }
    }

    private final class ClipPool {
        private final DecodedAudio audio;
        private final List<Clip> clips = new ArrayList<>();
        private int cursor;

        private ClipPool(final DecodedAudio audio) {
            this.audio = audio;
        }

        private Clip acquire() throws Exception {
            for (int i = 0; i < clips.size(); i++) {
                cursor = (cursor + 1) % clips.size();
                final Clip clip = clips.get(cursor);
                if (!clip.isRunning() && !clip.isActive()) {
                    return clip;
                }
            }

            if (clips.size() < MAX_CLIPS_PER_SOUND) {
                final Clip clip = openClip(audio);
                clips.add(clip);
                cursor = clips.size() - 1;
                return clip;
            }

            cursor = (cursor + 1) % clips.size();
            final Clip clip = clips.get(cursor);
            clip.stop();
            return clip;
        }

        private void applyVolume(final float volume) {
            for (final Clip clip : clips) {
                AudioVolume.apply(clip, volume);
            }
        }

        private void close() {
            for (final Clip clip : clips) {
                clip.stop();
                clip.close();
            }
            clips.clear();
        }
    }

    private Clip openClip(final DecodedAudio audio) throws Exception {
        final Clip clip = AudioSystem.getClip();
        clip.open(audio.format(), audio.pcm(), 0, audio.pcm().length);
        AudioVolume.apply(clip, volume);
        return clip;
    }
}
