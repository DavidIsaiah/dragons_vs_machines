package me.teamsupre.me.dragonsvsmachines.physics;

import com.badlogic.gdx.physics.box2d.*;
import me.teamsupre.me.dragonsvsmachines.entities.Bird;
import me.teamsupre.me.dragonsvsmachines.entities.Block;
import me.teamsupre.me.dragonsvsmachines.entities.Pig;

public class ContactHandler implements ContactListener {
    private boolean damageEnabled;

    public ContactHandler() {
        this.damageEnabled = false;
    }

    public void enableDamage() {
        this.damageEnabled = true;
    }

    @Override
    public void beginContact(Contact contact) {
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
        for (float val : impulse.getNormalImpulses()) {
            maxImpulse = Math.max(maxImpulse, val);
        }

        Object dataA = contact.getFixtureA().getBody().getUserData();
        Object dataB = contact.getFixtureB().getBody().getUserData();

        boolean birdInvolved = dataA instanceof Bird || dataB instanceof Bird;

        // Bird collisions: low threshold for direct hits
        // Non-bird collisions (ground hits, debris): low threshold too
        float threshold = birdInvolved ? 0.5f : 0.8f;
        if (maxImpulse < threshold) return;

        float damageMultiplier = birdInvolved ? 20f : 15f;
        float damage = maxImpulse * damageMultiplier;

        applyDamage(dataA, damage);
        applyDamage(dataB, damage);
    }

    private void applyDamage(Object data, float damage) {
        if (data instanceof Pig) {
            ((Pig) data).takeDamage(damage);
        } else if (data instanceof Block) {
            ((Block) data).takeDamage(damage);
        }
    }
}
