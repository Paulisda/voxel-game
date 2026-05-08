package de.paul.voxelgame.mob;

import de.paul.voxelgame.GameConfig;
import de.paul.voxelgame.audio.SoundEffectManager;
import de.paul.voxelgame.core.InventoryStack;
import de.paul.voxelgame.engine.InputState;
import de.paul.voxelgame.map.Block;
import de.paul.voxelgame.map.BlockFacing;
import de.paul.voxelgame.map.World;
import de.paul.voxelgame.math.HitBox;
import de.paul.voxelgame.math.Vector3f;
import de.paul.voxelgame.objects.BlockItemComponent;
import de.paul.voxelgame.objects.GameObject;
import de.paul.voxelgame.objects.ResourceId;
import de.paul.voxelgame.objects.SpawnEntityComponent;

import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_1;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_2;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_3;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_4;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_5;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_6;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_7;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_8;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_9;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_C;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_R;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_RAW_MOUSE_MOTION;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwRawMouseMotionSupported;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;

public class Player extends Mob {
    private static final double DEFAULT_MOUSE_SENSITIVITY = 0.11;
    private static final double MIN_MOUSE_SENSITIVITY = 0.02;
    private static final double MAX_MOUSE_SENSITIVITY = 0.30;
    private static final double BREAK_REACH = 6.0;
    private static final double RAYCAST_STEP = 0.1;
    private static final double SURVIVAL_SPEED = 5.0;
    private static final double CREATIVE_SPEED = 8.0;
    private static final double SPRINT_MULTIPLIER = 1.6;
    private static final double FLY_VERTICAL_SPEED = 7.0;
    private static final double FLIGHT_TOGGLE_TAP_SECONDS = 0.35;
    private static final int[] HOTBAR_KEYS = {
            GLFW_KEY_1, GLFW_KEY_2, GLFW_KEY_3, GLFW_KEY_4, GLFW_KEY_5,
            GLFW_KEY_6, GLFW_KEY_7, GLFW_KEY_8, GLFW_KEY_9
    };
    private static final ResourceId TAP_EFFECT = ResourceId.of("game:tap");
    private static final ResourceId JUMP_EFFECT = ResourceId.of("game:jump");

    private final long window;
    private final SoundEffectManager soundEffectManager;
    private final InventoryStack[] hotbarItems;
    private int selectedHotbarSlot;
    private int healthPoints = 20;
    private int hungerPoints = 20;

    private boolean mouseCaptured;
    private boolean hasLastMousePosition;
    private double lastMouseX;
    private double lastMouseY;
    private double mouseSensitivity = DEFAULT_MOUSE_SENSITIVITY;
    private double elapsedSeconds;
    private double lastCreativeSpaceTap = -10.0;
    private boolean creativeFlying;

    public Player(final long window, final World world, final InventoryStack[] hotbarItems, final SoundEffectManager soundEffectManager) {
        super(world);
        this.window = window;
        this.soundEffectManager = soundEffectManager;
        this.hotbarItems = hotbarItems == null ? new InventoryStack[HOTBAR_KEYS.length] : hotbarItems.clone();

        movementSpeed = SURVIVAL_SPEED;
        gravityOn = GameConfig.isSurvival();

        halfWidth = (float) (GameConfig.BLOCK_SIZE * 0.3);
        bodyHeight = (float) (GameConfig.BLOCK_SIZE * 1.8);
        eyeHeight = (float) (GameConfig.BLOCK_SIZE * 1.62);

        hitBox = new HitBox(
                new Vector3f(-halfWidth, 0, -halfWidth),
                new Vector3f(halfWidth, bodyHeight, halfWidth)
        );

        setLocation(-halfWidth, 0, -halfWidth);
    }

    public void update(final InputState input, final double deltaSeconds) {
        elapsedSeconds += Math.max(0.0, deltaSeconds);
        handleHotbarSelectionInput(input);
        handleMouseCapture(input);
        if (mouseCaptured) {
            updateMouseLook();
            handleBlockActions(input);
        }

        final Vector3f movementPerSecond = calculateMovementVector(input, deltaSeconds);
        final Vector3f movementThisFrame = movementPerSecond.mul((float) deltaSeconds);
        move(movementThisFrame);
    }

