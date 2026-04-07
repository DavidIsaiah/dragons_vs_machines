package me.teamsupre.me.dragonsvsmachines.levels;

import com.badlogic.gdx.physics.box2d.World;
import me.teamsupre.me.dragonsvsmachines.Constants;
import me.teamsupre.me.dragonsvsmachines.entities.Bird;
import me.teamsupre.me.dragonsvsmachines.entities.Bird.BirdType;
import me.teamsupre.me.dragonsvsmachines.entities.Block;
import me.teamsupre.me.dragonsvsmachines.entities.Block.BlockType;
import me.teamsupre.me.dragonsvsmachines.entities.GameEntity;
import me.teamsupre.me.dragonsvsmachines.entities.Pig;
import me.teamsupre.me.dragonsvsmachines.entities.TNT;

import java.util.List;

public class LevelData {

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
}
