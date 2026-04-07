package me.teamsupre.me.dragonsvsmachines.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import me.teamsupre.me.dragonsvsmachines.Constants;

public class Pig extends GameEntity {
    private float health;
    private static final float HALF_SIZE = Constants.PIG_RADIUS;

    public Pig(World world, float x, float y) {
        super(createBody(world, x, y), Color.GREEN);
        this.health = 15f;
    }

    private static Body createBody(World world, float x, float y) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(x, y);

        Body body = world.createBody(bodyDef);

        PolygonShape box = new PolygonShape();
        box.setAsBox(HALF_SIZE, HALF_SIZE);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = box;
        fixtureDef.density = 1.0f;
        fixtureDef.friction = 0.5f;
        fixtureDef.restitution = 0.1f;
        fixtureDef.filter.categoryBits = Constants.CATEGORY_PIG;

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

    public float getHealth() {
        return health;
    }

    @Override
    public void render(ShapeRenderer renderer) {
        Vector2 pos = body.getPosition();
        float angle = body.getAngle() * MathUtils.radiansToDegrees;
        float size = HALF_SIZE * 2;

        // Darker green when damaged
        float healthPct = Math.max(0, health / 15f);
        renderer.setColor(0f, 0.3f + 0.7f * healthPct, 0f, 1f);
        renderer.rect(pos.x - HALF_SIZE, pos.y - HALF_SIZE,
            HALF_SIZE, HALF_SIZE, size, size, 1f, 1f, angle);

        // Snout accent
        renderer.setColor(0.2f, 0.5f + 0.5f * healthPct, 0.2f, 1f);
        renderer.circle(pos.x, pos.y, HALF_SIZE * 0.4f, 10);
    }
}
