package me.teamsupre.me.dragonsvsmachines.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import me.teamsupre.me.dragonsvsmachines.Constants;

import java.util.List;

public class Bird extends GameEntity {
    private boolean launched;
    private boolean abilityUsed;
    private boolean explodeOnImpact;
    private boolean hasCollided;
    private final BirdType type;

    // Reusable temp vector for explosion calculations
    private final Vector2 tmpExpDir = new Vector2();

    public enum BirdType {
        RED("Red", new Color(0.9f, 0.1f, 0.1f, 1f), 2.0f, 0.25f),
        CHUCK("Chuck", new Color(1.0f, 0.85f, 0.0f, 1f), 3.0f, 0.22f),
        BOMB("Bomb", new Color(0.15f, 0.15f, 0.15f, 1f), 3.5f, 0.30f),
        MATILDA("Matilda", new Color(1.0f, 1.0f, 1.0f, 1f), 2.0f, 0.27f),
        BLUES("Blues", new Color(0.3f, 0.5f, 1.0f, 1f), 3.5f, 0.18f);

        public final String name;
        public final Color color;
        public final float density;
        public final float radius;

        BirdType(String name, Color color, float density, float radius) {
            this.name = name;
            this.color = color;
            this.density = density;
            this.radius = radius;
        }
    }

    public Bird(World world, float x, float y, BirdType type) {
        super(createBody(world, x, y, type), type.color);
        this.type = type;
        this.launched = false;
        this.abilityUsed = false;
        this.explodeOnImpact = false;
        this.hasCollided = false;
    }

    // Backwards compatible constructor (defaults to RED)
    public Bird(World world, float x, float y) {
        this(world, x, y, BirdType.RED);
    }

    private static Body createBody(World world, float x, float y, BirdType type) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(x, y);
        bodyDef.bullet = true;

        Body body = world.createBody(bodyDef);

        CircleShape circle = new CircleShape();
        circle.setRadius(type.radius);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = circle;
        fixtureDef.density = type.density;
        fixtureDef.friction = 0.5f;
        fixtureDef.restitution = 0.2f;
        fixtureDef.filter.categoryBits = Constants.CATEGORY_BIRD;

        body.createFixture(fixtureDef);
        circle.dispose();

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

    public boolean isAbilityUsed() {
        return abilityUsed;
    }

    public BirdType getType() {
        return type;
    }

    public boolean canActivateAbility() {
        return launched && !abilityUsed && type != BirdType.RED;
    }

    public boolean isExplodeOnImpact() {
        return explodeOnImpact;
    }

    public void setExplodeOnImpact(boolean value) {
        this.explodeOnImpact = value;
    }

    public boolean hasCollided() {
        return hasCollided;
    }

    public void setHasCollided(boolean value) {
        this.hasCollided = value;
    }

    public void triggerExplosion(World world) {
        activateBomb(world);
    }

    /**
     * Activate the bird's special ability. Returns a list of new entities
     * to add to the world (e.g. Blues splits, Matilda egg), or null.
     */
    public List<GameEntity> activateAbility(World world) {
        if (!canActivateAbility()) return null;
        abilityUsed = true;

        switch (type) {
            case CHUCK:
                activateChuck();
                return null;
            case BOMB:
                activateBomb(world);
                return null;
            case MATILDA:
                return activateMatilda(world);
            case BLUES:
                return activateBlues(world);
            default:
                return null;
        }
    }

    // Chuck: speed boost — doubles current velocity
    private void activateChuck() {
        Vector2 vel = body.getLinearVelocity();
        body.setLinearVelocity(vel.x * 2.5f, vel.y * 2.5f);
    }

    // Bomb: explosion — applies radial impulse to all nearby dynamic bodies
    private void activateBomb(World world) {
        Vector2 pos = body.getPosition();
        float explosionRadius = 3.0f;
        float explosionForce = 15f;

        // Query all bodies in the world
        world.QueryAABB(new QueryCallback() {
            @Override
            public boolean reportFixture(Fixture fixture) {
                Body other = fixture.getBody();
                if (other == body) return true;
                if (other.getType() != BodyDef.BodyType.DynamicBody) return true;

                Vector2 otherPos = other.getPosition();
                float dist = pos.dst(otherPos);
                if (dist < explosionRadius && dist > 0.01f) {
                    tmpExpDir.set(otherPos).sub(pos).nor();
                    float strength = explosionForce * (1f - dist / explosionRadius);
                    tmpExpDir.scl(strength);
                    other.applyLinearImpulse(
                        tmpExpDir,
                        other.getWorldCenter(), true
                    );

                    // Apply damage to pigs and blocks caught in blast
                    Object data = other.getUserData();
                    if (data instanceof Pig) {
                        ((Pig) data).takeDamage(strength * 10f);
                    } else if (data instanceof Block) {
                        ((Block) data).takeDamage(strength * 8f);
                    }
                }
                return true;
            }
        }, pos.x - explosionRadius, pos.y - explosionRadius,
           pos.x + explosionRadius, pos.y + explosionRadius);

        // Destroy the bomb bird itself
        markForRemoval();
    }

