package me.teamsupre.me.dragonsvsmachines.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.viewport.FitViewport;
import me.teamsupre.me.dragonsvsmachines.Constants;
import me.teamsupre.me.dragonsvsmachines.DragonsVsMachines;
import me.teamsupre.me.dragonsvsmachines.entities.Bird;
import me.teamsupre.me.dragonsvsmachines.entities.Bird.BirdType;
import me.teamsupre.me.dragonsvsmachines.entities.GameEntity;
import me.teamsupre.me.dragonsvsmachines.entities.Pig;
import me.teamsupre.me.dragonsvsmachines.entities.Projectile;
import me.teamsupre.me.dragonsvsmachines.entities.ShooterPig;
import me.teamsupre.me.dragonsvsmachines.entities.TNT;
import me.teamsupre.me.dragonsvsmachines.levels.LevelData;
import me.teamsupre.me.dragonsvsmachines.physics.ContactHandler;

import java.util.ArrayList;
import java.util.List;

public class GameScreen extends InputAdapter implements Screen {
    private final DragonsVsMachines game;
    private final int level;
    private float worldWidth;

    // Projectiles from shooter pigs
    private final ArrayList<Projectile> projectiles = new ArrayList<Projectile>();

    // Physics
    private World world;
    private Box2DDebugRenderer debugRenderer;
    private ContactHandler contactHandler;

    // Camera
    private OrthographicCamera camera;
    private FitViewport viewport;

    // Rendering
    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private BitmapFont font;
    private GlyphLayout layout;

    // Game entities
    private final ArrayList<GameEntity> entities = new ArrayList<GameEntity>();
    private final ArrayList<Bird> availableBirds = new ArrayList<Bird>();
    private final ArrayList<Bird> launchedBirds = new ArrayList<Bird>();
    private Bird currentBird;

    // Slingshot state
    private boolean dragging;
    private final Vector2 dragPoint = new Vector2();
    private final Vector2 slingshotAnchor = new Vector2();

    // Reusable temp vectors — NEVER return these or store references
    private final Vector2 tmpVec1 = new Vector2();
    private final Vector2 tmpVec2 = new Vector2();

    // Game state
    private enum GameState { AIMING, FLYING, SETTLING, WON, LOST }
    private GameState state;
    private float settleTimer;
    private float stateTimer;

    // Camera shake
    private float shakeIntensity;
    private float shakeDuration;
    private float shakeTimer;

    public GameScreen(DragonsVsMachines game, int level) {
        this.game = game;
        this.level = level;
    }

    @Override
    public void show() {
        worldWidth = LevelData.getWorldWidth(level);

        camera = new OrthographicCamera();
        float viewHeight = Constants.WORLD_HEIGHT;
        float viewWidth = Math.max(Constants.WORLD_WIDTH, worldWidth);
        viewport = new FitViewport(viewWidth, viewHeight, camera);
        camera.position.set(worldWidth / 2f, viewHeight / 2f, 0);
        camera.update();

        world = new World(new Vector2(0, -9.8f), true);
        contactHandler = new ContactHandler();
        world.setContactListener(contactHandler);
        debugRenderer = new Box2DDebugRenderer();

        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.setUseIntegerPositions(false);
        font.getData().setScale(viewport.getWorldHeight() / Gdx.graphics.getHeight() * 2f);
        layout = new GlyphLayout();

        Gdx.input.setInputProcessor(this);
        slingshotAnchor.set(Constants.SLINGSHOT_X, Constants.SLINGSHOT_Y);

        entities.clear();
        projectiles.clear();
        availableBirds.clear();
        launchedBirds.clear();

        createGround();
        LevelData.buildLevel(level, world, entities);

        BirdType[] birdTypes = LevelData.getBirdTypes(level);
        for (int i = 0; i < birdTypes.length; i++) {
            availableBirds.add(new Bird(world, -10, -10, birdTypes[i]));
        }

        loadNextBird();
        state = GameState.AIMING;
        dragging = false;
        settleTimer = 0;
        stateTimer = 0;
    }

