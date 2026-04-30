package de.paul.voxelgame.math;

public class Vector3f {
    public static final Vector3f ZERO = new Vector3f(0, 0, 0);

    private final float x;
    private final float y;
    private final float z;

    public Vector3f(final double x, final double y, final double z) {
        this((float) x, (float) y, (float) z);
    }

    public Vector3f(final float x, final float y, final float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public float length() {
        return (float) Math.sqrt(lengthSquared());
    }

    public float lengthSquared() {
        return x * x + y * y + z * z;
    }

    public float dot(final Vector3f v) {
        return x * v.x + y * v.y + z * v.z;
    }

    public Vector3f cross(final Vector3f v) {
        return new Vector3f(
                y * v.z - z * v.y,
                z * v.x - x * v.z,
                x * v.y - y * v.x
        );
    }

    public Vector3f normalized() {
        final float length = length();
        if (length == 0) {
            return Vector3f.ZERO;
        }
        return new Vector3f(x / length, y / length, z / length);
    }

    public Vector3f add(final Vector3f v) {
        return new Vector3f(x + v.x, y + v.y, z + v.z);
    }

    public Vector3f add(final float value) {
        return new Vector3f(x + value, y + value, z + value);
    }

    public Vector3f sub(final Vector3f v) {
        return new Vector3f(x - v.x, y - v.y, z - v.z);
    }

    public Vector3f sub(final float value) {
        return new Vector3f(x - value, y - value, z - value);
    }

    public Vector3f mul(final Vector3f v) {
        return new Vector3f(x * v.x, y * v.y, z * v.z);
    }

    public Vector3f mul(final float value) {
        return new Vector3f(x * value, y * value, z * value);
    }

    public Vector3f div(final Vector3f v) {
        return new Vector3f(x / v.x, y / v.y, z / v.z);
    }

    public Vector3f div(final float value) {
        return new Vector3f(x / value, y / value, z / value);
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + "," + z + ")";
    }
}
