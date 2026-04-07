package me.teamsupre.me.dragonsvsmachines.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.physics.box2d.Body;

public abstract class GameEntity {
    protected Body body;
    protected boolean markedForRemoval;
    protected Color color;

    public GameEntity(Body body, Color color) {
        this.body = body;
        this.color = color;
        this.markedForRemoval = false;
        body.setUserData(this);
    }

    public Body getBody() {
        return body;
    }

    public boolean isMarkedForRemoval() {
        return markedForRemoval;
    }

    public void markForRemoval() {
        this.markedForRemoval = true;
    }

    public abstract void render(ShapeRenderer renderer);
}