    private void createGround() {
        BodyDef groundDef = new BodyDef();
        groundDef.type = BodyDef.BodyType.StaticBody;
        groundDef.position.set(worldWidth / 2f, Constants.GROUND_HEIGHT / 2f);
        Body ground = world.createBody(groundDef);
        PolygonShape groundBox = new PolygonShape();
        groundBox.setAsBox(worldWidth / 2f, Constants.GROUND_HEIGHT / 2f);
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = groundBox;
        fixtureDef.friction = 0.8f;
        fixtureDef.filter.categoryBits = Constants.CATEGORY_GROUND;
        ground.createFixture(fixtureDef);
        groundBox.dispose();

        BodyDef wallDef = new BodyDef();
        wallDef.type = BodyDef.BodyType.StaticBody;
        wallDef.position.set(worldWidth, Constants.WORLD_HEIGHT / 2f);
        Body rightWall = world.createBody(wallDef);
        PolygonShape wallShape = new PolygonShape();
        wallShape.setAsBox(0.1f, Constants.WORLD_HEIGHT / 2f);
        rightWall.createFixture(wallShape, 0f);
        wallShape.dispose();
    }

    private void loadNextBird() {
        if (!availableBirds.isEmpty()) {
            currentBird = availableBirds.remove(0);
            currentBird.getBody().setTransform(slingshotAnchor, 0);
            currentBird.getBody().setLinearVelocity(0, 0);
            currentBird.getBody().setAngularVelocity(0);
        } else {
            currentBird = null;
        }
    }

    // --- Input handling (use tmpVec1 for unprojection) ---

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (state == GameState.WON || state == GameState.LOST) {
            game.setScreen(new LevelSelectScreen(game));
            return true;
        }

        if ((state == GameState.FLYING || state == GameState.SETTLING) && currentBird != null && currentBird.canActivateAbility()) {
            BirdType activatingType = currentBird.getType();
            List<GameEntity> newEntities = currentBird.activateAbility(world);
            if (newEntities != null) {
                for (int i = 0, n = newEntities.size(); i < n; i++) {
                    GameEntity e = newEntities.get(i);
                    if (e instanceof Bird) {
                        launchedBirds.add((Bird) e);
                    } else {
                        entities.add(e);
                    }
                }
            }
            if (activatingType == BirdType.BOMB) {
                triggerShake(0.2f, 0.5f);
            } else if (activatingType == BirdType.BLUES) {
                triggerShake(0.08f, 0.2f);
            }
            return true;
        }

        if (state != GameState.AIMING || currentBird == null) return false;

        tmpVec1.set(screenX, screenY);
        viewport.unproject(tmpVec1);

        float distToBird = tmpVec1.dst(currentBird.getBody().getPosition());
        if (distToBird < 1.0f) {
            dragging = true;
            dragPoint.set(tmpVec1);
            return true;
        }
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (!dragging) return false;

        tmpVec1.set(screenX, screenY);
        viewport.unproject(tmpVec1);
        dragPoint.set(tmpVec1);

        // Clamp pull distance (reuse tmpVec1)
        tmpVec1.set(dragPoint).sub(slingshotAnchor);
        if (tmpVec1.len() > Constants.MAX_PULL_DISTANCE) {
            tmpVec1.nor().scl(Constants.MAX_PULL_DISTANCE);
            dragPoint.set(slingshotAnchor).add(tmpVec1);
        }

        currentBird.getBody().setTransform(dragPoint, 0);
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (!dragging) return false;
        dragging = false;

        // Calculate launch impulse (reuse tmpVec1)
        tmpVec1.set(slingshotAnchor).sub(dragPoint);
        if (tmpVec1.len() < 0.2f) {
            currentBird.getBody().setTransform(slingshotAnchor, 0);
            return true;
        }

