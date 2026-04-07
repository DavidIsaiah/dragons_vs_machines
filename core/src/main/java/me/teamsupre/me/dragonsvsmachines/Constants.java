package me.teamsupre.me.dragonsvsmachines;

public class Constants {
    // World dimensions in meters
    public static final float WORLD_WIDTH = 16f;
    public static final float WORLD_HEIGHT = 9f;

    // Ground
    public static final float GROUND_HEIGHT = 0.5f;

    // Slingshot
    public static final float SLINGSHOT_X = 3f;
    public static final float SLINGSHOT_Y = GROUND_HEIGHT + 2.0f;
    public static final float MAX_PULL_DISTANCE = 2.5f;
    public static final float LAUNCH_POWER = 5f;

    // Entity sizes
    public static final float BIRD_RADIUS = 0.25f;
    public static final float PIG_RADIUS = 0.25f;
    public static final float BLOCK_HALF_WIDTH = 0.2f;
    public static final float BLOCK_HALF_HEIGHT = 0.5f;

    // Physics
    public static final float PHYSICS_TIME_STEP = 1f / 60f;
    public static final int VELOCITY_ITERATIONS = 6;
    public static final int POSITION_ITERATIONS = 2;

    // Damage
    public static final float PIG_DAMAGE_THRESHOLD = 2f;
    public static final float BLOCK_DAMAGE_THRESHOLD = 3f;

    // Category bits for collision filtering
    public static final short CATEGORY_GROUND = 0x0001;
    public static final short CATEGORY_BIRD = 0x0002;
    public static final short CATEGORY_PIG = 0x0004;
    public static final short CATEGORY_BLOCK = 0x0008;
}
