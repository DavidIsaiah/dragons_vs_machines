package me.teamsupre.me.dragonsvsmachines.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import me.teamsupre.me.dragonsvsmachines.Constants;

public class Bird extends GameEntity {
    private boolean launched;

    public Bird(World world, float x, float y) {
        super(createBody(world, x, y), Color.RED);
        this.launched = false;
    }

    private static Body createBody(World world, float x, float y) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(x, y);
        bodyDef.bullet = true;

        Body body = world.createBody(bodyDef);

        CircleShape circle = new CircleShape();
        circle.setRadius(Constants.BIRD_RADIUS);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = circle;
        fixtureDef.density = 2.0f;
        fixtureDef.friction = 0.5f;
        fixtureDef.restitution = 0.2f;
        fixtureDef.filter.categoryBits = Constants.CATEGORY_BIRD;

        body.createFixture(fixtureDef);
        circle.dispose();

        // Start as non-dynamic until launched
        body.setGravityScale(0f);
        body.setAwake(false);

        return body;
    }

    public void launch(Vector2 impulse) {
        body.setGravityScale(1f);
        body.setAwake(true);
        body.applyLinearImpulse(impulse, body.getWorldCenter(), true);
        launched = true;
    }

    public boolean isLaunched() {
        return launched;
    }

    public boolean isStopped() {
        if (!launched) return false;
        return body.getLinearVelocity().len2() < 0.1f;
    }

    @Override
    public void render(ShapeRenderer renderer) {
        Vector2 pos = body.getPosition();
        renderer.setColor(color);
        renderer.circle(pos.x, pos.y, Constants.BIRD_RADIUS, 20);
    }
}
