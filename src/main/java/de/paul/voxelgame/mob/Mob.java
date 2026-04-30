package de.paul.voxelgame.mob;

import de.paul.voxelgame.GameConfig;
import de.paul.voxelgame.map.Block;
import de.paul.voxelgame.map.World;
import de.paul.voxelgame.math.HitBox;
import de.paul.voxelgame.math.Vector3f;

public abstract class Mob {
    private static final double COLLISION_EPSILON = 1e-4;
    private static final double MAX_COLLISION_STEP = GameConfig.BLOCK_SIZE * 0.45;

    protected final float gravity = -24f;
    protected final float jumpVelocity = 8f;
    protected final float maxFallSpeed = -35f;

    protected final World world;

    protected double movementSpeed;
    protected boolean gravityOn;
    protected float fallVelocity;
    protected HitBox hitBox;
    protected boolean isOnGround;
    protected float halfWidth;
    protected float bodyHeight;
    protected float eyeHeight;

    private double yaw;
    private double pitch;
    private double cameraX;
    private double cameraY;
    private double cameraZ;

    protected Mob(World world) {
        this.world = world;
    }

    public Vector3f move(Vector3f velocity) {
        if (velocity.lengthSquared() <= 0.0f) {
            return Vector3f.ZERO;
        }

        isOnGround = false;

        int steps = computeCollisionSubsteps(velocity);
        Vector3f stepVelocity = velocity.mul(1.0f / steps);
        Vector3f appliedTotal = Vector3f.ZERO;

        for (int i = 0; i < steps; i++) {
            Vector3f appliedStep = moveSingleStep(stepVelocity);
            appliedTotal = appliedTotal.add(appliedStep);
        }

        return appliedTotal;
    }

    private Vector3f moveSingleStep(Vector3f velocity) {
        if (velocity.lengthSquared() <= 0.0f) {
            return Vector3f.ZERO;
        }

        double moveX = clipMoveX(velocity.getX());
        if (Math.abs(moveX) > COLLISION_EPSILON) {
            setLocation(hitBox.getMin().getX() + moveX, hitBox.getMin().getY(), hitBox.getMin().getZ());
        }

        double requestedY = velocity.getY();
        double moveY = clipMoveY(requestedY);
        if (Math.abs(moveY) > COLLISION_EPSILON) {
            setLocation(hitBox.getMin().getX(), hitBox.getMin().getY() + moveY, hitBox.getMin().getZ());
        }

        if (requestedY < 0 && moveY > requestedY + COLLISION_EPSILON) {
            isOnGround = true;
            fallVelocity = 0;
        }
        if (requestedY > 0 && moveY < requestedY - COLLISION_EPSILON) {
            fallVelocity = 0;
        }

        double moveZ = clipMoveZ(velocity.getZ());
        if (Math.abs(moveZ) > COLLISION_EPSILON) {
            setLocation(hitBox.getMin().getX(), hitBox.getMin().getY(), hitBox.getMin().getZ() + moveZ);
        }

        return new Vector3f(moveX, moveY, moveZ);
    }

    private int computeCollisionSubsteps(Vector3f velocity) {
        double maxDelta = Math.max(
                Math.abs(velocity.getX()),
                Math.max(Math.abs(velocity.getY()), Math.abs(velocity.getZ()))
        );
        if (maxDelta <= MAX_COLLISION_STEP) {
            return 1;
        }
        int steps = (int) Math.ceil(maxDelta / MAX_COLLISION_STEP);
        return Math.max(1, Math.min(12, steps));
    }

    private double clipMoveX(double requestedMove) {
        if (Math.abs(requestedMove) <= COLLISION_EPSILON) {
            return 0;
        }

        double blockSize = GameConfig.BLOCK_SIZE;
        double move = requestedMove;

        double nextMinX = hitBox.getMin().getX() + move;
        double nextMaxX = hitBox.getMax().getX() + move;

        int minBX = toBlockCoordinate(nextMinX + COLLISION_EPSILON);
        int maxBX = toBlockCoordinate(nextMaxX - COLLISION_EPSILON);
        int minBY = toBlockCoordinate(hitBox.getMin().getY() + COLLISION_EPSILON);
        int maxBY = toBlockCoordinate(hitBox.getMax().getY() - COLLISION_EPSILON);
        int minBZ = toBlockCoordinate(hitBox.getMin().getZ() + COLLISION_EPSILON);
        int maxBZ = toBlockCoordinate(hitBox.getMax().getZ() - COLLISION_EPSILON);

        for (int bx = minBX; bx <= maxBX; bx++) {
            for (int by = minBY; by <= maxBY; by++) {
                for (int bz = minBZ; bz <= maxBZ; bz++) {
                    if (!isSolidBlockAt(bx, by, bz)) {
                        continue;
                    }
                    if (move > 0) {
                        double blockMinX = bx * blockSize;
                        double allowed = blockMinX - hitBox.getMax().getX() - COLLISION_EPSILON;
                        if (allowed < move) {
                            move = allowed;
                        }
                    } else {
                        double blockMaxX = (bx + 1) * blockSize;
                        double allowed = blockMaxX - hitBox.getMin().getX() + COLLISION_EPSILON;
                        if (allowed > move) {
                            move = allowed;
                        }
                    }
                }
            }
        }

        return move;
    }