    private void handleMouseCapture(final InputState input) {
        final boolean shouldCapture = !mouseCaptured
                && (input.isMousePressed(GLFW_MOUSE_BUTTON_LEFT) || input.isMousePressed(GLFW_MOUSE_BUTTON_RIGHT));
        if (shouldCapture) {
            captureMouse();
        }

        if (mouseCaptured && input.isKeyPressed(GLFW_KEY_ESCAPE)) {
            releaseMouse();
        }
    }

    private void updateMouseLook() {
        final double[] mouseX = new double[1];
        final double[] mouseY = new double[1];
        glfwGetCursorPos(window, mouseX, mouseY);

        if (!hasLastMousePosition) {
            hasLastMousePosition = true;
            lastMouseX = mouseX[0];
            lastMouseY = mouseY[0];
            return;
        }

        final double dx = mouseX[0] - lastMouseX;
        final double dy = mouseY[0] - lastMouseY;
        lastMouseX = mouseX[0];
        lastMouseY = mouseY[0];

        setYaw(getYaw() - dx * mouseSensitivity);
        setPitch(getPitch() - dy * mouseSensitivity);
    }

    private void handleBlockActions(final InputState input) {
        if (input.isMousePressed(GLFW_MOUSE_BUTTON_LEFT)) {
            destroyBlockInView();
        }
        if (input.isMousePressed(GLFW_MOUSE_BUTTON_RIGHT)) {
            useItemInView(getSelectedHotbarStack());
        }
    }

    private void handleHotbarSelectionInput(final InputState input) {
        for (int i = 0; i < HOTBAR_KEYS.length; i++) {
            if (input.isKeyPressed(HOTBAR_KEYS[i])) {
                selectedHotbarSlot = i;
            }
        }
    }

    public void scrollHotbar(final double scrollY) {
        final int steps = (int) Math.signum(scrollY) * (int) Math.ceil(Math.abs(scrollY));
        if (steps == 0 || hotbarItems.length == 0) {
            return;
        }
        selectedHotbarSlot = Math.floorMod(selectedHotbarSlot - steps, hotbarItems.length);
    }

    private Vector3f calculateMovementVector(final InputState input, final double deltaSeconds) {
        final double yawRad = Math.toRadians(getYaw());
        final Vector3f forward = new Vector3f(-Math.sin(yawRad), 0, -Math.cos(yawRad));
        final Vector3f right = new Vector3f(Math.cos(yawRad), 0, -Math.sin(yawRad));
        Vector3f movement = Vector3f.ZERO;

        if (input.isKeyDown(GLFW_KEY_W)) {
            movement = movement.add(forward);
        }
        if (input.isKeyDown(GLFW_KEY_S)) {
            movement = movement.sub(forward);
        }
        if (input.isKeyDown(GLFW_KEY_D)) {
            movement = movement.add(right);
        }
        if (input.isKeyDown(GLFW_KEY_A)) {
            movement = movement.sub(right);
        }

        final boolean toggledCreativeFlight = handleCreativeFlightToggle(input);
        final boolean groundedMovement = GameConfig.isSurvival() || (GameConfig.isCreative() && !creativeFlying);
        double speed = GameConfig.isSurvival() ? SURVIVAL_SPEED : CREATIVE_SPEED;
        if (input.isKeyDown(GLFW_KEY_LEFT_SHIFT)) {
            speed *= SPRINT_MULTIPLIER;
        }

        if (movement.lengthSquared() > 0) {
            movement = movement.normalized().mul((float) speed);
        }

        if (groundedMovement) {
            if (input.isKeyPressed(GLFW_KEY_SPACE) && isOnGround && !toggledCreativeFlight) {
                fallVelocity = jumpVelocity;
                isOnGround = false;
                soundEffectManager.play(JUMP_EFFECT);
            }
            fallVelocity += gravity * (float) deltaSeconds;
            if (fallVelocity < maxFallSpeed) {
                fallVelocity = maxFallSpeed;
            }
            movement = movement.add(new Vector3f(0, fallVelocity, 0));
        } else {
            fallVelocity = 0;
            double verticalSpeed = 0;
            if (input.isKeyDown(GLFW_KEY_SPACE)) {
                verticalSpeed += FLY_VERTICAL_SPEED;
            }
            if (input.isKeyDown(GLFW_KEY_LEFT_CONTROL) || input.isKeyDown(GLFW_KEY_C)) {
                verticalSpeed -= FLY_VERTICAL_SPEED;
            }
            movement = movement.add(new Vector3f(0, verticalSpeed, 0));
        }

        if (input.isKeyPressed(GLFW_KEY_R)) {
            final Vector3f spawnPoint = world.getSpawnPoint();
            teleport(spawnPoint.getX(), spawnPoint.getY(), spawnPoint.getZ(), -8.0, getYaw());
        }

        return movement;
    }

