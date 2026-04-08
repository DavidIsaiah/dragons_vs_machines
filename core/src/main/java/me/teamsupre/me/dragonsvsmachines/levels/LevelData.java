package me.teamsupre.me.dragonsvsmachines.levels;

import com.badlogic.gdx.physics.box2d.World;
import me.teamsupre.me.dragonsvsmachines.Constants;
import me.teamsupre.me.dragonsvsmachines.entities.Bird;
import me.teamsupre.me.dragonsvsmachines.entities.Bird.BirdType;
import me.teamsupre.me.dragonsvsmachines.entities.Block;
import me.teamsupre.me.dragonsvsmachines.entities.Block.BlockType;
import me.teamsupre.me.dragonsvsmachines.entities.GameEntity;
import me.teamsupre.me.dragonsvsmachines.entities.Pig;
import me.teamsupre.me.dragonsvsmachines.entities.ShooterPig;
import me.teamsupre.me.dragonsvsmachines.entities.TNT;

import java.util.List;

public class LevelData {

    public static float getWorldWidth(int level) {
        if (level == 5) return 40f;
        return Constants.WORLD_WIDTH;
    }

    public static BirdType[] getBirdTypes(int level) {
        switch (level) {
            case 1:
                return new BirdType[]{BirdType.RED, BirdType.RED, BirdType.CHUCK};
            case 2:
                return new BirdType[]{BirdType.RED, BirdType.CHUCK, BirdType.BLUES, BirdType.BOMB};
            case 3:
                return new BirdType[]{BirdType.CHUCK, BirdType.BLUES, BirdType.BOMB, BirdType.MATILDA, BirdType.RED};
            case 4:
                return new BirdType[]{BirdType.BOMB, BirdType.BLUES, BirdType.MATILDA, BirdType.CHUCK, BirdType.BOMB, BirdType.RED, BirdType.RED};
            case 5:
                return new BirdType[]{
                    BirdType.BOMB, BirdType.CHUCK, BirdType.BLUES, BirdType.MATILDA,
                    BirdType.BOMB, BirdType.CHUCK, BirdType.BLUES, BirdType.MATILDA,
                    BirdType.BOMB, BirdType.RED, BirdType.RED, BirdType.RED
                };
            default:
                return new BirdType[]{BirdType.RED, BirdType.RED, BirdType.RED};
        }
    }

    public static void buildLevel(int level, World world, List<GameEntity> entities) {
        switch (level) {
            case 1: buildLevel1(world, entities); break;
            case 2: buildLevel2(world, entities); break;
            case 3: buildLevel3(world, entities); break;
            case 4: buildLevel4(world, entities); break;
            case 5: buildLevel5(world, entities); break;
        }
    }

    // Level 1: Simple tower with one pig on top
    private static void buildLevel1(World world, List<GameEntity> entities) {
        float baseX = 10f;
        float g = Constants.GROUND_HEIGHT;
        float bw = 0.15f;
        float bh = 0.5f;

        // Two vertical pillars
        entities.add(new Block(world, baseX - 0.5f, g + bh, bw, bh, BlockType.WOOD));
        entities.add(new Block(world, baseX + 0.5f, g + bh, bw, bh, BlockType.WOOD));
        // Horizontal plank on top
        entities.add(new Block(world, baseX, g + bh * 2 + bw, bh, bw, BlockType.WOOD));
        // Pig on top
        entities.add(new Pig(world, baseX, g + bh * 2 + bw * 2 + Constants.PIG_RADIUS));
    }

    // Level 2: Two towers with two pigs, TNT between them
    private static void buildLevel2(World world, List<GameEntity> entities) {
        float g = Constants.GROUND_HEIGHT;
        float bw = 0.15f;
        float bh = 0.5f;

        // Tower 1 (wood)
        float x1 = 9f;
        entities.add(new Block(world, x1 - 0.5f, g + bh, bw, bh, BlockType.WOOD));
        entities.add(new Block(world, x1 + 0.5f, g + bh, bw, bh, BlockType.WOOD));
        entities.add(new Block(world, x1, g + bh * 2 + bw, bh, bw, BlockType.WOOD));
        entities.add(new Pig(world, x1, g + bh * 2 + bw * 2 + Constants.PIG_RADIUS));

        // Tower 2 (stone)
        float x2 = 12f;
        entities.add(new Block(world, x2 - 0.5f, g + bh, bw, bh, BlockType.STONE));
        entities.add(new Block(world, x2 + 0.5f, g + bh, bw, bh, BlockType.STONE));
        entities.add(new Block(world, x2, g + bh * 2 + bw, bh, bw, BlockType.STONE));
        entities.add(new Pig(world, x2, g + bh * 2 + bw * 2 + Constants.PIG_RADIUS));

        // TNT between towers
        entities.add(new TNT(world, 10.5f, g + 0.3f));
    }

