package de.paul.voxelgame.audio;

import javax.sound.sampled.AudioFormat;

record DecodedAudio(AudioFormat format, byte[] pcm) {
}
