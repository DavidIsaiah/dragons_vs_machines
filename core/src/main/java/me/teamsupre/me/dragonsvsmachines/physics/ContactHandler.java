package me.teamsupre.me.dragonsvsmachines.physics;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import me.teamsupre.me.dragonsvsmachines.entities.Bird;
import me.teamsupre.me.dragonsvsmachines.entities.Block;
import me.teamsupre.me.dragonsvsmachines.entities.Pig;
import me.teamsupre.me.dragonsvsmachines.entities.Projectile;
import me.teamsupre.me.dragonsvsmachines.entities.ShooterPig;
import me.teamsupre.me.dragonsvsmachines.entities.TNT;

import java.util.ArrayList;
import java.util.List;

public class ContactHandler implements ContactListener {
    private boolean damageEnabled;
    private final List<TNT> tntToDetonate = new ArrayList<TNT>();
    private final Vector2 tmpKnockback = new Vector2();

    public ContactHandler() {
        this.damageEnabled = false;
    }

    public void enableDamage() {
        this.damageEnabled = true;
    }

    public List<TNT> getTntToDetonate() {
        return tntToDetonate;
    }

    @Override
    public void beginContact(Contact contact) {
        if (!damageEnabled) return;

        Object dataA = contact.getFixtureA().getBody().getUserData();
        Object dataB = contact.getFixtureB().getBody().getUserData();

        // Flag launched explosive birds that have collided with something (not another bird)
        if (dataA instanceof Bird && ((Bird) dataA).isExplodeOnImpact()
                && ((Bird) dataA).isLaunched() && !(dataB instanceof Bird)) {
            ((Bird) dataA).setHasCollided(true);
        }
        if (dataB instanceof Bird && ((Bird) dataB).isExplodeOnImpact()
                && ((Bird) dataB).isLaunched() && !(dataA instanceof Bird)) {
            ((Bird) dataB).setHasCollided(true);
        }

        // Projectile hits bird: apply knockback to bird, destroy projectile
        handleProjectileBirdCollision(dataA, dataB, contact);
        handleProjectileBirdCollision(dataB, dataA, contact);
    }

    @Override
    public void endContact(Contact contact) {
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {
        if (!damageEnabled) return;

        float maxImpulse = 0;
        float[] impulses = impulse.getNormalImpulses();
        for (int i = 0; i < impulses.length; i++) {
            if (impulses[i] > maxImpulse) maxImpulse = impulses[i];
        }

        Object dataA = contact.getFixtureA().getBody().getUserData();
        Object dataB = contact.getFixtureB().getBody().getUserData();

        boolean birdInvolved = dataA instanceof Bird || dataB instanceof Bird;

        float threshold = birdInvolved ? 0.5f : 0.8f;
        if (maxImpulse < threshold) return;

        float damageMultiplier = birdInvolved ? 20f : 15f;
        float damage = maxImpulse * damageMultiplier;

        applyDamage(dataA, damage, birdInvolved);
        applyDamage(dataB, damage, birdInvolved);
    }

    private void applyDamage(Object data, float damage, boolean birdInvolved) {
        if (data instanceof ShooterPig) {
            ((ShooterPig) data).takeDamage(damage);
        } else if (data instanceof Pig) {
            ((Pig) data).takeDamage(damage);
        } else if (data instanceof Block) {
            ((Block) data).takeDamage(damage);
        } else if (data instanceof TNT) {
            TNT tnt = (TNT) data;
            if (!tnt.hasDetonated() && !tntToDetonate.contains(tnt) && damage > 25f) {
                tntToDetonate.add(tnt);
            }
        }
    }

    private void handleProjectileBirdCollision(Object maybeProj, Object maybeBird, Contact contact) {
        if (!(maybeProj instanceof Projectile) || !(maybeBird instanceof Bird)) return;
        Projectile proj = (Projectile) maybeProj;
        Bird bird = (Bird) maybeBird;

        // Apply knockback impulse to the bird (reuse tmpKnockback)
        Vector2 projVel = proj.getBody().getLinearVelocity();
        tmpKnockback.set(projVel).nor().scl(Projectile.KNOCKBACK_FORCE);
        bird.getBody().applyLinearImpulse(tmpKnockback, bird.getBody().getWorldCenter(), true);

        // Destroy projectile
        proj.markForRemoval();
    }
}
