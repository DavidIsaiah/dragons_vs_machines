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
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.viewport.FitViewport;
import me.teamsupre.me.dragonsvsmachines.Constants;
import me.teamsupre.me.dragonsvsmachines.DragonsVsMachines;
import me.teamsupre.me.dragonsvsmachines.entities.Bird;
import me.teamsupre.me.dragonsvsmachines.entities.GameEntity;
import me.teamsupre.me.dragonsvsmachines.entities.Pig;
import me.teamsupre.me.dragonsvsmachines.levels.LevelData;
import me.teamsupre.me.dragonsvsmachines.physics.ContactHandler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameScreen extends InputAdapter implements Screen {
    private final DragonsVsMachines game;
    private final int level;

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
    private List<Bird> availableBirds;
    private Bird currentBird;
    private int currentBirdIndex;

    // Slingshot state
    private boolean dragging;
    private Vector2 dragPoint;
    private Vector2 slingshotAnchor;

    // Game state
    private enum GameState { AIMING, FLYING, SETTLING, WON, LOST }
    private GameState state;
    private float settleTimer;
    private float stateTimer;

    public GameScreen(DragonsVsMachines game, int level) {
        this.game = game;
        this.level = level;
    }

    @Override
    public void show() {
        // Camera setup
        camera = new OrthographicCamera();
        viewport = new FitViewport(Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT, camera);
        camera.position.set(Constants.WORLD_WIDTH / 2f, Constants.WORLD_HEIGHT / 2f, 0);
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
        createGround();
        LevelData.buildLevel(level, world, entities);

        // Create birds
        availableBirds = new ArrayList<Bird>();
        int birdCount = LevelData.getBirdCount(level);
        for (int i = 0; i < birdCount; i++) {
            availableBirds.add(new Bird(world, -10, -10)); // offscreen initially
        }

        // Load first bird
        currentBirdIndex = 0;
        loadNextBird();

        state = GameState.AIMING;
        dragging = false;
        settleTimer = 0;
        stateTimer = 0;
    }

    private void createGround() {
        BodyDef groundDef = new BodyDef();
        groundDef.type = BodyDef.BodyType.StaticBody;
        groundDef.position.set(Constants.WORLD_WIDTH / 2f, Constants.GROUND_HEIGHT / 2f);

        Body ground = world.createBody(groundDef);

        PolygonShape groundBox = new PolygonShape();
        groundBox.setAsBox(Constants.WORLD_WIDTH / 2f, Constants.GROUND_HEIGHT / 2f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = groundBox;
        fixtureDef.friction = 0.8f;
        fixtureDef.filter.categoryBits = Constants.CATEGORY_GROUND;

        ground.createFixture(fixtureDef);
        groundBox.dispose();

        // Walls to keep things in bounds
        BodyDef wallDef = new BodyDef();
        wallDef.type = BodyDef.BodyType.StaticBody;

        // Right wall
        wallDef.position.set(Constants.WORLD_WIDTH, Constants.WORLD_HEIGHT / 2f);
        Body rightWall = world.createBody(wallDef);
        PolygonShape wallShape = new PolygonShape();
        wallShape.setAsBox(0.1f, Constants.WORLD_HEIGHT / 2f);
        rightWall.createFixture(wallShape, 0f);
        wallShape.dispose();
    }

    private void loadNextBird() {
        if (currentBirdIndex < availableBirds.size()) {
            currentBird = availableBirds.get(currentBirdIndex);
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

    // --- Update & Render ---

    @Override
    public void render(float delta) {
        // Clamp delta
        delta = Math.min(delta, 0.05f);

        update(delta);

        // Clear
        Gdx.gl.glClearColor(0.4f, 0.65f, 0.9f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();

        // Draw filled shapes
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Ground
        shapeRenderer.setColor(0.3f, 0.6f, 0.2f, 1f);
        shapeRenderer.rect(0, 0, Constants.WORLD_WIDTH, Constants.GROUND_HEIGHT);

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

        // Draw current bird
        if (currentBird != null) {
            currentBird.render(shapeRenderer);
        }

        // Draw launched birds that are still active
        for (int i = 0; i < currentBirdIndex; i++) {
            Bird b = availableBirds.get(i);
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
                        if (simX >= Constants.WORLD_WIDTH) {
                            simX = Constants.WORLD_WIDTH;
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

        // Draw HUD text
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        font.setColor(Color.WHITE);
        int birdsLeft = availableBirds.size() - currentBirdIndex - (state == GameState.AIMING ? 0 : 1);
        if (birdsLeft < 0) birdsLeft = 0;
        font.draw(batch, "Birds: " + birdsLeft + "  |  Level " + level + "  |  ESC=Menu  R=Restart",
            0.3f, Constants.WORLD_HEIGHT - 0.3f);

        // Pigs remaining
        int pigsLeft = 0;
        for (GameEntity e : entities) {
            if (e instanceof Pig && !e.isMarkedForRemoval()) pigsLeft++;
        }
        font.draw(batch, "Pigs: " + pigsLeft, 0.3f, Constants.WORLD_HEIGHT - 0.8f);

        // Win/Lose overlay
        if (state == GameState.WON) {
            font.getData().setScale(viewport.getWorldHeight() / Gdx.graphics.getHeight() * 5f);
            font.setColor(Color.YELLOW);
            layout.setText(font, "LEVEL COMPLETE!");
            font.draw(batch, "LEVEL COMPLETE!",
                Constants.WORLD_WIDTH / 2f - layout.width / 2f,
                Constants.WORLD_HEIGHT / 2f + layout.height / 2f);
            font.getData().setScale(viewport.getWorldHeight() / Gdx.graphics.getHeight() * 2f);
            font.setColor(Color.WHITE);
            layout.setText(font, "Tap to continue");
            font.draw(batch, "Tap to continue",
                Constants.WORLD_WIDTH / 2f - layout.width / 2f,
                Constants.WORLD_HEIGHT / 2f - 1f);
        } else if (state == GameState.LOST) {
            font.getData().setScale(viewport.getWorldHeight() / Gdx.graphics.getHeight() * 5f);
            font.setColor(Color.RED);
            layout.setText(font, "LEVEL FAILED!");
            font.draw(batch, "LEVEL FAILED!",
                Constants.WORLD_WIDTH / 2f - layout.width / 2f,
                Constants.WORLD_HEIGHT / 2f + layout.height / 2f);
            font.getData().setScale(viewport.getWorldHeight() / Gdx.graphics.getHeight() * 2f);
            font.setColor(Color.WHITE);
            layout.setText(font, "Tap to continue");
            font.draw(batch, "Tap to continue",
                Constants.WORLD_WIDTH / 2f - layout.width / 2f,
                Constants.WORLD_HEIGHT / 2f - 1f);
        }

        // Reset font scale
        font.getData().setScale(viewport.getWorldHeight() / Gdx.graphics.getHeight() * 2f);

        batch.end();
    }

    private void update(float delta) {
        // Step physics
        world.step(Constants.PHYSICS_TIME_STEP, Constants.VELOCITY_ITERATIONS, Constants.POSITION_ITERATIONS);

        // Remove destroyed entities
        Iterator<GameEntity> iter = entities.iterator();
        while (iter.hasNext()) {
            GameEntity entity = iter.next();
            if (entity.isMarkedForRemoval()) {
                world.destroyBody(entity.getBody());
                iter.remove();
            }
        }

        // Check win condition
        boolean pigsAlive = false;
        for (GameEntity e : entities) {
            if (e instanceof Pig && !e.isMarkedForRemoval()) {
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
                if (currentBird != null) {
                    // Bird went off screen or stopped
                    Vector2 pos = currentBird.getBody().getPosition();
                    boolean offScreen = pos.x < -2 || pos.x > Constants.WORLD_WIDTH + 2 || pos.y < -2;
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
                    currentBirdIndex++;
                    if (currentBirdIndex < availableBirds.size()) {
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
        camera.position.set(Constants.WORLD_WIDTH / 2f, Constants.WORLD_HEIGHT / 2f, 0);
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
