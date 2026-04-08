package me.teamsupre.me.dragonsvsmachines.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import me.teamsupre.me.dragonsvsmachines.Constants;

public class Projectile extends GameEntity {
    private static final float RADIUS = 0.1f;
    private static final float MAX_LIFETIME = 3f;
    public static final float KNOCKBACK_FORCE = 4f;
    private float lifetime;

    public Projectile(World world, float x, float y) {
        super(createBody(world, x, y), new Color(1f, 0.2f, 1f, 1f));
        this.lifetime = 0;
    }

    private static Body createBody(World world, float x, float y) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(x, y);
        bodyDef.bullet = true;
        bodyDef.gravityScale = 0.3f; // slight arc

        Body body = world.createBody(bodyDef);

        CircleShape circle = new CircleShape();
        circle.setRadius(RADIUS);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = circle;
        fixtureDef.density = 0.5f;
        fixtureDef.friction = 0.1f;
        fixtureDef.restitution = 0.3f;
        fixtureDef.filter.categoryBits = Constants.CATEGORY_PIG;

        body.createFixture(fixtureDef);
        circle.dispose();

        return body;
    }

    public void update(float delta) {
        lifetime += delta;
        if (lifetime > MAX_LIFETIME) {
            markForRemoval();
        }
        // Remove if stopped (hit something)
        if (lifetime > 0.2f && body.getLinearVelocity().len2() < 1f) {
            markForRemoval();
        }
    }

    @Override
    public void render(ShapeRenderer renderer) {
        Vector2 pos = body.getPosition();
        renderer.setColor(color);
        renderer.circle(pos.x, pos.y, RADIUS, 10);
        // Bright core
        renderer.setColor(1f, 0.8f, 1f, 1f);
        renderer.circle(pos.x, pos.y, RADIUS * 0.5f, 8);
    }
}
