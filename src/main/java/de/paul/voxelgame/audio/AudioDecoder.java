package de.paul.voxelgame.audio;

import de.paul.voxelgame.assets.ResourceManager;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

final class AudioDecoder {
    private AudioDecoder() {
    }

    static DecodedAudio decode(final ResourceManager resources, final String resourcePath) throws Exception {
        try (InputStream raw = new BufferedInputStream(resources.open(resourcePath));
             AudioInputStream source = AudioSystem.getAudioInputStream(raw);
             AudioInputStream pcm = AudioSystem.getAudioInputStream(targetFormat(source.getFormat()), source)) {
            return new DecodedAudio(pcm.getFormat(), pcm.readAllBytes());
        }
    }

    static AudioInputStream openPcmStream(final ResourceManager resources, final String resourcePath) throws Exception {
        final InputStream raw = new BufferedInputStream(resources.open(resourcePath));
        try {
            final AudioInputStream source = AudioSystem.getAudioInputStream(raw);
            return AudioSystem.getAudioInputStream(targetFormat(source.getFormat()), source);
        } catch (Exception e) {
            try {
                raw.close();
            } catch (IOException ignored) {
            }
            throw e;
        }
    }

    private static AudioFormat targetFormat(final AudioFormat source) {
        return new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                source.getSampleRate(),
                16,
                source.getChannels(),
                source.getChannels() * 2,
                source.getSampleRate(),
                false
        );
    }
}
