package me.teamsupre.me.dragonsvsmachines.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import me.teamsupre.me.dragonsvsmachines.Constants;

public class Pig extends GameEntity {
    private float health;

    public Pig(World world, float x, float y) {
        super(createBody(world, x, y), Color.GREEN);
        this.health = 15f;
    }

    private static Body createBody(World world, float x, float y) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(x, y);

        Body body = world.createBody(bodyDef);

        CircleShape circle = new CircleShape();
        circle.setRadius(Constants.PIG_RADIUS);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = circle;
        fixtureDef.density = 1.0f;
        fixtureDef.friction = 0.5f;
        fixtureDef.restitution = 0.1f;
        fixtureDef.filter.categoryBits = Constants.CATEGORY_PIG;

        body.createFixture(fixtureDef);
        circle.dispose();

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
        // Darker green when damaged
        float healthPct = Math.max(0, health / 15f);
        renderer.setColor(0f, 0.3f + 0.7f * healthPct, 0f, 1f);
        renderer.circle(pos.x, pos.y, Constants.PIG_RADIUS, 20);
    }
}