    // Matilda: drops an egg projectile downward, bird pops upward
    private java.util.List<GameEntity> activateMatilda(World world) {
        Vector2 pos = body.getPosition();
        Vector2 vel = body.getLinearVelocity();

        // Bird pops upward
        body.setLinearVelocity(vel.x * 0.5f, Math.abs(vel.y) + 5f);

        // Create explosive egg projectile falling down
        Bird egg = new Bird(world, pos.x, pos.y - 0.3f, BirdType.BOMB);
        egg.getBody().setGravityScale(1f);
        egg.getBody().setAwake(true);
        egg.getBody().setLinearVelocity(vel.x * 0.3f, -8f);
        egg.launched = true;
        egg.abilityUsed = true;
        egg.explodeOnImpact = true;
        // Make the egg visually distinct (white/cream)
        egg.color = new Color(0.95f, 0.9f, 0.8f, 1f);

        java.util.List<GameEntity> newEntities = new java.util.ArrayList<GameEntity>();
        newEntities.add(egg);
        return newEntities;
    }

    // Blues: splits into 3 smaller birds in a fan pattern
    private java.util.List<GameEntity> activateBlues(World world) {
        Vector2 pos = body.getPosition();
        Vector2 vel = body.getLinearVelocity();
        float speed = vel.len();
        float baseAngle = vel.angleRad();

        java.util.List<GameEntity> newBirds = new java.util.ArrayList<GameEntity>();

        float[] angles = {-0.3f, 0f, 0.3f}; // fan spread in radians
        for (float offset : angles) {
            float angle = baseAngle + offset;
            Bird split = new Bird(world, pos.x, pos.y, BirdType.BLUES);
            split.getBody().setGravityScale(1f);
            split.getBody().setAwake(true);
            split.getBody().setLinearVelocity(
                MathUtils.cos(angle) * speed,
                MathUtils.sin(angle) * speed
            );
            split.launched = true;
            split.abilityUsed = true;
            newBirds.add(split);
        }

        // Remove original bird
        markForRemoval();
        return newBirds;
    }

    public boolean isStopped() {
        if (!launched) return false;
        return body.getLinearVelocity().len2() < 0.1f;
    }

    @Override
    public void render(ShapeRenderer renderer) {
        Vector2 pos = body.getPosition();
        renderer.setColor(color);
        renderer.circle(pos.x, pos.y, type.radius, 20);

        // Draw visual indicators per type
        switch (type) {
            case CHUCK:
                // Yellow triangle/speed lines
                renderer.setColor(1f, 0.6f, 0f, 1f);
                renderer.triangle(
                    pos.x + type.radius * 0.8f, pos.y,
                    pos.x - type.radius * 0.3f, pos.y + type.radius * 0.4f,
                    pos.x - type.radius * 0.3f, pos.y - type.radius * 0.4f
                );
                break;
            case BOMB:
                // Fuse on top
                renderer.setColor(0.6f, 0.4f, 0.1f, 1f);
                renderer.rectLine(pos.x, pos.y + type.radius,
                    pos.x + 0.1f, pos.y + type.radius + 0.15f, 0.04f);
                renderer.setColor(1f, 0.5f, 0f, 1f);
                renderer.circle(pos.x + 0.1f, pos.y + type.radius + 0.18f, 0.05f, 8);
                break;
            case MATILDA:
                // Eyes / beak accent
                renderer.setColor(1f, 0.4f, 0.6f, 1f);
                renderer.circle(pos.x, pos.y + type.radius * 0.3f, type.radius * 0.2f, 8);
                break;
            case BLUES:
                // Small inner circle accent
                renderer.setColor(0.5f, 0.7f, 1f, 1f);
                renderer.circle(pos.x, pos.y, type.radius * 0.5f, 12);
                break;
            default:
                break;
        }
    }
}
