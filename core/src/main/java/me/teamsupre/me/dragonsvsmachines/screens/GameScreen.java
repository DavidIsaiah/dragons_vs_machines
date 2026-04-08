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
import java.util.Iterator;
import java.util.List;

public class GameScreen extends InputAdapter implements Screen {
    private final DragonsVsMachines game;
    private final int level;
    private float worldWidth;

    // Projectiles from shooter pigs
    private List<Projectile> projectiles;

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
    private List<GameEntity> entities;
    private List<Bird> availableBirds;    // birds waiting to be launched
    private List<Bird> launchedBirds;     // birds already in play
    private Bird currentBird;

    // Slingshot state
    private boolean dragging;
    private Vector2 dragPoint;
    private Vector2 slingshotAnchor;

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
        // Per-level world width
        worldWidth = LevelData.getWorldWidth(level);

        // Camera setup — wider viewport for wider levels
        camera = new OrthographicCamera();
        float viewHeight = Constants.WORLD_HEIGHT;
        float viewWidth = Math.max(Constants.WORLD_WIDTH, worldWidth);
        viewport = new FitViewport(viewWidth, viewHeight, camera);
        camera.position.set(worldWidth / 2f, viewHeight / 2f, 0);
        camera.update();

        // Physics world
        world = new World(new Vector2(0, -9.8f), true);
        contactHandler = new ContactHandler();
        world.setContactListener(contactHandler);
        debugRenderer = new Box2DDebugRenderer();

        // Rendering
        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.setUseIntegerPositions(false);
        font.getData().setScale(viewport.getWorldHeight() / Gdx.graphics.getHeight() * 2f);
        layout = new GlyphLayout();

        // Input
        Gdx.input.setInputProcessor(this);
        dragPoint = new Vector2();
        slingshotAnchor = new Vector2(Constants.SLINGSHOT_X, Constants.SLINGSHOT_Y);

        // Build the level
        entities = new ArrayList<GameEntity>();
        projectiles = new ArrayList<Projectile>();
        createGround();
        LevelData.buildLevel(level, world, entities);

        // Create birds with types from level data
        availableBirds = new ArrayList<Bird>();
        launchedBirds = new ArrayList<Bird>();
        BirdType[] birdTypes = LevelData.getBirdTypes(level);
        for (BirdType birdType : birdTypes) {
            availableBirds.add(new Bird(world, -10, -10, birdType));
        }

        // Load first bird
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

        // Right wall
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

    // --- Input handling ---

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (state == GameState.WON || state == GameState.LOST) {
            game.setScreen(new LevelSelectScreen(game));
            return true;
        }

        // Activate ability if bird is in flight or settling (Bomb can detonate anytime after launch)
        if ((state == GameState.FLYING || state == GameState.SETTLING) && currentBird != null && currentBird.canActivateAbility()) {
            BirdType activatingType = currentBird.getType();
            List<GameEntity> newEntities = currentBird.activateAbility(world);
            if (newEntities != null) {
                for (GameEntity e : newEntities) {
                    if (e instanceof Bird) {
                        launchedBirds.add((Bird) e);
                    } else {
                        entities.add(e);
                    }
                }
            }
            // Camera shake for explosive abilities
            if (activatingType == BirdType.BOMB) {
                triggerShake(0.2f, 0.5f);
            } else if (activatingType == BirdType.BLUES) {
                triggerShake(0.08f, 0.2f);
            }
            return true;
        }

        if (state != GameState.AIMING || currentBird == null) return false;

        Vector2 touchWorld = viewport.unproject(new Vector2(screenX, screenY));

        float distToBird = touchWorld.dst(currentBird.getBody().getPosition());
        if (distToBird < 1.0f) {
            dragging = true;
            dragPoint.set(touchWorld);
            return true;
        }
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (!dragging) return false;

        Vector2 worldPos = viewport.unproject(new Vector2(screenX, screenY));
        dragPoint.set(worldPos);

        // Clamp pull distance
        Vector2 pull = new Vector2(dragPoint).sub(slingshotAnchor);
        if (pull.len() > Constants.MAX_PULL_DISTANCE) {
            pull.nor().scl(Constants.MAX_PULL_DISTANCE);
            dragPoint.set(slingshotAnchor).add(pull);
        }

