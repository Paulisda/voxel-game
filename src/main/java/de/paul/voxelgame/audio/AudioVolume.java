package de.paul.voxelgame.audio;

import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;

final class AudioVolume {
    private AudioVolume() {
    }

    static float clamp(final float volume) {
        return Math.max(0.0f, Math.min(1.0f, volume));
    }

    static void apply(final Line line, final float volume) {
        if (line == null) {
            return;
        }

        final float clamped = clamp(volume);
        if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            final FloatControl gain = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
            final float db = clamped <= 0.0001f ? gain.getMinimum() : (float) (20.0 * Math.log10(clamped));
            gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
        } else if (line.isControlSupported(FloatControl.Type.VOLUME)) {
            final FloatControl direct = (FloatControl) line.getControl(FloatControl.Type.VOLUME);
            direct.setValue(Math.max(direct.getMinimum(), Math.min(direct.getMaximum(), clamped)));
        }
    }
}