    // Level 3: Fortress — pigs sit on platforms, not used as structure
    private static void buildLevel3(World world, List<GameEntity> entities) {
        float g = Constants.GROUND_HEIGHT;
        float bw = 0.15f; // thin plank half-width
        float bh = 0.5f;  // tall plank half-height
        float baseX = 10f;

        // === Ground floor: 4 pillars + floor platform ===
        entities.add(new Block(world, baseX - 1.2f, g + bh, bw, bh, BlockType.STONE));
        entities.add(new Block(world, baseX - 0.4f, g + bh, bw, bh, BlockType.WOOD));
        entities.add(new Block(world, baseX + 0.4f, g + bh, bw, bh, BlockType.WOOD));
        entities.add(new Block(world, baseX + 1.2f, g + bh, bw, bh, BlockType.STONE));

        // Floor 1 platform (wide plank spanning pillars)
        float floor1Y = g + bh * 2 + bw;
        entities.add(new Block(world, baseX, floor1Y, 1.4f, bw, BlockType.STONE));

        // Pigs sitting ON the platform
        entities.add(new Pig(world, baseX - 0.5f, floor1Y + bw + Constants.PIG_RADIUS));
        entities.add(new Pig(world, baseX + 0.5f, floor1Y + bw + Constants.PIG_RADIUS));

        // === Second floor: 2 pillars on top of floor 1 platform ===
        float floor1Top = floor1Y + bw;
        entities.add(new Block(world, baseX - 0.5f, floor1Top + bh, bw, bh, BlockType.STONE));
        entities.add(new Block(world, baseX + 0.5f, floor1Top + bh, bw, bh, BlockType.STONE));

        // Floor 2 platform
        float floor2Y = floor1Top + bh * 2 + bw;
        entities.add(new Block(world, baseX, floor2Y, 0.8f, bw, BlockType.WOOD));

        // Pig on top
        entities.add(new Pig(world, baseX, floor2Y + bw + Constants.PIG_RADIUS));

        // TNT inside fortress
        entities.add(new TNT(world, baseX, g + 0.3f));
    }