        // Move bird to drag position
        currentBird.getBody().setTransform(dragPoint, 0);
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (!dragging) return false;
        dragging = false;

        // Calculate launch impulse (opposite of pull direction)
        Vector2 pull = new Vector2(slingshotAnchor).sub(dragPoint);
        if (pull.len() < 0.2f) {
            // Too small, snap back
            currentBird.getBody().setTransform(slingshotAnchor, 0);
            return true;
        }

        Vector2 impulse = pull.scl(Constants.LAUNCH_POWER);
        contactHandler.enableDamage();
        currentBird.launch(impulse);
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
        // Stack shakes — take the stronger one
        if (intensity > shakeIntensity) {
            shakeIntensity = intensity;
        }
        shakeDuration = Math.max(shakeDuration, duration);
        shakeTimer = 0;
    }

    // --- Update & Render ---

    @Override
    public void render(float delta) {
        // Clamp delta
        delta = Math.min(delta, 0.05f);

        update(delta);

        // Clear
        Gdx.gl.glClearColor(0.4f, 0.65f, 0.9f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Apply camera shake
        camera.position.set(worldWidth / 2f, Constants.WORLD_HEIGHT / 2f, 0);
        if (shakeTimer < shakeDuration) {
            shakeTimer += delta;
            float progress = shakeTimer / shakeDuration;
            float damping = 1f - progress; // fade out
            float offsetX = MathUtils.random(-shakeIntensity, shakeIntensity) * damping;
            float offsetY = MathUtils.random(-shakeIntensity, shakeIntensity) * damping;
            camera.position.x += offsetX;
            camera.position.y += offsetY;
        } else {
            shakeIntensity = 0;
            shakeDuration = 0;
        }
        camera.update();

        // Draw filled shapes
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Ground
        shapeRenderer.setColor(0.3f, 0.6f, 0.2f, 1f);
        shapeRenderer.rect(0, 0, worldWidth, Constants.GROUND_HEIGHT);

        // Slingshot base
        shapeRenderer.setColor(0.4f, 0.25f, 0.1f, 1f);
        shapeRenderer.rect(Constants.SLINGSHOT_X - 0.08f, Constants.GROUND_HEIGHT,
            0.16f, Constants.SLINGSHOT_Y - Constants.GROUND_HEIGHT + 0.3f);

        // Slingshot fork
        shapeRenderer.rect(Constants.SLINGSHOT_X - 0.25f, Constants.SLINGSHOT_Y + 0.1f, 0.12f, 0.4f);
        shapeRenderer.rect(Constants.SLINGSHOT_X + 0.13f, Constants.SLINGSHOT_Y + 0.1f, 0.12f, 0.4f);

        // Draw entities
        for (GameEntity entity : entities) {
            entity.render(shapeRenderer);
        }

        // Debug: draw TNT blast radius (subtle fill)
        for (GameEntity entity : entities) {
            if (entity instanceof TNT) {
                ((TNT) entity).renderBlastRadius(shapeRenderer);
            }
        }

        // Debug: draw shooter pig attack range (subtle fill)
        for (GameEntity entity : entities) {
            if (entity instanceof ShooterPig) {
                ((ShooterPig) entity).renderAttackRange(shapeRenderer);
            }
        }

        // Draw projectiles
        for (Projectile p : projectiles) {
            p.render(shapeRenderer);
        }

        // Draw current bird on slingshot
        if (currentBird != null && !currentBird.isMarkedForRemoval()) {
            currentBird.render(shapeRenderer);
        }

        // Draw all launched birds still in play
        for (Bird b : launchedBirds) {
            if (!b.isMarkedForRemoval()) {
                b.render(shapeRenderer);
            }
        }

        shapeRenderer.end();

        // Draw elastic band while dragging
        if (dragging && currentBird != null) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(0.3f, 0.15f, 0.05f, 1f);
            Gdx.gl.glLineWidth(3f);

            Vector2 birdPos = currentBird.getBody().getPosition();
            // Left band
            shapeRenderer.line(Constants.SLINGSHOT_X - 0.19f, Constants.SLINGSHOT_Y + 0.45f,
                birdPos.x, birdPos.y);
            // Right band
            shapeRenderer.line(Constants.SLINGSHOT_X + 0.19f, Constants.SLINGSHOT_Y + 0.45f,
                birdPos.x, birdPos.y);
            shapeRenderer.end();
            Gdx.gl.glLineWidth(1f);

            // Draw trajectory prediction dots with ground bounce simulation
            Vector2 pull = new Vector2(slingshotAnchor).sub(dragPoint);
            if (pull.len() > 0.2f) {
                Vector2 launchImpulse = new Vector2(pull).scl(Constants.LAUNCH_POWER);
                float mass = currentBird.getBody().getMass();
                float simVx = launchImpulse.x / mass;
                float simVy = launchImpulse.y / mass;
                float gravity = -9.8f;
                float groundY = Constants.GROUND_HEIGHT + Constants.BIRD_RADIUS;
                float restitution = 0.2f;
                float friction = 0.5f;

                // Step-based simulation to handle bounces
                float simX = dragPoint.x;
                float simY = dragPoint.y;
                float dt = 0.02f;

                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                int numDots = 16;
                int stepsPerDot = 4;
                for (int i = 0; i < numDots; i++) {
                    // Advance simulation several sub-steps per dot
                    for (int s = 0; s < stepsPerDot; s++) {
                        simVy += gravity * dt;
                        simX += simVx * dt;
                        simY += simVy * dt;

                        // Ground bounce
                        if (simY <= groundY) {
                            simY = groundY;
                            simVy = -simVy * restitution;
                            simVx *= (1f - friction * dt * 5f);

                            // Stop if barely moving
                            if (Math.abs(simVy) < 0.3f) {
                                simVy = 0;
                            }
                        }

                        // Right wall bounce
                        if (simX >= worldWidth) {
                            simX = worldWidth;
                            simVx = -simVx * restitution;
                        }
                    }

                    // Stop drawing if essentially at rest on ground
                    if (simY <= groundY + 0.01f && Math.abs(simVy) < 0.1f && Math.abs(simVx) < 0.3f) break;
                    if (simX < -1f) break;

                    float alpha = 1f - (float) i / numDots;
                    shapeRenderer.setColor(1f, 1f, 1f, alpha * 0.7f);
                    float dotSize = 0.05f + 0.03f * (1f - alpha);
                    shapeRenderer.circle(simX, simY, dotSize, 8);
                }
                shapeRenderer.end();
            }
        }

        // Box2D debug renderer (wireframes)
        debugRenderer.render(world, camera.combined);

        // Debug: draw explosion radius ring and shooter pig range outlines
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(2f);
        TNT.renderDebugExplosions(shapeRenderer, delta);
        for (GameEntity entity : entities) {
            if (entity instanceof ShooterPig) {
                ((ShooterPig) entity).renderAttackRangeOutline(shapeRenderer);
            }
        }
        shapeRenderer.end();
        Gdx.gl.glLineWidth(1f);

        // Draw HUD text
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        font.setColor(Color.WHITE);
        int birdsLeft = availableBirds.size();

        String birdName = currentBird != null ? currentBird.getType().name : "";
        font.draw(batch, "Birds: " + birdsLeft + "  |  " + birdName + "  |  Level " + level + "  |  ESC=Menu  R=Restart",
            0.3f, Constants.WORLD_HEIGHT - 0.3f);

        // Pigs remaining
        int pigsLeft = 0;
        for (GameEntity e : entities) {
            if ((e instanceof Pig || e instanceof ShooterPig) && !e.isMarkedForRemoval()) pigsLeft++;
        }
        font.draw(batch, "Pigs: " + pigsLeft, 0.3f, Constants.WORLD_HEIGHT - 0.8f);

        // Ability hint during flight
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
            font.draw(batch, hint, Constants.WORLD_WIDTH / 2f - layout.width / 2f,
                Constants.WORLD_HEIGHT - 0.3f);
            font.setColor(Color.WHITE);
        }

        // Win/Lose overlay
        if (state == GameState.WON) {
            font.getData().setScale(viewport.getWorldHeight() / Gdx.graphics.getHeight() * 5f);
            font.setColor(Color.YELLOW);
            layout.setText(font, "LEVEL COMPLETE!");
            font.draw(batch, "LEVEL COMPLETE!",
                worldWidth / 2f - layout.width / 2f,
                Constants.WORLD_HEIGHT / 2f + layout.height / 2f);
            font.getData().setScale(viewport.getWorldHeight() / Gdx.graphics.getHeight() * 2f);
            font.setColor(Color.WHITE);
            layout.setText(font, "Tap to continue");
            font.draw(batch, "Tap to continue",
                worldWidth / 2f - layout.width / 2f,
                Constants.WORLD_HEIGHT / 2f - 1f);
        } else if (state == GameState.LOST) {
            font.getData().setScale(viewport.getWorldHeight() / Gdx.graphics.getHeight() * 5f);
            font.setColor(Color.RED);
            layout.setText(font, "LEVEL FAILED!");
            font.draw(batch, "LEVEL FAILED!",
                worldWidth / 2f - layout.width / 2f,
                Constants.WORLD_HEIGHT / 2f + layout.height / 2f);
            font.getData().setScale(viewport.getWorldHeight() / Gdx.graphics.getHeight() * 2f);
            font.setColor(Color.WHITE);
            layout.setText(font, "Tap to continue");
            font.draw(batch, "Tap to continue",
                worldWidth / 2f - layout.width / 2f,
                Constants.WORLD_HEIGHT / 2f - 1f);
        }

        // Reset font scale
        font.getData().setScale(viewport.getWorldHeight() / Gdx.graphics.getHeight() * 2f);

        batch.end();
    }

    private void update(float delta) {
        // Step physics
        world.step(Constants.PHYSICS_TIME_STEP, Constants.VELOCITY_ITERATIONS, Constants.POSITION_ITERATIONS);

        // Shooter pig AI — fire at birds in range
        for (GameEntity entity : entities) {
            if (entity instanceof ShooterPig && !entity.isMarkedForRemoval()) {
                ShooterPig sp = (ShooterPig) entity;
                Projectile proj = sp.update(delta, world, launchedBirds, currentBird);
                if (proj != null) {
                    projectiles.add(proj);
                }
            }
        }

        // Update projectiles
        Iterator<Projectile> projIter = projectiles.iterator();
        while (projIter.hasNext()) {
            Projectile p = projIter.next();
            p.update(delta);
            if (p.isMarkedForRemoval()) {
                world.destroyBody(p.getBody());
                projIter.remove();
            }
        }

        // Check for explosive birds that have collided (egg bombs)
        for (Bird b : launchedBirds) {
            if (b.isExplodeOnImpact() && b.hasCollided() && !b.isMarkedForRemoval()) {
                b.triggerExplosion(world);
                triggerShake(0.15f, 0.4f);
            }
        }

        // Detonate TNTs queued by contact handler
        List<TNT> tntQueue = contactHandler.getTntToDetonate();
        for (TNT tnt : tntQueue) {
            if (!tnt.hasDetonated()) {
                tnt.detonate(world);
                triggerShake(0.25f, 0.5f);
            }
        }
        tntQueue.clear();

        // Remove destroyed entities
        Iterator<GameEntity> iter = entities.iterator();
        while (iter.hasNext()) {
            GameEntity entity = iter.next();
            if (entity.isMarkedForRemoval()) {
                world.destroyBody(entity.getBody());
                iter.remove();
            }
        }

        // Remove destroyed launched birds
        Iterator<Bird> birdIter = launchedBirds.iterator();
        while (birdIter.hasNext()) {
            Bird b = birdIter.next();
            if (b.isMarkedForRemoval()) {
                world.destroyBody(b.getBody());
                birdIter.remove();
            }
        }

        // Handle currentBird being destroyed by its own ability
        if (currentBird != null && currentBird.isMarkedForRemoval()) {
            currentBird = null;
        }

        // Check win condition
        boolean pigsAlive = false;
        for (GameEntity e : entities) {
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
                    // Bird destroyed itself (Bomb/Blues ability) — move to settling
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
                    // Move to next bird or game over
                    if (!availableBirds.isEmpty()) {
                        loadNextBird();
                        state = GameState.AIMING;
                    } else {
                        if (pigsAlive) {
                            state = GameState.LOST;
                            stateTimer = 0;
                        }
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