    private boolean handleCreativeFlightToggle(final InputState input) {
        if (!GameConfig.isCreative()) {
            creativeFlying = false;
            lastCreativeSpaceTap = -10.0;
            return false;
        }
        if (!input.isKeyPressed(GLFW_KEY_SPACE)) {
            return false;
        }

        final boolean doubleTap = elapsedSeconds - lastCreativeSpaceTap <= FLIGHT_TOGGLE_TAP_SECONDS;
        lastCreativeSpaceTap = elapsedSeconds;
        if (!doubleTap) {
            return false;
        }

        creativeFlying = !creativeFlying;
        fallVelocity = 0;
        lastCreativeSpaceTap = -10.0;
        return true;
    }

    public void destroyBlockInView() {
        final RaycastHit hit = raycastSolidBlock(BREAK_REACH, RAYCAST_STEP);
        if (hit != null) {
            if (world.removeBlock(hit.hitX, hit.hitY, hit.hitZ)) {
                soundEffectManager.play(TAP_EFFECT);
            }
        }
    }

    public void useItemInView(final InventoryStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        final GameObject item = stack.item();
        if (item.has(BlockItemComponent.class)) {
            if (placeBlockItemInView(item.get(BlockItemComponent.class)) && GameConfig.isSurvival()) {
                stack.remove(1);
                if (stack.isEmpty()) {
                    setHotbarStack(selectedHotbarSlot, null);
                }
            }
            return;
        }
        if (item.has(SpawnEntityComponent.class)) {
            return;
        }
    }

    private boolean placeBlockItemInView(final BlockItemComponent blockItem) {
        final RaycastHit hit = raycastSolidBlock(BREAK_REACH, RAYCAST_STEP);
        if (hit == null || !hit.hasPlacementCandidate) {
            return false;
        }

        final double size = GameConfig.BLOCK_SIZE;
        final HitBox candidate = new HitBox(
                new Vector3f(hit.placeX * size, hit.placeY * size, hit.placeZ * size),
                new Vector3f((hit.placeX + 1) * size, (hit.placeY + 1) * size, (hit.placeZ + 1) * size)
        );
        if (candidate.intersects(getHitBox())) {
            return false;
        }

        if (world.placeBlock(hit.placeX, hit.placeY, hit.placeZ, blockItem.blockId(), placementFacing())) {
            soundEffectManager.play(TAP_EFFECT);
            return true;
        }
        return false;
    }

    private BlockFacing placementFacing() {
        final Vector3f viewDirection = getViewDirection();
        return BlockFacing.fromHorizontalVector(viewDirection.getX(), viewDirection.getZ()).opposite();
    }

    private RaycastHit raycastSolidBlock(final double reach, final double step) {
        final Vector3f direction = getViewDirection();
        final Vector3f start = new Vector3f(getCameraX(), getCameraY(), getCameraZ());

        int candidateX = 0;
        int candidateY = 0;
        int candidateZ = 0;
        boolean hasCandidate = false;

        for (double dist = 0; dist <= reach; dist += step) {
            final Vector3f point = start.add(direction.mul((float) dist));
            final int blockX = toBlockCoordinate(point.getX());
            final int blockY = toBlockCoordinate(point.getY());
            final int blockZ = toBlockCoordinate(point.getZ());

            final Block block = world.getBlock(blockX, blockY, blockZ);
            if (block != null) {
                return new RaycastHit(blockX, blockY, blockZ, candidateX, candidateY, candidateZ, hasCandidate);
            }

            candidateX = blockX;
            candidateY = blockY;
            candidateZ = blockZ;
            hasCandidate = true;
        }

        return null;
    }

