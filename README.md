# Dragons vs Machines

A physics-based slingshot game built with [libGDX](https://libgdx.com/) and [Box2D](https://box2d.org/). Launch birds from a slingshot to destroy enemy pig fortresses across increasingly challenging levels.

## How to Play

1. **Aim**: Click/tap near the bird on the slingshot and drag back to pull
2. **Launch**: Release to fire the bird along the predicted trajectory
3. **Ability**: Tap the screen while a special bird is mid-flight to activate its power
4. **Win**: Destroy all pigs before running out of birds
5. **Controls**: `ESC` = back to menu, `R` = restart level

## Features

### 5 Bird Types

| Bird | Color | Ability |
|---|---|---|
| **Red** | Red | None (standard projectile) |
| **Chuck** | Yellow | Tap for 2.5x speed boost |
| **Bomb** | Black | Tap to explode (works mid-air or after landing) |
| **Matilda** | White | Tap to drop an explosive egg downward |
| **Blues** | Blue | Tap to split into 3 birds in a fan pattern |

Each bird type has unique density, radius, and visual indicators. Abilities can be activated during both flight and settling states.

### 3 Block Materials

| Material | Color | Durability | Density |
|---|---|---|---|
| **Wood** | Brown | 60 HP | Light |
| **Stone** | Grey | 150 HP | Heavy |
| **Ice** | Light blue | 30 HP | Very light |

Blocks darken as they take damage and are destroyed when HP reaches zero.

### Enemy Types

- **Pigs** (green squares): Basic enemies with 15 HP. Killed by bird impacts, falling debris, explosions, or ground collisions.
- **Shooter Pigs** (purple squares with crosshair): Tougher enemies (20 HP) that fire knockback projectiles at birds within their attack range. Feature predictive aiming with iterative trajectory calculation and gravity compensation.

### Interactive Objects

- **TNT** (red squares with dark stripe): Explosive blocks that detonate on significant impact. Blast radius of 1.8m with radial impulse damage to nearby pigs, blocks, and entities. Chain reactions trigger nearby TNTs within 1.5m.

### Slingshot Mechanics

- Drag-to-launch with clamped pull distance
- Predictive trajectory line with ground bounce simulation
- Visual elastic band connecting slingshot forks to bird while dragging

### Visual Feedback

- **Camera shake** on all explosions (Bomb, TNT, egg impact, Blues split) with intensity-based stacking and smooth fade-out
- **Box2D debug rendering** overlays physics body wireframes
- **Debug circles** showing TNT blast radius and shooter pig attack range
- **ShapeRenderer** colored shapes for all entities with damage-based darkening
- **HUD** displaying bird count, current bird type, pig count, ability hints, and win/lose overlays

### 5 Levels

| Level | Name | World Size | Description |
|---|---|---|---|
| 1 | Simple Tower | 16m | Single wood tower, 1 pig, 3 birds |
| 2 | Double Towers | 16m | Wood + stone towers with TNT between them, 2 pigs, 4 birds |
| 3 | Fortress | 16m | Multi-story stone/wood/ice fortress, 3 pigs, TNT, 5 birds |
| 4 | Compound | 16m | 3 separate structures (stone tower, bunker, wood/ice tower), 7 pigs, 3 TNTs, 7 birds |
| 5 | Warzone | 40m | 6 sections with 4 shooter pigs, 12 regular pigs, 5 TNTs, 12 birds |

### Menu System

- **Main Menu**: Play and Exit buttons using Scene2D UI with programmatic skin (no external asset files)
- **Level Select**: Color-coded level buttons with difficulty names

## Performance

### Zero-Allocation Render Loop

All per-frame memory allocations have been eliminated to prevent garbage collection hitches:

- **Reusable Vector2 instances**: All input handling, trajectory calculation, and physics callbacks use pre-allocated `tmpVec` fields instead of `new Vector2()`. Explosion callbacks in Bird, TNT, and ShooterPig reuse instance-level `tmpExpDir`/`tmpAim`/`tmpDir` vectors.
- **Index-based iteration**: Every `for` loop in `render()` and `update()` uses `for (int i = 0, n = list.size(); i < n; i++)` instead of enhanced for-each loops, which would allocate an `Iterator` object per loop per frame.
- **Reverse-index removal**: Entity and projectile cleanup uses reverse-index iteration (`for (int i = size - 1; i >= 0; i--)`) instead of `Iterator.remove()`, avoiding both Iterator allocation and concurrent modification issues.
- **Pre-allocated collections**: All `ArrayList` fields (`entities`, `launchedBirds`, `availableBirds`, `projectiles`) are declared as instance fields and cleared on level load rather than re-allocated.
- **Primitive array iteration**: Physics contact impulse arrays use direct index access instead of enhanced for-each on `float[]`.

### Physics

- **Fixed timestep**: Box2D steps at `1/60s` with 6 velocity and 2 position iterations
- **Bullet bodies**: Birds use `bullet = true` for continuous collision detection on fast-moving projectiles
- **Deferred destruction**: Bodies queued for destruction are removed after `world.step()`, never during physics callbacks
- **Category-based filtering**: Collision categories (`GROUND`, `BIRD`, `PIG`, `BLOCK`) enable selective collision handling

### Architecture

- **Screen management**: `Game.setScreen()` pattern with `MainMenuScreen`, `LevelSelectScreen`, and `GameScreen`
- **Entity base class**: `GameEntity` provides shared `Body`, `Color`, removal flag, and abstract `render()`
- **Contact delegation**: `ContactHandler` implements `ContactListener` with deferred TNT detonation queue and projectile-bird knockback handling
- **Per-level configuration**: `LevelData` provides world width, bird types, and structure layouts per level

## Building & Running

```bash
# Run on desktop
./gradlew :lwjgl3:run

# Build runnable JAR
./gradlew :lwjgl3:jar

# Compile only
./gradlew :core:compileJava
```

## Project Structure

```
core/src/main/java/me/teamsupre/me/dragonsvsmachines/
  Constants.java              # World dimensions, physics params, entity sizes
  DragonsVsMachines.java      # Main Game class, shared resources
  entities/
    GameEntity.java           # Abstract base (Body, Color, removal)
    Bird.java                 # 5 bird types with abilities
    Pig.java                  # Basic enemy (square body)
    ShooterPig.java           # AI enemy with predictive aiming
    Block.java                # 3 material types (Wood/Stone/Ice)
    TNT.java                  # Explosive with chain reaction
    Projectile.java           # Shooter pig projectile
  screens/
    MainMenuScreen.java       # Scene2D main menu
    LevelSelectScreen.java    # Level selection grid
    GameScreen.java           # Gameplay (physics, rendering, input, state)
  physics/
    ContactHandler.java       # Collision damage, TNT triggers, knockback
  levels/
    LevelData.java            # Level configs (structures, birds, world size)
```

## Platforms

- `core`: Shared game logic
- `lwjgl3`: Primary desktop (LWJGL3)
- `android`: Android (needs Android SDK)
- `ios`: iOS (RoboVM)
- `html`: Web (GWT/WebGL)
- `lwjgl2`: Legacy desktop (LWJGL2)
- `ios-moe`: iOS (Multi-OS Engine)

## Tech Stack

- **Framework**: libGDX 1.14.0
- **Physics**: Box2D (bundled with libGDX)
- **Rendering**: ShapeRenderer (colored shapes) + Box2DDebugRenderer (wireframes)
- **UI**: Scene2D with programmatic skins
- **Build**: Gradle
