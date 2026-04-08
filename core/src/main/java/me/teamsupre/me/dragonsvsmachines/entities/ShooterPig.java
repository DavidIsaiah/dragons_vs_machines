package me.teamsupre.me.dragonsvsmachines.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import me.teamsupre.me.dragonsvsmachines.Constants;

import java.util.List;

public class ShooterPig extends GameEntity {
    private float health;
    private static final float HALF_SIZE = 0.3f;
    private final float attackRange;
    private float cooldownTimer;
    private static final float COOLDOWN = 2.5f;
    private static final float PROJECTILE_SPEED = 16f;

    // Reusable temp vectors for AI calculations
    private final Vector2 tmpAim = new Vector2();
    private final Vector2 tmpDir = new Vector2();

    public ShooterPig(World world, float x, float y, float attackRange) {
        super(createBody(world, x, y), new Color(0.6f, 0.0f, 0.6f, 1f));
        this.health = 20f;
        this.attackRange = attackRange;
        this.cooldownTimer = 1.0f; // small initial delay before first shot
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
        fixtureDef.density = 1.2f;
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

    public float getAttackRange() {
        return attackRange;
    }

    /**
     * Update AI: find nearest launched bird in range and shoot at it.
     * Returns a new Projectile if firing, or null.
     */
    public Projectile update(float delta, World world, List<Bird> launchedBirds, Bird currentBird) {
        if (isMarkedForRemoval()) return null;

        cooldownTimer -= delta;
        if (cooldownTimer > 0) return null;

        Vector2 myPos = body.getPosition();

        // Find nearest launched bird in range (index-based loop, no iterator)
        Bird target = null;
        float closestDist = Float.MAX_VALUE;

        for (int i = 0, n = launchedBirds.size(); i < n; i++) {
            Bird b = launchedBirds.get(i);
            if (b.isMarkedForRemoval() || !b.isLaunched()) continue;
            float dist = myPos.dst(b.getBody().getPosition());
            if (dist < attackRange && dist < closestDist) {
                closestDist = dist;
                target = b;
            }
        }

        if (currentBird != null && currentBird.isLaunched() && !currentBird.isMarkedForRemoval()) {
            float dist = myPos.dst(currentBird.getBody().getPosition());
            if (dist < attackRange && dist < closestDist) {
                target = currentBird;
            }
        }

        if (target == null) return null;

        // Fire with predictive aiming (reuse tmpAim, tmpDir)
        cooldownTimer = COOLDOWN;
        Vector2 targetPos = target.getBody().getPosition();
        Vector2 targetVel = target.getBody().getLinearVelocity();

        tmpAim.set(targetPos);

        float speed = targetVel.len();
        if (speed > 0.5f) {
            for (int i = 0; i < 2; i++) {
                float dist = myPos.dst(tmpAim);
                float timeToHit = Math.min(dist / PROJECTILE_SPEED, 0.8f);
                tmpAim.set(
                    targetPos.x + targetVel.x * timeToHit,
                    targetPos.y + targetVel.y * timeToHit + 0.5f * (-9.8f) * timeToHit * timeToHit
                );
            }
            if (tmpAim.y < Constants.GROUND_HEIGHT + 0.2f) {
                tmpAim.y = Constants.GROUND_HEIGHT + 0.2f;
            }
        }

        float finalDist = myPos.dst(tmpAim);
        float flightTime = Math.min(finalDist / PROJECTILE_SPEED, 0.8f);
        tmpAim.y += 0.5f * 9.8f * 0.3f * flightTime * flightTime;

        tmpDir.set(tmpAim).sub(myPos).nor();

        // Spawn projectile slightly in front of pig
        float spawnX = myPos.x + tmpDir.x * (HALF_SIZE + 0.15f);
        float spawnY = myPos.y + tmpDir.y * (HALF_SIZE + 0.15f);

        Projectile proj = new Projectile(world, spawnX, spawnY);
        proj.getBody().setLinearVelocity(tmpDir.x * PROJECTILE_SPEED, tmpDir.y * PROJECTILE_SPEED);
        return proj;
    }

    public void renderAttackRange(ShapeRenderer renderer) {
        if (isMarkedForRemoval()) return;
        Vector2 pos = body.getPosition();
        renderer.setColor(0.6f, 0f, 0.6f, 0.08f);
        renderer.circle(pos.x, pos.y, attackRange, 50);
    }

    public void renderAttackRangeOutline(ShapeRenderer renderer) {
        if (isMarkedForRemoval()) return;
        Vector2 pos = body.getPosition();
        renderer.setColor(0.8f, 0f, 0.8f, 0.3f);
        renderer.circle(pos.x, pos.y, attackRange, 50);
    }

    @Override
    public void render(ShapeRenderer renderer) {
        Vector2 pos = body.getPosition();
        float angle = body.getAngle() * MathUtils.radiansToDegrees;
        float size = HALF_SIZE * 2;

        float healthPct = Math.max(0, health / 20f);
        renderer.setColor(
            0.4f + 0.2f * healthPct,
            0f,
            0.4f + 0.2f * healthPct,
            1f
        );
        renderer.rect(pos.x - HALF_SIZE, pos.y - HALF_SIZE,
            HALF_SIZE, HALF_SIZE, size, size, 1f, 1f, angle);

        // Crosshair accent
        renderer.setColor(1f, 0f, 0f, 0.8f);
        renderer.circle(pos.x, pos.y, HALF_SIZE * 0.25f, 8);
        renderer.rectLine(pos.x - HALF_SIZE * 0.5f, pos.y,
            pos.x + HALF_SIZE * 0.5f, pos.y, 0.03f);
        renderer.rectLine(pos.x, pos.y - HALF_SIZE * 0.5f,
            pos.x, pos.y + HALF_SIZE * 0.5f, 0.03f);
    }
}