    private Vector3f getViewDirection() {
        final double yawRad = Math.toRadians(getYaw());
        final double pitchRad = Math.toRadians(getPitch());
        return new Vector3f(
                -Math.sin(yawRad) * Math.cos(pitchRad),
                Math.sin(pitchRad),
                -Math.cos(yawRad) * Math.cos(pitchRad)
        ).normalized();
    }

    public void captureMouse() {
        mouseCaptured = true;
        hasLastMousePosition = false;
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        if (glfwRawMouseMotionSupported()) {
            glfwSetInputMode(window, GLFW_RAW_MOUSE_MOTION, GLFW_TRUE);
        }
    }

    public void releaseMouse() {
        mouseCaptured = false;
        hasLastMousePosition = false;
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        if (glfwRawMouseMotionSupported()) {
            glfwSetInputMode(window, GLFW_RAW_MOUSE_MOTION, 0);
        }
    }

    public boolean isMouseCaptured() {
        return mouseCaptured;
    }

    public int getSelectedHotbarSlot() {
        return selectedHotbarSlot;
    }

    public int getHotbarSize() {
        return hotbarItems.length;
    }

    public GameObject getHotbarItem(final int slotIndex) {
        final InventoryStack stack = getHotbarStack(slotIndex);
        return stack == null ? null : stack.item();
    }

    public InventoryStack getHotbarStack(final int slotIndex) {
        if (slotIndex < 0 || slotIndex >= hotbarItems.length) {
            return null;
        }
        return hotbarItems[slotIndex];
    }

    public void setHotbarItem(final int slotIndex, final GameObject item) {
        setHotbarStack(slotIndex, item == null ? null : InventoryStack.fullStack(item));
    }

    public void setHotbarStack(final int slotIndex, final InventoryStack stack) {
        if (slotIndex < 0 || slotIndex >= hotbarItems.length) {
            return;
        }
        hotbarItems[slotIndex] = stack == null || stack.isEmpty() ? null : stack;
    }

    public double getMouseSensitivity() {
        return mouseSensitivity;
    }

    public void setMouseSensitivity(final double mouseSensitivity) {
        this.mouseSensitivity = Math.max(MIN_MOUSE_SENSITIVITY, Math.min(MAX_MOUSE_SENSITIVITY, mouseSensitivity));
    }

    public void adjustMouseSensitivity(final double delta) {
        setMouseSensitivity(mouseSensitivity + delta);
    }

    public int getHealthPoints() {
        return healthPoints;
    }

    public int getHungerPoints() {
        return hungerPoints;
    }

    public void setHealthPoints(final int healthPoints) {
        this.healthPoints = clampStat(healthPoints);
    }

    public void setHungerPoints(final int hungerPoints) {
        this.hungerPoints = clampStat(hungerPoints);
    }

    public float getFieldOfView() {
        return 75.0f;
    }

    public double getYawDegrees() {
        return getYaw();
    }

    public double getPitchDegrees() {
        return getPitch();
    }

    private InventoryStack getSelectedHotbarStack() {
        return getHotbarStack(selectedHotbarSlot);
    }

    private static int clampStat(final int value) {
        return Math.max(0, Math.min(20, value));
    }

    private static class RaycastHit {
        private final int hitX;
        private final int hitY;
        private final int hitZ;
        private final int placeX;
        private final int placeY;
        private final int placeZ;
        private final boolean hasPlacementCandidate;

        private RaycastHit(final int hitX, final int hitY, final int hitZ, final int placeX, final int placeY, final int placeZ, final boolean hasPlacementCandidate) {
            this.hitX = hitX;
            this.hitY = hitY;
            this.hitZ = hitZ;
            this.placeX = placeX;
            this.placeY = placeY;
            this.placeZ = placeZ;
            this.hasPlacementCandidate = hasPlacementCandidate;
        }
    }
}