        tmpVec1.scl(Constants.LAUNCH_POWER);
        contactHandler.enableDamage();
        currentBird.launch(tmpVec1);
        launchedBirds.add(currentBird);
        state = GameState.FLYING;
        stateTimer = 0;
        return true;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK) {
            game.setScreen(new LevelSelectScreen(game));
            return true;
        }
        if (keycode == Input.Keys.R) {
            game.setScreen(new GameScreen(game, level));
            return true;
        }
        return false;
    }

    private void triggerShake(float intensity, float duration) {
        if (intensity > shakeIntensity) {
            shakeIntensity = intensity;
        }
        shakeDuration = Math.max(shakeDuration, duration);
        shakeTimer = 0;
    }

    // --- Update & Render (all loops use index-based iteration) ---

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 0.05f);
        update(delta);

        Gdx.gl.glClearColor(0.4f, 0.65f, 0.9f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Camera shake
        camera.position.set(worldWidth / 2f, Constants.WORLD_HEIGHT / 2f, 0);
        if (shakeTimer < shakeDuration) {
            shakeTimer += delta;
            float damping = 1f - shakeTimer / shakeDuration;
            camera.position.x += MathUtils.random(-shakeIntensity, shakeIntensity) * damping;
            camera.position.y += MathUtils.random(-shakeIntensity, shakeIntensity) * damping;
        } else {
            shakeIntensity = 0;
            shakeDuration = 0;
        }
        camera.update();

        // --- Filled shapes ---
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Ground
        shapeRenderer.setColor(0.3f, 0.6f, 0.2f, 1f);
        shapeRenderer.rect(0, 0, worldWidth, Constants.GROUND_HEIGHT);

        // Slingshot
        shapeRenderer.setColor(0.4f, 0.25f, 0.1f, 1f);
        shapeRenderer.rect(Constants.SLINGSHOT_X - 0.08f, Constants.GROUND_HEIGHT,
            0.16f, Constants.SLINGSHOT_Y - Constants.GROUND_HEIGHT + 0.3f);
        shapeRenderer.rect(Constants.SLINGSHOT_X - 0.25f, Constants.SLINGSHOT_Y + 0.1f, 0.12f, 0.4f);
        shapeRenderer.rect(Constants.SLINGSHOT_X + 0.13f, Constants.SLINGSHOT_Y + 0.1f, 0.12f, 0.4f);

        // Entities + debug overlays (single pass)
        for (int i = 0, n = entities.size(); i < n; i++) {
            GameEntity entity = entities.get(i);
            entity.render(shapeRenderer);
            if (entity instanceof TNT) {
                ((TNT) entity).renderBlastRadius(shapeRenderer);
            } else if (entity instanceof ShooterPig) {
                ((ShooterPig) entity).renderAttackRange(shapeRenderer);
            }
        }

        // Projectiles
        for (int i = 0, n = projectiles.size(); i < n; i++) {
            projectiles.get(i).render(shapeRenderer);
        }

        // Current bird
        if (currentBird != null && !currentBird.isMarkedForRemoval()) {
            currentBird.render(shapeRenderer);
        }

        // Launched birds
        for (int i = 0, n = launchedBirds.size(); i < n; i++) {
            Bird b = launchedBirds.get(i);
            if (!b.isMarkedForRemoval()) {
                b.render(shapeRenderer);
            }
        }

        shapeRenderer.end();

        // --- Elastic band + trajectory (while dragging) ---
        if (dragging && currentBird != null) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(0.3f, 0.15f, 0.05f, 1f);
            Gdx.gl.glLineWidth(3f);

            Vector2 birdPos = currentBird.getBody().getPosition();
            shapeRenderer.line(Constants.SLINGSHOT_X - 0.19f, Constants.SLINGSHOT_Y + 0.45f,
                birdPos.x, birdPos.y);
            shapeRenderer.line(Constants.SLINGSHOT_X + 0.19f, Constants.SLINGSHOT_Y + 0.45f,
                birdPos.x, birdPos.y);
            shapeRenderer.end();
            Gdx.gl.glLineWidth(1f);

            // Trajectory prediction (reuse tmpVec1 for pull calc)
            tmpVec1.set(slingshotAnchor).sub(dragPoint);
            if (tmpVec1.len() > 0.2f) {
                float mass = currentBird.getBody().getMass();
                float simVx = tmpVec1.x * Constants.LAUNCH_POWER / mass;
                float simVy = tmpVec1.y * Constants.LAUNCH_POWER / mass;
                float gravity = -9.8f;
                float groundY = Constants.GROUND_HEIGHT + Constants.BIRD_RADIUS;
                float restitution = 0.2f;
                float friction = 0.5f;
                float simX = dragPoint.x;
                float simY = dragPoint.y;
                float dt = 0.02f;

                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                int numDots = 16;
                int stepsPerDot = 4;
                for (int i = 0; i < numDots; i++) {
                    for (int s = 0; s < stepsPerDot; s++) {
                        simVy += gravity * dt;
                        simX += simVx * dt;
                        simY += simVy * dt;
                        if (simY <= groundY) {
                            simY = groundY;
                            simVy = -simVy * restitution;
                            simVx *= (1f - friction * dt * 5f);
                            if (Math.abs(simVy) < 0.3f) simVy = 0;
                        }
                        if (simX >= worldWidth) {
                            simX = worldWidth;
                            simVx = -simVx * restitution;
                        }
                    }
                    if (simY <= groundY + 0.01f && Math.abs(simVy) < 0.1f && Math.abs(simVx) < 0.3f) break;
                    if (simX < -1f) break;

                    float alpha = 1f - (float) i / numDots;
                    shapeRenderer.setColor(1f, 1f, 1f, alpha * 0.7f);
                    shapeRenderer.circle(simX, simY, 0.05f + 0.03f * (1f - alpha), 8);
                }
                shapeRenderer.end();
            }
        }

        // Box2D debug
        debugRenderer.render(world, camera.combined);

        // Debug outlines (shooter pig range + TNT explosion)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(2f);
        TNT.renderDebugExplosions(shapeRenderer, delta);
        for (int i = 0, n = entities.size(); i < n; i++) {
            GameEntity entity = entities.get(i);
            if (entity instanceof ShooterPig) {
                ((ShooterPig) entity).renderAttackRangeOutline(shapeRenderer);
            }
        }
        shapeRenderer.end();
        Gdx.gl.glLineWidth(1f);

        // --- HUD ---
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        font.setColor(Color.WHITE);
        int birdsLeft = availableBirds.size();
        String birdName = currentBird != null ? currentBird.getType().name : "";
        font.draw(batch, "Birds: " + birdsLeft + "  |  " + birdName + "  |  Level " + level + "  |  ESC=Menu  R=Restart",
            0.3f, Constants.WORLD_HEIGHT - 0.3f);

        int pigsLeft = 0;
        for (int i = 0, n = entities.size(); i < n; i++) {
            GameEntity e = entities.get(i);
            if ((e instanceof Pig || e instanceof ShooterPig) && !e.isMarkedForRemoval()) pigsLeft++;
        }
        font.draw(batch, "Pigs: " + pigsLeft, 0.3f, Constants.WORLD_HEIGHT - 0.8f);

        // Ability hint
        if (state == GameState.FLYING && currentBird != null && currentBird.canActivateAbility()) {
            font.setColor(Color.YELLOW);
            String hint = "";
            switch (currentBird.getType()) {
                case CHUCK: hint = "TAP for Speed Boost!"; break;
                case BOMB: hint = "TAP to Explode!"; break;
                case MATILDA: hint = "TAP to Drop Egg!"; break;
                case BLUES: hint = "TAP to Split!"; break;
                default: break;
            }
            layout.setText(font, hint);
            font.draw(batch, hint, worldWidth / 2f - layout.width / 2f,
                Constants.WORLD_HEIGHT - 0.3f);
            font.setColor(Color.WHITE);
        }

        // Win/Lose overlay
        if (state == GameState.WON || state == GameState.LOST) {
            boolean won = state == GameState.WON;
            font.getData().setScale(viewport.getWorldHeight() / Gdx.graphics.getHeight() * 5f);
            font.setColor(won ? Color.YELLOW : Color.RED);
            String msg = won ? "LEVEL COMPLETE!" : "LEVEL FAILED!";
            layout.setText(font, msg);
            font.draw(batch, msg, worldWidth / 2f - layout.width / 2f,
                Constants.WORLD_HEIGHT / 2f + layout.height / 2f);
            font.getData().setScale(viewport.getWorldHeight() / Gdx.graphics.getHeight() * 2f);
            font.setColor(Color.WHITE);
            layout.setText(font, "Tap to continue");
            font.draw(batch, "Tap to continue", worldWidth / 2f - layout.width / 2f,
                Constants.WORLD_HEIGHT / 2f - 1f);
        }

        font.getData().setScale(viewport.getWorldHeight() / Gdx.graphics.getHeight() * 2f);
        batch.end();
    }

    private void update(float delta) {
        world.step(Constants.PHYSICS_TIME_STEP, Constants.VELOCITY_ITERATIONS, Constants.POSITION_ITERATIONS);

        // Shooter pig AI
        for (int i = 0, n = entities.size(); i < n; i++) {
            GameEntity entity = entities.get(i);
            if (entity instanceof ShooterPig && !entity.isMarkedForRemoval()) {
                Projectile proj = ((ShooterPig) entity).update(delta, world, launchedBirds, currentBird);
                if (proj != null) {
                    projectiles.add(proj);
                }
            }
        }

        // Update projectiles (reverse iterate for removal)
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);
            p.update(delta);
            if (p.isMarkedForRemoval()) {
                world.destroyBody(p.getBody());
                projectiles.remove(i);
            }
        }

        // Explosive birds
        for (int i = 0, n = launchedBirds.size(); i < n; i++) {
            Bird b = launchedBirds.get(i);
            if (b.isExplodeOnImpact() && b.hasCollided() && !b.isMarkedForRemoval()) {
                b.triggerExplosion(world);
                triggerShake(0.15f, 0.4f);
            }
        }

        // TNT detonation queue
        List<TNT> tntQueue = contactHandler.getTntToDetonate();
        for (int i = 0, n = tntQueue.size(); i < n; i++) {
            TNT tnt = tntQueue.get(i);
            if (!tnt.hasDetonated()) {
                tnt.detonate(world);
                triggerShake(0.25f, 0.5f);
            }
        }
        tntQueue.clear();

        // Remove destroyed entities (reverse iterate)
        for (int i = entities.size() - 1; i >= 0; i--) {
            GameEntity entity = entities.get(i);
            if (entity.isMarkedForRemoval()) {
                world.destroyBody(entity.getBody());
                entities.remove(i);
            }
        }

        // Remove destroyed launched birds (reverse iterate)
        for (int i = launchedBirds.size() - 1; i >= 0; i--) {
            Bird b = launchedBirds.get(i);
            if (b.isMarkedForRemoval()) {
                world.destroyBody(b.getBody());
                launchedBirds.remove(i);
            }
        }

        if (currentBird != null && currentBird.isMarkedForRemoval()) {
            currentBird = null;
        }

        // Win condition
        boolean pigsAlive = false;
        for (int i = 0, n = entities.size(); i < n; i++) {
            GameEntity e = entities.get(i);
            if ((e instanceof Pig || e instanceof ShooterPig) && !e.isMarkedForRemoval()) {
                pigsAlive = true;
                break;
            }
        }

        if (!pigsAlive && state != GameState.WON && state != GameState.LOST) {
            state = GameState.WON;
            stateTimer = 0;
            return;
        }

        // State machine
        switch (state) {
            case FLYING:
                stateTimer += delta;
                if (currentBird == null || currentBird.isMarkedForRemoval()) {
                    state = GameState.SETTLING;
                    settleTimer = 0;
                } else {
                    Vector2 pos = currentBird.getBody().getPosition();
                    boolean offScreen = pos.x < -2 || pos.x > worldWidth + 2 || pos.y < -2;
                    boolean stopped = stateTimer > 1f && currentBird.isStopped();
                    if (offScreen || stopped || stateTimer > 5f) {
                        state = GameState.SETTLING;
                        settleTimer = 0;
                    }
                }
                break;
            case SETTLING:
                settleTimer += delta;
                if (settleTimer > 2f) {
                    if (!availableBirds.isEmpty()) {
                        loadNextBird();
                        state = GameState.AIMING;
                    } else if (pigsAlive) {
                        state = GameState.LOST;
                        stateTimer = 0;
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void resize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        viewport.update(width, height, true);
        camera.position.set(worldWidth / 2f, Constants.WORLD_HEIGHT / 2f, 0);
        font.getData().setScale(viewport.getWorldHeight() / Gdx.graphics.getHeight() * 2f);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        world.dispose();
        debugRenderer.dispose();
        shapeRenderer.dispose();
        batch.dispose();
        font.dispose();
    }
}
