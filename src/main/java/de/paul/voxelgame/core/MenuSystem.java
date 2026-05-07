package de.paul.voxelgame.core;

public final class MenuSystem {
    public enum Screen {
        NONE,
        PAUSE,
        OPTIONS
    }

    private Screen screen = Screen.NONE;

    public void openPause() {
        screen = Screen.PAUSE;
    }

    public void openOptions() {
        screen = Screen.OPTIONS;
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
        return screen == Screen.OPTIONS;
    }

    public Screen screen() {
        return screen;
    }
}
