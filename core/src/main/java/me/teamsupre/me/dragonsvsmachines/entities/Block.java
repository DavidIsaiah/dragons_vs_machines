package me.teamsupre.me.dragonsvsmachines.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import me.teamsupre.me.dragonsvsmachines.Constants;

public class Block extends GameEntity {
    private float health;
    private final float halfWidth;
    private final float halfHeight;
    private final BlockType type;

    public enum BlockType {
        WOOD(new Color(0.6f, 0.35f, 0.1f, 1f), 60f),
        STONE(new Color(0.5f, 0.5f, 0.5f, 1f), 150f),
        ICE(new Color(0.6f, 0.85f, 1f, 1f), 30f);

        public final Color color;
        public final float maxHealth;

        BlockType(Color color, float maxHealth) {
            this.color = color;
            this.maxHealth = maxHealth;
        }
    }

    public Block(World world, float x, float y, float halfWidth, float halfHeight, BlockType type) {
        super(createBody(world, x, y, halfWidth, halfHeight, type), type.color);
        this.halfWidth = halfWidth;
        this.halfHeight = halfHeight;
        this.type = type;
        this.health = type.maxHealth;
    }

    private static Body createBody(World world, float x, float y, float hw, float hh, BlockType type) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(x, y);

        Body body = world.createBody(bodyDef);

        PolygonShape box = new PolygonShape();
        box.setAsBox(hw, hh);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = box;
        fixtureDef.density = type == BlockType.STONE ? 3.0f : type == BlockType.ICE ? 0.5f : 1.5f;
        fixtureDef.friction = 0.6f;
        fixtureDef.restitution = type == BlockType.ICE ? 0.05f : 0.1f;
        fixtureDef.filter.categoryBits = Constants.CATEGORY_BLOCK;

        body.createFixture(fixtureDef);
        box.dispose();

        return body;
    }

    public void takeDamage(float amount) {
        health -= amount;
        if (health <= 0) {
            markForRemoval();
        }
    }

    @Override
    public void render(ShapeRenderer renderer) {
        Vector2 pos = body.getPosition();
        float angle = body.getAngle() * MathUtils.radiansToDegrees;

        float healthPct = Math.max(0, health / type.maxHealth);
        renderer.setColor(
            type.color.r * (0.3f + 0.7f * healthPct),
            type.color.g * (0.3f + 0.7f * healthPct),
            type.color.b * (0.3f + 0.7f * healthPct),
            1f
        );

        float w = halfWidth * 2;
        float h = halfHeight * 2;
        renderer.rect(pos.x - halfWidth, pos.y - halfHeight,
            halfWidth, halfHeight, w, h, 1f, 1f, angle);
    }
}
