package de.paul.voxelgame.core;

import java.util.Random;

public final class EnvironmentSystem {
    public static final int TICKS_PER_SECOND = 20;
    public static final int DAY_LENGTH_TICKS = 24_000;

    private static final int MIN_CLEAR_TICKS = TICKS_PER_SECOND * 45;
    private static final int EXTRA_CLEAR_TICKS = TICKS_PER_SECOND * 180;
    private static final int MIN_RAIN_TICKS = TICKS_PER_SECOND * 35;
    private static final int EXTRA_RAIN_TICKS = TICKS_PER_SECOND * 95;
    private static final double RAIN_START_CHANCE_PER_TICK = 1.0 / (TICKS_PER_SECOND * 240.0);
    private static final float WEATHER_FADE_PER_TICK = 1.0f / (TICKS_PER_SECOND * 5.0f);

    private final Random random = new Random();
    private double tickAccumulator;
    private long totalTicks = 1_000;
    private int rainTicksRemaining;
    private int clearTicksRemaining = TICKS_PER_SECOND * 30;
    private float rainStrength;

    public void update(final double deltaSeconds) {
        tickAccumulator += Math.max(0.0, deltaSeconds) * TICKS_PER_SECOND;
        final int ticks = (int) tickAccumulator;
        if (ticks <= 0) {
            return;
        }

        tickAccumulator -= ticks;
        for (int i = 0; i < ticks; i++) {
            tick();
        }
    }

    private void tick() {
        totalTicks++;
        if (rainTicksRemaining > 0) {
            rainTicksRemaining--;
        } else if (clearTicksRemaining > 0) {
            clearTicksRemaining--;
        } else if (random.nextDouble() < RAIN_START_CHANCE_PER_TICK) {
            startRain();
        }

        final float targetRain = rainTicksRemaining > 0 ? 1.0f : 0.0f;
        if (rainStrength < targetRain) {
            rainStrength = Math.min(targetRain, rainStrength + WEATHER_FADE_PER_TICK);
        } else if (rainStrength > targetRain) {
            rainStrength = Math.max(targetRain, rainStrength - WEATHER_FADE_PER_TICK);
        }

        if (rainTicksRemaining == 0 && rainStrength == 0.0f && clearTicksRemaining <= 0) {
            clearTicksRemaining = MIN_CLEAR_TICKS + random.nextInt(EXTRA_CLEAR_TICKS + 1);
        }
    }

    private void startRain() {
        rainTicksRemaining = MIN_RAIN_TICKS + random.nextInt(EXTRA_RAIN_TICKS + 1);
    }

    public void setWeatherClear() {
        rainTicksRemaining = 0;
        clearTicksRemaining = MIN_CLEAR_TICKS + random.nextInt(EXTRA_CLEAR_TICKS + 1);
        rainStrength = 0.0f;
        tickAccumulator = 0.0;
    }

    public void setWeatherRain() {
        startRain();
        clearTicksRemaining = 0;
        rainStrength = 1.0f;
        tickAccumulator = 0.0;
    }

    public boolean isRaining() {
        return rainTicksRemaining > 0 || rainStrength > 0.01f;
    }

    public long totalTicks() {
        return totalTicks;
    }

    public int dayTime() {
        return (int) Math.floorMod(totalTicks, DAY_LENGTH_TICKS);
    }

    public void setDayTime(final int dayTime) {
        final int normalizedTime = Math.floorMod(dayTime, DAY_LENGTH_TICKS);
        final long currentDay = Math.floorDiv(totalTicks, DAY_LENGTH_TICKS);
        totalTicks = currentDay * DAY_LENGTH_TICKS + normalizedTime;
        tickAccumulator = 0.0;
    }

    public float dayProgress() {
        return dayTime() / (float) DAY_LENGTH_TICKS;
    }

    public float sunAltitude() {
        return (float) Math.sin(dayProgress() * Math.PI * 2.0);
    }

    public float sunVisibility() {
        return clamp((sunAltitude() + 0.08f) / 0.28f, 0.0f, 1.0f);
    }

    public float moonVisibility() {
        return clamp((-sunAltitude() + 0.08f) / 0.28f, 0.0f, 1.0f);
    }

    public int moonPhase() {
        return (int) Math.floorMod(totalTicks / DAY_LENGTH_TICKS, 8);
    }

    public float rainStrength() {
        return rainStrength;
    }

    public float skyRed() {
        return skyChannel(0.060f, 0.53f, 0.035f);
    }

    public float skyGreen() {
        return skyChannel(0.080f, 0.77f, 0.048f);
    }

    public float skyBlue() {
        return skyChannel(0.155f, 1.0f, 0.095f);
    }

    private float skyChannel(final float night, final float day, final float moonGlow) {
        final float dayLight = clamp((sunAltitude() + 0.18f) / 1.05f, 0.0f, 1.0f);
        final float moonLight = moonVisibility() * (1.0f - dayLight);
        final float rainDim = 1.0f - rainStrength * 0.32f;
        return clamp((night + (day - night) * dayLight + moonGlow * moonLight) * rainDim, 0.0f, 1.0f);
    }

    private static float clamp(final float value, final float min, final float max) {
        return Math.max(min, Math.min(max, value));
    }
}