    // Level 4: Gigantic multi-structure compound
    private static void buildLevel4(World world, List<GameEntity> entities) {
        float g = Constants.GROUND_HEIGHT;
        float bw = 0.15f;
        float bh = 0.5f;

        // ========== LEFT TOWER (tall stone tower) ==========
        float lx = 8f;

        // Ground pillars
        entities.add(new Block(world, lx - 0.5f, g + bh, bw, bh, BlockType.STONE));
        entities.add(new Block(world, lx + 0.5f, g + bh, bw, bh, BlockType.STONE));
        // Floor 1
        float lf1 = g + bh * 2 + bw;
        entities.add(new Block(world, lx, lf1, 0.7f, bw, BlockType.STONE));
        entities.add(new Pig(world, lx, lf1 + bw + Constants.PIG_RADIUS));

        // Second story pillars
        float lf1Top = lf1 + bw;
        entities.add(new Block(world, lx - 0.4f, lf1Top + bh, bw, bh, BlockType.STONE));
        entities.add(new Block(world, lx + 0.4f, lf1Top + bh, bw, bh, BlockType.STONE));
        // Floor 2
        float lf2 = lf1Top + bh * 2 + bw;
        entities.add(new Block(world, lx, lf2, 0.6f, bw, BlockType.STONE));
        entities.add(new Pig(world, lx, lf2 + bw + Constants.PIG_RADIUS));

        // Third story — ice cap
        float lf2Top = lf2 + bw;
        entities.add(new Block(world, lx - 0.3f, lf2Top + bh, bw, bh, BlockType.ICE));
        entities.add(new Block(world, lx + 0.3f, lf2Top + bh, bw, bh, BlockType.ICE));
        float lf3 = lf2Top + bh * 2 + bw;
        entities.add(new Block(world, lx, lf3, 0.5f, bw, BlockType.ICE));

        // ========== MIDDLE BUNKER (low, wide, heavily fortified) ==========
        float mx = 11f;

        // Wide base — 6 pillars
        for (float offset = -1.2f; offset <= 1.2f; offset += 0.8f) {
            entities.add(new Block(world, mx + offset, g + bh, bw, bh, BlockType.WOOD));
        }
        // Wide floor
        float mf1 = g + bh * 2 + bw;
        entities.add(new Block(world, mx - 0.6f, mf1, 0.8f, bw, BlockType.STONE));
        entities.add(new Block(world, mx + 0.6f, mf1, 0.8f, bw, BlockType.STONE));

        // Pigs in the bunker
        entities.add(new Pig(world, mx - 0.6f, mf1 + bw + Constants.PIG_RADIUS));
        entities.add(new Pig(world, mx, mf1 + bw + Constants.PIG_RADIUS));
        entities.add(new Pig(world, mx + 0.6f, mf1 + bw + Constants.PIG_RADIUS));

        // Roof over bunker
        float mf1Top = mf1 + bw + Constants.PIG_RADIUS * 2;
        entities.add(new Block(world, mx - 0.8f, mf1Top + bh * 0.5f, bw, bh * 0.5f, BlockType.STONE));
        entities.add(new Block(world, mx + 0.8f, mf1Top + bh * 0.5f, bw, bh * 0.5f, BlockType.STONE));
        float mRoof = mf1Top + bh + bw;
        entities.add(new Block(world, mx, mRoof, 1.0f, bw, BlockType.STONE));

        // ========== RIGHT TOWER (wood + ice, tall) ==========
        float rx = 14f;

        // Ground pillars
        entities.add(new Block(world, rx - 0.5f, g + bh, bw, bh, BlockType.WOOD));
        entities.add(new Block(world, rx + 0.5f, g + bh, bw, bh, BlockType.WOOD));
        float rf1 = g + bh * 2 + bw;
        entities.add(new Block(world, rx, rf1, 0.7f, bw, BlockType.WOOD));

        // Second story
        float rf1Top = rf1 + bw;
        entities.add(new Block(world, rx - 0.4f, rf1Top + bh, bw, bh, BlockType.ICE));
        entities.add(new Block(world, rx + 0.4f, rf1Top + bh, bw, bh, BlockType.ICE));
        float rf2 = rf1Top + bh * 2 + bw;
        entities.add(new Block(world, rx, rf2, 0.6f, bw, BlockType.WOOD));
        entities.add(new Pig(world, rx, rf2 + bw + Constants.PIG_RADIUS));

        // Third story
        float rf2Top = rf2 + bw;
        entities.add(new Block(world, rx - 0.3f, rf2Top + bh, bw, bh, BlockType.WOOD));
        entities.add(new Block(world, rx + 0.3f, rf2Top + bh, bw, bh, BlockType.WOOD));
        float rf3 = rf2Top + bh * 2 + bw;
        entities.add(new Block(world, rx, rf3, 0.5f, bw, BlockType.ICE));
        entities.add(new Pig(world, rx, rf3 + bw + Constants.PIG_RADIUS));

        // ========== TNT placements ==========
        entities.add(new TNT(world, lx, g + 0.3f));          // inside left tower base
        entities.add(new TNT(world, mx, g + 0.3f));           // under bunker
        entities.add(new TNT(world, rx, rf1Top + 0.3f));      // inside right tower 2nd story
    }

