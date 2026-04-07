package me.teamsupre.me.dragonsvsmachines.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import me.teamsupre.me.dragonsvsmachines.Constants;

public class TNT extends GameEntity {
    private static final float HALF_SIZE = 0.3f;
    public static final float EXPLOSION_RADIUS = 1.8f;
    private static final float EXPLOSION_FORCE = 10f;

    private boolean hasDetonated;

    // For debug visualization of last explosion
    private static Vector2 lastExplosionPos = null;
    private static float lastExplosionTimer = 0;

    public TNT(World world, float x, float y) {
        super(createBody(world, x, y), new Color(0.9f, 0.15f, 0.0f, 1f));
        this.hasDetonated = false;
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
        fixtureDef.density = 1.5f;
        fixtureDef.friction = 0.5f;
        fixtureDef.restitution = 0.1f;
        fixtureDef.filter.categoryBits = Constants.CATEGORY_BLOCK;

        body.createFixture(fixtureDef);
        box.dispose();

        return body;
    }

    public boolean hasDetonated() {
        return hasDetonated;
    }

    public float detonate(World world) {
        if (hasDetonated) return 0;
        hasDetonated = true;

        Vector2 pos = body.getPosition();

        // Store for debug drawing
        lastExplosionPos = new Vector2(pos);
        lastExplosionTimer = 1.5f;

        world.QueryAABB(new QueryCallback() {
            @Override
            public boolean reportFixture(Fixture fixture) {
                Body other = fixture.getBody();
                if (other == body) return true;
                if (other.getType() != BodyDef.BodyType.DynamicBody) return true;

                Vector2 otherPos = other.getPosition();
                float dist = pos.dst(otherPos);
                if (dist < EXPLOSION_RADIUS && dist > 0.01f) {
                    Vector2 direction = new Vector2(otherPos).sub(pos).nor();
                    float strength = EXPLOSION_FORCE * (1f - dist / EXPLOSION_RADIUS);
                    other.applyLinearImpulse(
                        direction.scl(strength),
                        other.getWorldCenter(), true
                    );

                    Object data = other.getUserData();
                    if (data instanceof Pig) {
                        ((Pig) data).takeDamage(strength * 12f);
                    } else if (data instanceof Block) {
                        ((Block) data).takeDamage(strength * 10f);
                    }
                    // No direct TNT chain reaction — let physics debris trigger other TNTs
                }
                return true;
            }
        }, pos.x - EXPLOSION_RADIUS, pos.y - EXPLOSION_RADIUS,
           pos.x + EXPLOSION_RADIUS, pos.y + EXPLOSION_RADIUS);

        markForRemoval();
        return EXPLOSION_FORCE;
    }

    /**
     * Draw debug explosion radius circles. Call from GameScreen after shapeRenderer.begin(Line).
     */
    public static void renderDebugExplosions(ShapeRenderer renderer, float delta) {
        if (lastExplosionPos != null && lastExplosionTimer > 0) {
            lastExplosionTimer -= delta;
            float alpha = Math.min(1f, lastExplosionTimer);
            renderer.setColor(1f, 0.3f, 0f, alpha * 0.6f);
            renderer.circle(lastExplosionPos.x, lastExplosionPos.y, EXPLOSION_RADIUS, 40);
            if (lastExplosionTimer <= 0) {
                lastExplosionPos = null;
            }
        }
    }

    /**
     * Draw the blast radius around each TNT (call in Filled mode for all TNTs).
     */
    public void renderBlastRadius(ShapeRenderer renderer) {
        if (hasDetonated) return;
        Vector2 pos = body.getPosition();
        renderer.setColor(1f, 0.2f, 0f, 0.08f);
        renderer.circle(pos.x, pos.y, EXPLOSION_RADIUS, 40);
    }

    @Override
    public void render(ShapeRenderer renderer) {
        Vector2 pos = body.getPosition();
        float angle = body.getAngle() * MathUtils.radiansToDegrees;
        float size = HALF_SIZE * 2;

        // Red box
        renderer.setColor(color);
        renderer.rect(pos.x - HALF_SIZE, pos.y - HALF_SIZE,
            HALF_SIZE, HALF_SIZE, size, size, 1f, 1f, angle);

        // TNT label stripe
        renderer.setColor(0.6f, 0.1f, 0.0f, 1f);
        renderer.rect(pos.x - HALF_SIZE, pos.y - HALF_SIZE * 0.3f,
            HALF_SIZE, HALF_SIZE * 0.3f, size, HALF_SIZE * 0.6f, 1f, 1f, angle);
    }
}
