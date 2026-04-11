package me.teamsupre.me.dragonsvsmachines;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

/**
 * Procedural parallax background with multiple layers drawn via ShapeRenderer.
 * Each layer scrolls at a different speed relative to the camera position.
 * Zero per-frame allocations.
 */
public class ParallaxBackground {

    // Sky gradient colors
    private static final float SKY_TOP_R = 0.15f, SKY_TOP_G = 0.3f, SKY_TOP_B = 0.7f;
    private static final float SKY_BOT_R = 0.5f, SKY_BOT_G = 0.7f, SKY_BOT_B = 0.95f;

    // Layer parallax factors (0 = fixed, 1 = moves with camera)
    private static final float CLOUD_FACTOR = 0.05f;
    private static final float FAR_MOUNTAIN_FACTOR = 0.1f;
    private static final float NEAR_MOUNTAIN_FACTOR = 0.25f;
    private static final float FAR_HILL_FACTOR = 0.4f;
    private static final float NEAR_HILL_FACTOR = 0.6f;

    // Cloud auto-scroll
    private float cloudOffset;

    // Pre-computed mountain/hill peak positions (x offsets within one repeating tile)
    private final float[] farMountainPeaks = {0f, 2.5f, 5f, 7f, 9.5f, 12f, 14.5f, 17f, 19f, 21.5f, 24f, 26.5f, 29f, 31f, 33.5f, 36f, 38.5f};
    private final float[] farMountainHeights = {2.5f, 3.5f, 2.8f, 4.0f, 3.0f, 3.8f, 2.2f, 3.3f, 4.2f, 2.6f, 3.6f, 2.9f, 3.7f, 2.4f, 3.1f, 4.0f, 2.7f};

    private final float[] nearMountainPeaks = {1f, 4f, 7.5f, 10f, 13f, 16f, 19.5f, 22f, 25f, 28f, 31f, 34f, 37f};
    private final float[] nearMountainHeights = {1.8f, 2.5f, 2.0f, 3.0f, 2.3f, 2.8f, 1.5f, 2.6f, 3.2f, 2.1f, 2.7f, 1.9f, 2.4f};

    private final float[] farHillPeaks = {0.5f, 3f, 5.5f, 8f, 10.5f, 13f, 15.5f, 18f, 20.5f, 23f, 25.5f, 28f, 30.5f, 33f, 35.5f, 38f};
    private final float[] farHillHeights = {1.0f, 1.4f, 1.1f, 1.5f, 1.2f, 1.3f, 0.9f, 1.4f, 1.6f, 1.0f, 1.3f, 1.1f, 1.5f, 1.2f, 1.0f, 1.4f};

    private final float[] nearHillPeaks = {1.5f, 4.5f, 7f, 10f, 13.5f, 16.5f, 19f, 22f, 25.5f, 28.5f, 31f, 34f, 37f};
    private final float[] nearHillHeights = {0.6f, 0.9f, 0.7f, 1.0f, 0.8f, 0.7f, 0.5f, 0.9f, 1.1f, 0.6f, 0.8f, 0.7f, 0.9f};

    // Cloud positions (x, y, radius)
    private final float[] cloudX = {2f, 6f, 11f, 15f, 20f, 25f, 30f, 35f, 40f, 45f};
    private final float[] cloudY = {7.5f, 8.0f, 7.2f, 8.3f, 7.8f, 7.0f, 8.1f, 7.4f, 7.9f, 7.6f};
    private final float[] cloudR = {0.6f, 0.8f, 0.5f, 0.7f, 0.9f, 0.6f, 0.7f, 0.5f, 0.8f, 0.6f};

    public void update(float delta) {
        cloudOffset += delta * 0.3f; // slow auto-drift
    }