    // Level 5: Massive compound with shooter pigs — 3x larger than level 4
    private static void buildLevel5(World world, List<GameEntity> entities) {
        float g = Constants.GROUND_HEIGHT;
        float bw = 0.15f;
        float bh = 0.5f;

        // Helper: build a standard tower at x position
        // Returns the top Y for placing things on top
        // === SECTION 1: Front guard post (x=8-10) ===
        float s1 = 9f;
        entities.add(new Block(world, s1 - 0.5f, g + bh, bw, bh, BlockType.WOOD));
        entities.add(new Block(world, s1 + 0.5f, g + bh, bw, bh, BlockType.WOOD));
        float s1f1 = g + bh * 2 + bw;
        entities.add(new Block(world, s1, s1f1, 0.7f, bw, BlockType.WOOD));
        entities.add(new Pig(world, s1, s1f1 + bw + Constants.PIG_RADIUS));
        // Shooter pig on top
        float s1f1Top = s1f1 + bw;
        entities.add(new Block(world, s1 - 0.4f, s1f1Top + bh, bw, bh, BlockType.WOOD));
        entities.add(new Block(world, s1 + 0.4f, s1f1Top + bh, bw, bh, BlockType.WOOD));
        float s1f2 = s1f1Top + bh * 2 + bw;
        entities.add(new Block(world, s1, s1f2, 0.6f, bw, BlockType.WOOD));
        entities.add(new ShooterPig(world, s1, s1f2 + bw + 0.3f, 5f));

        // === SECTION 2: Stone fortress (x=13-16) ===
        float s2 = 14.5f;
        // Wide base
        for (float off = -1.2f; off <= 1.2f; off += 0.6f) {
            entities.add(new Block(world, s2 + off, g + bh, bw, bh, BlockType.STONE));
        }
        float s2f1 = g + bh * 2 + bw;
        entities.add(new Block(world, s2 - 0.6f, s2f1, 0.9f, bw, BlockType.STONE));
        entities.add(new Block(world, s2 + 0.6f, s2f1, 0.9f, bw, BlockType.STONE));
        entities.add(new Pig(world, s2 - 0.5f, s2f1 + bw + Constants.PIG_RADIUS));
        entities.add(new Pig(world, s2 + 0.5f, s2f1 + bw + Constants.PIG_RADIUS));
        // Second floor
        float s2f1Top = s2f1 + bw;
        entities.add(new Block(world, s2 - 0.6f, s2f1Top + bh, bw, bh, BlockType.STONE));
        entities.add(new Block(world, s2 + 0.6f, s2f1Top + bh, bw, bh, BlockType.STONE));
        float s2f2 = s2f1Top + bh * 2 + bw;
        entities.add(new Block(world, s2, s2f2, 0.8f, bw, BlockType.STONE));
        entities.add(new ShooterPig(world, s2, s2f2 + bw + 0.3f, 6f));
        entities.add(new TNT(world, s2, g + 0.3f));

        // === SECTION 3: Ice bridge (x=18-22) ===
        float s3L = 19f;
        float s3R = 22f;
        // Left pillar pair
        entities.add(new Block(world, s3L - 0.4f, g + bh, bw, bh, BlockType.ICE));
        entities.add(new Block(world, s3L + 0.4f, g + bh, bw, bh, BlockType.ICE));
        // Right pillar pair
        entities.add(new Block(world, s3R - 0.4f, g + bh, bw, bh, BlockType.ICE));
        entities.add(new Block(world, s3R + 0.4f, g + bh, bw, bh, BlockType.ICE));
        // Long bridge plank
        float s3f1 = g + bh * 2 + bw;
        float bridgeMid = (s3L + s3R) / 2f;
        entities.add(new Block(world, bridgeMid - 0.7f, s3f1, 1.0f, bw, BlockType.ICE));
        entities.add(new Block(world, bridgeMid + 0.7f, s3f1, 1.0f, bw, BlockType.ICE));
        entities.add(new Pig(world, s3L, s3f1 + bw + Constants.PIG_RADIUS));
        entities.add(new Pig(world, bridgeMid, s3f1 + bw + Constants.PIG_RADIUS));
        entities.add(new Pig(world, s3R, s3f1 + bw + Constants.PIG_RADIUS));
        entities.add(new TNT(world, bridgeMid, g + 0.3f));

        // === SECTION 4: Tall sniper tower (x=25) ===
        float s4 = 25f;
        entities.add(new Block(world, s4 - 0.4f, g + bh, bw, bh, BlockType.STONE));
        entities.add(new Block(world, s4 + 0.4f, g + bh, bw, bh, BlockType.STONE));
        float s4f1 = g + bh * 2 + bw;
        entities.add(new Block(world, s4, s4f1, 0.6f, bw, BlockType.STONE));
        // Floor 2
        float s4f1Top = s4f1 + bw;
        entities.add(new Block(world, s4 - 0.3f, s4f1Top + bh, bw, bh, BlockType.STONE));
        entities.add(new Block(world, s4 + 0.3f, s4f1Top + bh, bw, bh, BlockType.STONE));
        float s4f2 = s4f1Top + bh * 2 + bw;
        entities.add(new Block(world, s4, s4f2, 0.5f, bw, BlockType.STONE));
        // Floor 3
        float s4f2Top = s4f2 + bw;
        entities.add(new Block(world, s4 - 0.3f, s4f2Top + bh, bw, bh, BlockType.STONE));
        entities.add(new Block(world, s4 + 0.3f, s4f2Top + bh, bw, bh, BlockType.STONE));
        float s4f3 = s4f2Top + bh * 2 + bw;
        entities.add(new Block(world, s4, s4f3, 0.5f, bw, BlockType.STONE));
        // Shooter pig with long range on top
        entities.add(new ShooterPig(world, s4, s4f3 + bw + 0.3f, 8f));

        // === SECTION 5: Wood bunker maze (x=28-31) ===
        float s5 = 29.5f;
        // Dense wood structure
        for (float off = -1.5f; off <= 1.5f; off += 0.75f) {
            entities.add(new Block(world, s5 + off, g + bh, bw, bh, BlockType.WOOD));
        }
        float s5f1 = g + bh * 2 + bw;
        entities.add(new Block(world, s5 - 0.75f, s5f1, 0.9f, bw, BlockType.WOOD));
        entities.add(new Block(world, s5 + 0.75f, s5f1, 0.9f, bw, BlockType.WOOD));
        entities.add(new Pig(world, s5 - 0.7f, s5f1 + bw + Constants.PIG_RADIUS));
        entities.add(new Pig(world, s5, s5f1 + bw + Constants.PIG_RADIUS));
        entities.add(new Pig(world, s5 + 0.7f, s5f1 + bw + Constants.PIG_RADIUS));
        // Roof
        float s5f1Top = s5f1 + bw + Constants.PIG_RADIUS * 2;
        entities.add(new Block(world, s5 - 1.0f, s5f1Top + bh * 0.4f, bw, bh * 0.4f, BlockType.WOOD));
        entities.add(new Block(world, s5 + 1.0f, s5f1Top + bh * 0.4f, bw, bh * 0.4f, BlockType.WOOD));
        float s5roof = s5f1Top + bh * 0.8f + bw;
        entities.add(new Block(world, s5, s5roof, 1.2f, bw, BlockType.WOOD));
        entities.add(new TNT(world, s5, g + 0.3f));

        // === SECTION 6: Final stronghold (x=33-37) ===
        float s6 = 35f;
        // Massive stone base
        for (float off = -1.5f; off <= 1.5f; off += 0.6f) {
            entities.add(new Block(world, s6 + off, g + bh, bw, bh, BlockType.STONE));
        }
        float s6f1 = g + bh * 2 + bw;
        entities.add(new Block(world, s6 - 0.7f, s6f1, 1.0f, bw, BlockType.STONE));
        entities.add(new Block(world, s6 + 0.7f, s6f1, 1.0f, bw, BlockType.STONE));
        entities.add(new Pig(world, s6 - 0.6f, s6f1 + bw + Constants.PIG_RADIUS));
        entities.add(new Pig(world, s6 + 0.6f, s6f1 + bw + Constants.PIG_RADIUS));
        // Second floor
        float s6f1Top = s6f1 + bw;
        entities.add(new Block(world, s6 - 0.8f, s6f1Top + bh, bw, bh, BlockType.STONE));
        entities.add(new Block(world, s6, s6f1Top + bh, bw, bh, BlockType.STONE));
        entities.add(new Block(world, s6 + 0.8f, s6f1Top + bh, bw, bh, BlockType.STONE));
        float s6f2 = s6f1Top + bh * 2 + bw;
        entities.add(new Block(world, s6 - 0.4f, s6f2, 1.0f, bw, BlockType.STONE));
        entities.add(new Block(world, s6 + 0.4f, s6f2, 1.0f, bw, BlockType.STONE));
        entities.add(new Pig(world, s6, s6f2 + bw + Constants.PIG_RADIUS));
        // Shooter pig on throne
        float s6f2Top = s6f2 + bw;
        entities.add(new Block(world, s6 - 0.4f, s6f2Top + bh, bw, bh, BlockType.STONE));
        entities.add(new Block(world, s6 + 0.4f, s6f2Top + bh, bw, bh, BlockType.STONE));
        float s6f3 = s6f2Top + bh * 2 + bw;
        entities.add(new Block(world, s6, s6f3, 0.6f, bw, BlockType.STONE));
        entities.add(new ShooterPig(world, s6, s6f3 + bw + 0.3f, 7f));
        entities.add(new TNT(world, s6, g + 0.3f));
        entities.add(new TNT(world, s6, s6f1Top + 0.3f));
    }
}