    private double clipMoveY(double requestedMove) {
        if (Math.abs(requestedMove) <= COLLISION_EPSILON) {
            return 0;
        }

        double blockSize = GameConfig.BLOCK_SIZE;
        double move = requestedMove;

        double nextMinY = hitBox.getMin().getY() + move;
        double nextMaxY = hitBox.getMax().getY() + move;

        int minBX = toBlockCoordinate(hitBox.getMin().getX() + COLLISION_EPSILON);
        int maxBX = toBlockCoordinate(hitBox.getMax().getX() - COLLISION_EPSILON);
        int minBY = toBlockCoordinate(nextMinY + COLLISION_EPSILON);
        int maxBY = toBlockCoordinate(nextMaxY - COLLISION_EPSILON);
        int minBZ = toBlockCoordinate(hitBox.getMin().getZ() + COLLISION_EPSILON);
        int maxBZ = toBlockCoordinate(hitBox.getMax().getZ() - COLLISION_EPSILON);

        for (int bx = minBX; bx <= maxBX; bx++) {
            for (int by = minBY; by <= maxBY; by++) {
                for (int bz = minBZ; bz <= maxBZ; bz++) {
                    if (!isSolidBlockAt(bx, by, bz)) {
                        continue;
                    }
                    if (move > 0) {
                        double blockMinY = by * blockSize;
                        double allowed = blockMinY - hitBox.getMax().getY() - COLLISION_EPSILON;
                        if (allowed < move) {
                            move = allowed;
                        }
                    } else {
                        double blockMaxY = (by + 1) * blockSize;
                        double allowed = blockMaxY - hitBox.getMin().getY() + COLLISION_EPSILON;
                        if (allowed > move) {
                            move = allowed;
                        }
                    }
                }
            }
        }

        return move;
    }

    private double clipMoveZ(double requestedMove) {
        if (Math.abs(requestedMove) <= COLLISION_EPSILON) {
            return 0;
        }

        double blockSize = GameConfig.BLOCK_SIZE;
        double move = requestedMove;

        double nextMinZ = hitBox.getMin().getZ() + move;
        double nextMaxZ = hitBox.getMax().getZ() + move;

        int minBX = toBlockCoordinate(hitBox.getMin().getX() + COLLISION_EPSILON);
        int maxBX = toBlockCoordinate(hitBox.getMax().getX() - COLLISION_EPSILON);
        int minBY = toBlockCoordinate(hitBox.getMin().getY() + COLLISION_EPSILON);
        int maxBY = toBlockCoordinate(hitBox.getMax().getY() - COLLISION_EPSILON);
        int minBZ = toBlockCoordinate(nextMinZ + COLLISION_EPSILON);
        int maxBZ = toBlockCoordinate(nextMaxZ - COLLISION_EPSILON);

        for (int bx = minBX; bx <= maxBX; bx++) {
            for (int by = minBY; by <= maxBY; by++) {
                for (int bz = minBZ; bz <= maxBZ; bz++) {
                    if (!isSolidBlockAt(bx, by, bz)) {
                        continue;
                    }
                    if (move > 0) {
                        double blockMinZ = bz * blockSize;
                        double allowed = blockMinZ - hitBox.getMax().getZ() - COLLISION_EPSILON;
                        if (allowed < move) {
                            move = allowed;
                        }
                    } else {
                        double blockMaxZ = (bz + 1) * blockSize;
                        double allowed = blockMaxZ - hitBox.getMin().getZ() + COLLISION_EPSILON;
                        if (allowed > move) {
                            move = allowed;
                        }
                    }
                }
            }
        }

        return move;
    }

    private boolean isSolidBlockAt(int blockX, int blockY, int blockZ) {
        Block block = world.getBlock(blockX, blockY, blockZ);
        return block != null && block.isSolid();
    }

    protected int toBlockCoordinate(double worldCoordinate) {
        return (int) Math.floor(worldCoordinate / GameConfig.BLOCK_SIZE);
    }

    // x/y/z are the minimum corner of the hitbox.
    protected void setLocation(double x, double y, double z) {
        Vector3f size = hitBox.size();
        cameraX = x + size.getX() * 0.5;
        cameraY = y + eyeHeight;
        cameraZ = z + size.getZ() * 0.5;
        hitBox.move(new Vector3f(x, y, z));
    }

    protected double normalizeAngle(double angle) {
        double normalized = angle % 360.0;
        if (normalized < 0) {
            normalized += 360.0;
        }
        return normalized;
    }

    protected void setYaw(double angle) {
        yaw = normalizeAngle(angle);
    }

    protected void setPitch(double angle) {
        pitch = Math.max(-89.0, Math.min(89.0, angle));
    }

    protected double getPitch() {
        return pitch;
    }

    protected double getYaw() {
        return yaw;
    }

    protected HitBox getHitBox() {
        return hitBox;
    }

    public double getCameraX() {
        return cameraX;
    }

    public double getCameraY() {
        return cameraY;
    }

    public double getCameraZ() {
        return cameraZ;
    }

    public double getFeetX() {
        return hitBox.getMin().getX() + halfWidth;
    }

    public double getFeetY() {
        return hitBox.getMin().getY();
    }

    public double getFeetZ() {
        return hitBox.getMin().getZ() + halfWidth;
    }

    // x/y/z are the feet center of the player.
    public void teleport(double x, double y, double z, double pitch, double yaw) {
        setLocation(x - halfWidth, y, z - halfWidth);
        setPitch(pitch);
        setYaw(yaw);
        fallVelocity = 0;
        isOnGround = false;
    }
}