    /**
     * Render the parallax background. Call BEFORE drawing game entities.
     * Must be called between shapeRenderer.begin(Filled) and .end().
     *
     * @param renderer   the ShapeRenderer (must be in Filled mode)
     * @param cameraX    camera center X position
     * @param viewWidth  visible viewport width
     * @param viewHeight visible viewport height
     */
    public void render(ShapeRenderer renderer, float cameraX, float viewWidth, float viewHeight) {
        float halfW = viewWidth / 2f;
        float left = cameraX - halfW;
        float right = cameraX + halfW;
        float groundY = Constants.GROUND_HEIGHT;

        // === Sky gradient (no parallax — fills screen) ===
        int skySteps = 8;
        float stepH = viewHeight / skySteps;
        for (int i = 0; i < skySteps; i++) {
            float t = (float) i / skySteps;
            float r = SKY_BOT_R + (SKY_TOP_R - SKY_BOT_R) * t;
            float g = SKY_BOT_G + (SKY_TOP_G - SKY_BOT_G) * t;
            float b = SKY_BOT_B + (SKY_TOP_B - SKY_BOT_B) * t;
            renderer.setColor(r, g, b, 1f);
            renderer.rect(left, viewHeight - (i + 1) * stepH, viewWidth, stepH);
        }

        // === Clouds (very slow parallax + auto-drift) ===
        renderer.setColor(1f, 1f, 1f, 0.6f);
        for (int i = 0; i < cloudX.length; i++) {
            float cx = cloudX[i] + cloudOffset - cameraX * CLOUD_FACTOR;
            // Wrap clouds
            float range = viewWidth + 10f;
            cx = ((cx - left) % range + range) % range + left;

            float cy = cloudY[i];
            float cr = cloudR[i];
            // Fluffy cloud shape: 3 overlapping circles
            renderer.circle(cx - cr * 0.5f, cy, cr * 0.7f, 12);
            renderer.circle(cx, cy + cr * 0.2f, cr, 12);
            renderer.circle(cx + cr * 0.5f, cy, cr * 0.7f, 12);
        }

        // === Far mountains (slow parallax) ===
        renderer.setColor(0.35f, 0.35f, 0.55f, 1f);
        drawMountainLayer(renderer, farMountainPeaks, farMountainHeights,
            cameraX, FAR_MOUNTAIN_FACTOR, left, right, groundY, 1.8f);

        // === Near mountains (medium parallax) ===
        renderer.setColor(0.3f, 0.4f, 0.35f, 1f);
        drawMountainLayer(renderer, nearMountainPeaks, nearMountainHeights,
            cameraX, NEAR_MOUNTAIN_FACTOR, left, right, groundY, 2.0f);

        // === Far hills (faster parallax) ===
        renderer.setColor(0.25f, 0.5f, 0.25f, 1f);
        drawHillLayer(renderer, farHillPeaks, farHillHeights,
            cameraX, FAR_HILL_FACTOR, left, right, groundY, 1.5f);

        // === Near hills (fastest parallax) ===
        renderer.setColor(0.28f, 0.55f, 0.22f, 1f);
        drawHillLayer(renderer, nearHillPeaks, nearHillHeights,
            cameraX, NEAR_HILL_FACTOR, left, right, groundY, 1.2f);
    }

    private void drawMountainLayer(ShapeRenderer renderer, float[] peaks, float[] heights,
                                    float cameraX, float factor, float left, float right,
                                    float groundY, float baseWidth) {
        for (int i = 0; i < peaks.length; i++) {
            float px = peaks[i] - cameraX * factor;
            // Only draw if visible
            if (px + baseWidth < left - 5f || px - baseWidth > right + 5f) continue;

            float peakY = groundY + heights[i];
            // Triangle mountain
            renderer.triangle(
                px - baseWidth, groundY,
                px, peakY,
                px + baseWidth, groundY
            );
        }
    }

    private void drawHillLayer(ShapeRenderer renderer, float[] peaks, float[] heights,
                                float cameraX, float factor, float left, float right,
                                float groundY, float baseWidth) {
        for (int i = 0; i < peaks.length; i++) {
            float px = peaks[i] - cameraX * factor;
            if (px + baseWidth < left - 5f || px - baseWidth > right + 5f) continue;

            float hillH = heights[i];
            // Rounded hill: draw as a series of thin rects approximating a curve
            int segments = 6;
            float segW = baseWidth * 2f / segments;
            for (int s = 0; s < segments; s++) {
                float t = (float) s / segments;
                float t1 = (float) (s + 1) / segments;
                float h0 = hillH * MathUtils.sin(t * MathUtils.PI);
                float h1 = hillH * MathUtils.sin(t1 * MathUtils.PI);
                float x0 = px - baseWidth + s * segW;
                renderer.triangle(x0, groundY, x0 + segW, groundY, x0, groundY + h0);
                renderer.triangle(x0 + segW, groundY, x0 + segW, groundY + h1, x0, groundY + h0);
            }
        }
    }
}
