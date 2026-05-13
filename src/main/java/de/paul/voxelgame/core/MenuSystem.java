package de.paul.voxelgame.core;

public final class MenuSystem {
    public enum Screen {
        NONE,
        PAUSE,
        OPTIONS,
        OPTIONS_MUSIC,
        OPTIONS_GRAPHICS,
        OPTIONS_CONTROLS,
        OPTIONS_LANGUAGE
    }

    private Screen screen = Screen.NONE;

    public void openPause() {
        screen = Screen.PAUSE;
    }

    public void openOptions() {
        screen = Screen.OPTIONS;
    }

    public void openMusicOptions() {
        screen = Screen.OPTIONS_MUSIC;
    }

    public void openGraphicsOptions() {
        screen = Screen.OPTIONS_GRAPHICS;
    }

    public void openControlsOptions() {
        screen = Screen.OPTIONS_CONTROLS;
    }

    public void openLanguageOptions() {
        screen = Screen.OPTIONS_LANGUAGE;
    }

    public void close() {
        screen = Screen.NONE;
    }

    public boolean isOpen() {
        return screen != Screen.NONE;
    }

    public boolean isPause() {
        return screen == Screen.PAUSE;
    }

    public boolean isOptions() {
        return screen == Screen.OPTIONS
                || screen == Screen.OPTIONS_MUSIC
                || screen == Screen.OPTIONS_GRAPHICS
                || screen == Screen.OPTIONS_CONTROLS
                || screen == Screen.OPTIONS_LANGUAGE;
    }

    public boolean isOptionsRoot() {
        return screen == Screen.OPTIONS;
    }

    public Screen screen() {
        return screen;
    }
}
