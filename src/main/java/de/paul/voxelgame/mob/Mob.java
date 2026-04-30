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

    protected Mob(final World world) {
        this.world = world;
    }

    public Vector3f move(final Vector3f velocity) {
        if (velocity.lengthSquared() <= 0.0f) {
            return Vector3f.ZERO;
        }

        isOnGround = false;

        final int steps = computeCollisionSubsteps(velocity);
        final Vector3f stepVelocity = velocity.mul(1.0f / steps);
        Vector3f appliedTotal = Vector3f.ZERO;

        for (int i = 0; i < steps; i++) {
            final Vector3f appliedStep = moveSingleStep(stepVelocity);
            appliedTotal = appliedTotal.add(appliedStep);
        }

        return appliedTotal;
    }

    private Vector3f moveSingleStep(final Vector3f velocity) {
        if (velocity.lengthSquared() <= 0.0f) {
            return Vector3f.ZERO;
        }

        final double moveX = clipMoveX(velocity.getX());
        if (Math.abs(moveX) > COLLISION_EPSILON) {
            setLocation(hitBox.getMin().getX() + moveX, hitBox.getMin().getY(), hitBox.getMin().getZ());
        }

        final double requestedY = velocity.getY();
        final double moveY = clipMoveY(requestedY);
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

        final double moveZ = clipMoveZ(velocity.getZ());
        if (Math.abs(moveZ) > COLLISION_EPSILON) {
            setLocation(hitBox.getMin().getX(), hitBox.getMin().getY(), hitBox.getMin().getZ() + moveZ);
        }

        return new Vector3f(moveX, moveY, moveZ);
    }

    private int computeCollisionSubsteps(final Vector3f velocity) {
        final double maxDelta = Math.max(
                Math.abs(velocity.getX()),
                Math.max(Math.abs(velocity.getY()), Math.abs(velocity.getZ()))
        );
        if (maxDelta <= MAX_COLLISION_STEP) {
            return 1;
        }
        final int steps = (int) Math.ceil(maxDelta / MAX_COLLISION_STEP);
        return Math.max(1, Math.min(12, steps));
    }

    private double clipMoveX(final double requestedMove) {
        if (Math.abs(requestedMove) <= COLLISION_EPSILON) {
            return 0;
        }

        final double blockSize = GameConfig.BLOCK_SIZE;
        double move = requestedMove;

        final double nextMinX = hitBox.getMin().getX() + move;
        final double nextMaxX = hitBox.getMax().getX() + move;

        final int minBX = toBlockCoordinate(nextMinX + COLLISION_EPSILON);
        final int maxBX = toBlockCoordinate(nextMaxX - COLLISION_EPSILON);
        final int minBY = toBlockCoordinate(hitBox.getMin().getY() + COLLISION_EPSILON);
        final int maxBY = toBlockCoordinate(hitBox.getMax().getY() - COLLISION_EPSILON);
        final int minBZ = toBlockCoordinate(hitBox.getMin().getZ() + COLLISION_EPSILON);
        final int maxBZ = toBlockCoordinate(hitBox.getMax().getZ() - COLLISION_EPSILON);

        for (int bx = minBX; bx <= maxBX; bx++) {
            for (int by = minBY; by <= maxBY; by++) {
                for (int bz = minBZ; bz <= maxBZ; bz++) {
                    if (!isSolidBlockAt(bx, by, bz)) {
                        continue;
                    }
                    if (move > 0) {
                        final double blockMinX = bx * blockSize;
                        final double allowed = blockMinX - hitBox.getMax().getX() - COLLISION_EPSILON;
                        if (allowed < move) {
                            move = allowed;
                        }
                    } else {
                        final double blockMaxX = (bx + 1) * blockSize;
                        final double allowed = blockMaxX - hitBox.getMin().getX() + COLLISION_EPSILON;
                        if (allowed > move) {
                            move = allowed;
                        }
                    }
                }
            }
        }

        return move;
    }

    private double clipMoveY(final double requestedMove) {
        if (Math.abs(requestedMove) <= COLLISION_EPSILON) {
            return 0;
        }

        final double blockSize = GameConfig.BLOCK_SIZE;
        double move = requestedMove;

        final double nextMinY = hitBox.getMin().getY() + move;
        final double nextMaxY = hitBox.getMax().getY() + move;

        final int minBX = toBlockCoordinate(hitBox.getMin().getX() + COLLISION_EPSILON);
        final int maxBX = toBlockCoordinate(hitBox.getMax().getX() - COLLISION_EPSILON);
        final int minBY = toBlockCoordinate(nextMinY + COLLISION_EPSILON);
        final int maxBY = toBlockCoordinate(nextMaxY - COLLISION_EPSILON);
        final int minBZ = toBlockCoordinate(hitBox.getMin().getZ() + COLLISION_EPSILON);
        final int maxBZ = toBlockCoordinate(hitBox.getMax().getZ() - COLLISION_EPSILON);

        for (int bx = minBX; bx <= maxBX; bx++) {
            for (int by = minBY; by <= maxBY; by++) {
                for (int bz = minBZ; bz <= maxBZ; bz++) {
                    if (!isSolidBlockAt(bx, by, bz)) {
                        continue;
                    }
                    if (move > 0) {
                        final double blockMinY = by * blockSize;
                        final double allowed = blockMinY - hitBox.getMax().getY() - COLLISION_EPSILON;
                        if (allowed < move) {
                            move = allowed;
                        }
                    } else {
                        final double blockMaxY = (by + 1) * blockSize;
                        final double allowed = blockMaxY - hitBox.getMin().getY() + COLLISION_EPSILON;
                        if (allowed > move) {
                            move = allowed;
                        }
                    }
                }
            }
        }

        return move;
    }

    private double clipMoveZ(final double requestedMove) {
        if (Math.abs(requestedMove) <= COLLISION_EPSILON) {
            return 0;
        }

        final double blockSize = GameConfig.BLOCK_SIZE;
        double move = requestedMove;

        final double nextMinZ = hitBox.getMin().getZ() + move;
        final double nextMaxZ = hitBox.getMax().getZ() + move;

        final int minBX = toBlockCoordinate(hitBox.getMin().getX() + COLLISION_EPSILON);
        final int maxBX = toBlockCoordinate(hitBox.getMax().getX() - COLLISION_EPSILON);
        final int minBY = toBlockCoordinate(hitBox.getMin().getY() + COLLISION_EPSILON);
        final int maxBY = toBlockCoordinate(hitBox.getMax().getY() - COLLISION_EPSILON);
        final int minBZ = toBlockCoordinate(nextMinZ + COLLISION_EPSILON);
        final int maxBZ = toBlockCoordinate(nextMaxZ - COLLISION_EPSILON);

        for (int bx = minBX; bx <= maxBX; bx++) {
            for (int by = minBY; by <= maxBY; by++) {
                for (int bz = minBZ; bz <= maxBZ; bz++) {
                    if (!isSolidBlockAt(bx, by, bz)) {
                        continue;
                    }
                    if (move > 0) {
                        final double blockMinZ = bz * blockSize;
                        final double allowed = blockMinZ - hitBox.getMax().getZ() - COLLISION_EPSILON;
                        if (allowed < move) {
                            move = allowed;
                        }
                    } else {
                        final double blockMaxZ = (bz + 1) * blockSize;
                        final double allowed = blockMaxZ - hitBox.getMin().getZ() + COLLISION_EPSILON;
                        if (allowed > move) {
                            move = allowed;
                        }
                    }
                }
            }
        }

        return move;
    }

    private boolean isSolidBlockAt(final int blockX, final int blockY, final int blockZ) {
        final Block block = world.getBlock(blockX, blockY, blockZ);
        return block != null && block.isSolid();
    }

    protected int toBlockCoordinate(final double worldCoordinate) {
        return (int) Math.floor(worldCoordinate / GameConfig.BLOCK_SIZE);
    }

    // x/y/z are the minimum corner of the hitbox.
    protected void setLocation(final double x, final double y, final double z) {
        final Vector3f size = hitBox.size();
        cameraX = x + size.getX() * 0.5;
        cameraY = y + eyeHeight;
        cameraZ = z + size.getZ() * 0.5;
        hitBox.move(new Vector3f(x, y, z));
    }

    protected double normalizeAngle(final double angle) {
        double normalized = angle % 360.0;
        if (normalized < 0) {
            normalized += 360.0;
        }
        return normalized;
    }

    protected void setYaw(final double angle) {
        yaw = normalizeAngle(angle);
    }

    protected void setPitch(final double angle) {
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
    public void teleport(final double x, final double y, final double z, final double pitch, final double yaw) {
        setLocation(x - halfWidth, y, z - halfWidth);
        setPitch(pitch);
        setYaw(yaw);
        fallVelocity = 0;
        isOnGround = false;
    }
}
