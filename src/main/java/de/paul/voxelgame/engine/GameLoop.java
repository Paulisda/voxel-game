package de.paul.voxelgame.engine;

import java.util.ArrayList;
import java.util.List;

public class GameLoop {
    private final List<GameObject> gameObjects = new ArrayList<>();
    private double fps;
    private long lastFrameNs = -1L;
    private long fpsWindowStartNs = -1L;
    private int fpsFrames;

    public void addGameObject(GameObject gameObject) {
        if (gameObject != null) {
            gameObjects.add(gameObject);
        }
    }

    public double update(long nowNs) {
        if (lastFrameNs < 0) {
            lastFrameNs = nowNs;
            fpsWindowStartNs = nowNs;
            return 0.0;
        }

        double deltaSeconds = (nowNs - lastFrameNs) / 1_000_000_000.0;
        deltaSeconds = Math.max(1.0 / 240.0, Math.min(0.05, deltaSeconds));
        lastFrameNs = nowNs;

        for (GameObject gameObject : gameObjects) {
            gameObject.update(deltaSeconds);
        }

        fpsFrames++;
        long fpsElapsed = nowNs - fpsWindowStartNs;
        if (fpsElapsed >= 1_000_000_000L) {
            fps = fpsFrames * 1_000_000_000.0 / fpsElapsed;
            fpsFrames = 0;
            fpsWindowStartNs = nowNs;
        }

        return deltaSeconds;
    }

    public double getFps() {
        return fps;
    }
}
