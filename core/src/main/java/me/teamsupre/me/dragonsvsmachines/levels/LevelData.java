package me.teamsupre.me.dragonsvsmachines.levels;

import com.badlogic.gdx.physics.box2d.World;
import me.teamsupre.me.dragonsvsmachines.Constants;
import me.teamsupre.me.dragonsvsmachines.entities.Block;
import me.teamsupre.me.dragonsvsmachines.entities.Block.BlockType;
import me.teamsupre.me.dragonsvsmachines.entities.GameEntity;
import me.teamsupre.me.dragonsvsmachines.entities.Pig;

import java.util.List;

public class LevelData {

    public static int getBirdCount(int level) {
        switch (level) {
            case 1: return 3;
            case 2: return 4;
            case 3: return 5;
            default: return 3;
        }
    }

    public static void buildLevel(int level, World world, List<GameEntity> entities) {
        switch (level) {
            case 1: buildLevel1(world, entities); break;
            case 2: buildLevel2(world, entities); break;
            case 3: buildLevel3(world, entities); break;
        }
    }

    // Level 1: Simple tower with one pig
    private static void buildLevel1(World world, List<GameEntity> entities) {
        float baseX = 10f;
        float groundTop = Constants.GROUND_HEIGHT;
        float bw = 0.15f; // block half-width (thin plank)
        float bh = 0.5f;  // block half-height (tall plank)

        // Two vertical pillars
        entities.add(new Block(world, baseX - 0.5f, groundTop + bh, bw, bh, BlockType.WOOD));
        entities.add(new Block(world, baseX + 0.5f, groundTop + bh, bw, bh, BlockType.WOOD));

        // Horizontal plank on top
        entities.add(new Block(world, baseX, groundTop + bh * 2 + bw, bh, bw, BlockType.WOOD));

        // Pig sitting on top
        entities.add(new Pig(world, baseX, groundTop + bh * 2 + bw * 2 + Constants.PIG_RADIUS));
    }

    // Level 2: Two towers with two pigs
    private static void buildLevel2(World world, List<GameEntity> entities) {
        float groundTop = Constants.GROUND_HEIGHT;
        float bw = 0.15f;
        float bh = 0.5f;

        // Tower 1
        float x1 = 9f;
        entities.add(new Block(world, x1 - 0.5f, groundTop + bh, bw, bh, BlockType.WOOD));
        entities.add(new Block(world, x1 + 0.5f, groundTop + bh, bw, bh, BlockType.WOOD));
        entities.add(new Block(world, x1, groundTop + bh * 2 + bw, bh, bw, BlockType.WOOD));
        entities.add(new Pig(world, x1, groundTop + bh * 2 + bw * 2 + Constants.PIG_RADIUS));

        // Tower 2 (stone, tougher)
        float x2 = 12f;
        entities.add(new Block(world, x2 - 0.5f, groundTop + bh, bw, bh, BlockType.STONE));
        entities.add(new Block(world, x2 + 0.5f, groundTop + bh, bw, bh, BlockType.STONE));
        entities.add(new Block(world, x2, groundTop + bh * 2 + bw, bh, bw, BlockType.STONE));
        entities.add(new Pig(world, x2, groundTop + bh * 2 + bw * 2 + Constants.PIG_RADIUS));
    }

    // Level 3: Fortress with three pigs, mixed materials
    private static void buildLevel3(World world, List<GameEntity> entities) {
        float groundTop = Constants.GROUND_HEIGHT;
        float bw = 0.15f;
        float bh = 0.5f;

        float baseX = 10f;

        // Ground floor - wide base with ice walls
        entities.add(new Block(world, baseX - 1.5f, groundTop + bh, bw, bh, BlockType.ICE));
        entities.add(new Block(world, baseX - 0.5f, groundTop + bh, bw, bh, BlockType.WOOD));
        entities.add(new Block(world, baseX + 0.5f, groundTop + bh, bw, bh, BlockType.WOOD));
        entities.add(new Block(world, baseX + 1.5f, groundTop + bh, bw, bh, BlockType.ICE));

        // Floor 1
        entities.add(new Block(world, baseX - 0.5f, groundTop + bh * 2 + bw, 1.0f, bw, BlockType.STONE));
        entities.add(new Block(world, baseX + 0.5f, groundTop + bh * 2 + bw, 1.0f, bw, BlockType.STONE));

        // Pig on floor 1
        entities.add(new Pig(world, baseX - 0.5f, groundTop + bh * 2 + bw * 2 + Constants.PIG_RADIUS));
        entities.add(new Pig(world, baseX + 0.5f, groundTop + bh * 2 + bw * 2 + Constants.PIG_RADIUS));

        // Second floor pillars
        float floor2Base = groundTop + bh * 2 + bw * 2 + Constants.PIG_RADIUS * 2;
        entities.add(new Block(world, baseX - 0.5f, floor2Base + bh, bw, bh, BlockType.STONE));
        entities.add(new Block(world, baseX + 0.5f, floor2Base + bh, bw, bh, BlockType.STONE));

        // Roof
        entities.add(new Block(world, baseX, floor2Base + bh * 2 + bw, bh, bw, BlockType.WOOD));

        // Pig on top
        entities.add(new Pig(world, baseX, floor2Base + bh * 2 + bw * 2 + Constants.PIG_RADIUS));
    }
}
