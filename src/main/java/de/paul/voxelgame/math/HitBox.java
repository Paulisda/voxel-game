package de.paul.voxelgame.math;

public class HitBox {
    private Vector3f min;
    private Vector3f max;

    public HitBox(Vector3f min, Vector3f max) {
        this.min = min;
        this.max = max;
    }

    public Vector3f getMin() {
        return min;
    }

    public Vector3f getMax() {
        return max;
    }

    public Vector3f size() {
        return max.sub(min);
    }

    public void move(Vector3f to) {
        Vector3f diff = max.sub(min);
        min = to;
        max = to.add(diff);
    }

    public boolean intersects(HitBox other) {
        return min.getX() < other.max.getX() && max.getX() > other.min.getX()
                && min.getY() < other.max.getY() && max.getY() > other.min.getY()
                && min.getZ() < other.max.getZ() && max.getZ() > other.min.getZ();
    }
}
