package com.johnbronson.freefall.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.FloatArray;
import com.johnbronson.freefall.physics.PhysicsWorld;
import com.johnbronson.freefall.world.TrajectoryPredictor;

/**
 * Draws the predicted coast line as a trail of dots, and animates it in and out.
 *
 * <h2>Dots spaced in time</h2>
 * One dot every {@link #SECONDS_PER_DOT} of predicted flight. Because the spacing is in
 * time, not in distance, the dots spread apart where the ship is fast and bunch
 * together where it's slow, so the line doubles as a speed readout. (Distance between
 * two dots = speed × time between them.)
 *
 * <h2>The sweep animation</h2>
 * Only part of the line is drawn: a <em>window</em> from {@code windowStart} to
 * {@code windowEnd}. Both are measured in seconds of <em>prediction</em> time, from 0
 * (the ship now) to {@link TrajectoryPredictor#PREDICTION_SECONDS} (the far end). A dot
 * is drawn only if its time falls inside the window.
 *
 * <p>Each key press plays one cycle of two sweeps, each {@link #SWEEP_SECONDS} long
 * (H below is the prediction horizon):
 * <pre>
 *   grow:     start stays at 0, end slides 0 → H    (dots appear, ship outward)
 *   retract:  end stays at H, start slides 0 → H    (dots vanish, ship outward)
 *   ...then the line switches itself off.
 * </pre>
 * Both sweeps run from the ship outward, so the line seems to flow away from the nose,
 * which hints at the direction of travel.
 *
 * <p>Pressing the key again mid-cycle:
 * <ul>
 *   <li>while growing: cancels. The grow turns into a retract straight away.</li>
 *   <li>while retracting: queues another cycle, which starts once this one clears.</li>
 * </ul>
 * Either way the line is never cut short: the window always empties smoothly.
 *
 * <p>This is display state, so it lives here in {@code render/}, not in the world.
 * The simulation doesn't care whether the line is visible. (Think of it as component
 * state in Angular, versus the data in a service.)
 *
 * <p>The class has no access modifier, so it's <em>package-private</em>: only classes in
 * {@code render/} can use it. {@link WorldRenderer} owns the one instance.
 */
final class TrajectoryRenderer {

    // ---- Tuning constants --------------------------------------------------------

    /**
     * Real seconds for one sweep: dots drawn from the ship all the way to the end, or
     * removed all the way to the end. A full grow-then-retract cycle takes twice this.
     * Tune by eye.
     */
    private static final float SWEEP_SECONDS = 1f;

    /**
     * How fast the window edges slide, in prediction-seconds per real second.
     *
     * <p>Speed = distance ÷ time. The "distance" an edge travels in one sweep is the
     * whole line ({@code PREDICTION_SECONDS}), and the time is {@code SWEEP_SECONDS}.
     * So if the line is twice as long, the edges move twice as fast, and a sweep still
     * takes exactly {@code SWEEP_SECONDS}.
     */
    private static final float WINDOW_SPEED = TrajectoryPredictor.PREDICTION_SECONDS / SWEEP_SECONDS;

    /** Gap between dots, in seconds of prediction time. */
    private static final float SECONDS_PER_DOT = 0.1f;

    /**
     * The same gap counted in physics steps, because the predictor gives one point per
     * step: seconds ÷ seconds-per-step = steps.
     *
     * <p>{@code Math.max(1, ...)} is a guard. If someone set {@code SECONDS_PER_DOT}
     * smaller than one step, the rounding would give 0, and the drawing loop below
     * ({@code i += STEPS_PER_DOT}) would never move forward: an infinite loop.
     */
    private static final int STEPS_PER_DOT = Math.max(1, Math.round(SECONDS_PER_DOT / PhysicsWorld.TIME_STEP));

    /**
     * Dot radius in meters at camera zoom 1.
     *
     * <p>The camera works in meters (see {@link WorldRenderer}), so anything we draw is
     * sized in meters too. When the camera zooms out, each meter covers fewer pixels and
     * a fixed-size dot would shrink to nothing. Multiplying by the zoom when drawing
     * cancels that out: zoom out 3× and the dot is 3× bigger in meters, so it looks the
     * same size on screen.
     */
    private static final float DOT_RADIUS = 0.12f;

    /**
     * Corners per dot. ShapeRenderer has no true circles; it draws a polygon with this
     * many sides. A dot only a few pixels wide looks round with very few.
     */
    private static final int DOT_SEGMENTS = 8;

    /**
     * Skip dots closer than this to the ship's center, so the line starts just past
     * the nose instead of being drawn over the hull. A little more than the nose's
     * distance from the center, so it follows the ship art if that changes.
     */
    private static final float SHIP_CLEARANCE = ShipRenderer.NOSE_Y + 0.2f;

    /**
     * How see-through the far end of the line gets: 1 = fully solid, 0 = invisible.
     * The fade hints that the prediction gets less certain further out.
     */
    private static final float FAR_END_ALPHA = 0.25f;

    /**
     * Pale blue. libGDX colors are four floats from 0 to 1: red, green, blue and alpha
     * (opacity), rather than CSS's 0–255.
     */
    private static final Color DOT_COLOR = new Color(0.6f, 0.9f, 1f, 1f);

    // ---- Animation state ---------------------------------------------------------
    // Plain fields, not static: they belong to this one renderer object. Java sets
    // number fields to 0 and booleans to false by default, so `wanted` starts false
    // and the window starts at [0, 0] (empty) without us writing it.

    /**
     * True while a cycle has been asked for and its grow sweep hasn't finished yet.
     * The key flips it on; the animation flips it off when the grow reaches the end.
     */
    private boolean wanted;

    /** Near edge of the visible window, in prediction-seconds. See the class comment. */
    private float windowStart;

    /** Far edge of the visible window, in prediction-seconds. */
    private float windowEnd;

    /**
     * Which sweep is running: true = growing (moving the far edge), false = retracting
     * (moving the near edge). Starts true, so the first sweep after a key press grows.
     */
    private boolean growing = true;

    /** Called when the player presses the predictor key. */
    void toggle() {
        // `!` flips a boolean, as in TypeScript.
        wanted = !wanted;
    }

    /**
     * True while there's anything to draw, or about to be. While this is false the
     * world doesn't need to run the prediction at all, which saves its cost every frame.
     *
     * <p>{@code windowEnd > windowStart} means "the window has some width", i.e. at
     * least part of the line is on screen.
     */
    boolean isActive() {
        return wanted || windowEnd > windowStart;
    }

    /**
     * Advances the sweep animation. Call once per frame, before {@link #draw}.
     *
     * <p>It works like a tiny state machine with three states, picked by the
     * {@code if / else if / else} below: growing, retracting, and empty.
     */
    void update(float frameDelta) {
        // How far an edge may move this frame: speed × time. Multiplying by the frame
        // time keeps the animation the same length whether the game runs at 30 or 144
        // frames per second. (A fast machine has more frames, each with a smaller
        // frameDelta, so the edge moves less per frame and the totals come out equal.)
        float maxMove = WINDOW_SPEED * frameDelta;
        float fullLength = TrajectoryPredictor.PREDICTION_SECONDS;

        if (wanted && growing) {
            // ---- Growing: only the far edge moves out; the near edge stays at 0 ----
            windowEnd = moveToward(windowEnd, fullLength, maxMove);

            // moveToward never overshoots, so windowEnd lands exactly on fullLength and
            // this comparison is safe even with floats.
            if (windowEnd >= fullLength) {
                // Reached the end. Next frame starts removing dots from the ship side.
                growing = false;
                // One cycle per key press: switch ourselves off now, so the retract
                // that follows is the last sweep. (The retract branch below runs
                // whether or not the line is wanted, so it still finishes.)
                wanted = false;
            }
        } else if (windowEnd > windowStart) {
            // ---- Retracting: the near edge chases the far edge out to the end ----
            // This branch also runs if the player cancelled mid-grow (wanted became
            // false while growing was still true).
            //
            // The far edge keeps moving out too. Normally it's already at the end, so
            // this changes nothing. But after a mid-grow cancel it lets the grow finish
            // while the near edge chases it, instead of the far end jumping. Both edges
            // move at the same speed, so the near one only catches up at the end.
            windowEnd = moveToward(windowEnd, fullLength, maxMove);
            windowStart = moveToward(windowStart, fullLength, maxMove);
        } else {
            // ---- Empty: every dot has been removed ----
            // Reset both edges to the ship so the next grow starts from the nose. If a
            // cycle is queued (wanted is true), it starts growing next frame;
            // otherwise nothing more happens until the next key press.
            windowStart = 0f;
            windowEnd = 0f;
            growing = true;
        }
    }

    /**
     * Moves {@code value} toward {@code target} by at most {@code maxMove}, without
     * overshooting. For example, moveToward(4.9, 5, 0.2) gives 5, not 5.1.
     *
     * <p>{@code static}: it uses no fields, only its arguments, so it doesn't need an
     * object. {@code private}: it's a helper for this class alone.
     */
    private static float moveToward(float value, float target, float maxMove) {
        // Going up: add, but never past the target. Math.min picks the smaller number.
        if (value < target) return Math.min(value + maxMove, target);
        // Going down (or already there): subtract, but never below the target.
        return Math.max(value - maxMove, target);
    }

    /**
     * Draws the dots that fall inside the window.
     *
     * <p>Must be called between {@code shapes.begin()} and {@code shapes.end()}.
     * ShapeRenderer collects shapes between those calls and sends them to the graphics
     * card in one batch at {@code end()}, which is far faster than one trip per dot.
     *
     * @param shapes     the shared ShapeRenderer, already set up with the camera.
     * @param prediction this frame's predicted points.
     * @param offsetX    shift, in meters, from the raw physics position to where the
     *                   ship is drawn this frame (see {@code InterpolatedBody}), so the
     *                   line meets the drawn ship instead of running up to a step ahead.
     * @param offsetY    the same shift, vertically.
     * @param zoom       the camera zoom, to keep dots the same size on screen.
     */
    void draw(ShapeRenderer shapes, TrajectoryPredictor prediction, float offsetX, float offsetY, float zoom) {
        FloatArray points = prediction.getPoints();
        int count = prediction.getPointCount();
        // Nothing predicted (ship landed or crashed), or nothing in the window yet.
        if (count == 0 || windowEnd <= windowStart) return;

        // Point 0 is the ship itself. We measure each dot's distance from it to skip
        // dots that would sit on the hull. Points are packed [x0, y0, x1, y1, ...], so
        // point 0's x and y are floats 0 and 1.
        float shipX = points.get(0) + offsetX;
        float shipY = points.get(1) + offsetY;

        // The distance from the ship to a dot is sqrt(dx² + dy²) (Pythagoras). We only
        // need to know whether it's less than SHIP_CLEARANCE, and for positive numbers
        // a < b exactly when a² < b². So compare the squares and skip the square root.
        float clearanceSquared = SHIP_CLEARANCE * SHIP_CLEARANCE;
        float radius = DOT_RADIUS * zoom;

        // Filled shapes rather than outlines. WorldRenderer turned on auto shape type,
        // so we can switch here without ending and restarting the batch.
        shapes.set(ShapeType.Filled);

        // Visit every STEPS_PER_DOT-th point: one dot per SECONDS_PER_DOT.
        // `i += STEPS_PER_DOT` is the loop's step, like TypeScript.
        for (int i = 0; i < count; i += STEPS_PER_DOT) {
            // Point i is i physics steps ahead, and each step is TIME_STEP seconds.
            float time = i * PhysicsWorld.TIME_STEP;

            // Outside the animation window: not drawn this frame.
            // `continue` skips to the next loop iteration, like in TypeScript.
            if (time < windowStart || time > windowEnd) continue;

            // Point i's x is at float index 2i, and its y right after it at 2i + 1.
            // Add the offset so the line lines up with the drawn ship.
            float x = points.get(2 * i) + offsetX;
            float y = points.get(2 * i + 1) + offsetY;

            float dx = x - shipX;
            float dy = y - shipY;
            if (dx * dx + dy * dy < clearanceSquared) continue;

            // Fade along the line. `t` is how far along it this dot is, as a fraction:
            // 0 at the ship, 1 at the far end. lerp ("linear interpolation") blends two
            // values by that fraction: lerp(a, b, t) = a + (b − a) × t. So t = 0 gives
            // fully solid, t = 1 gives FAR_END_ALPHA, and halfway gives halfway between.
            float t = time / TrajectoryPredictor.PREDICTION_SECONDS;
            float alpha = MathUtils.lerp(1f, FAR_END_ALPHA, t);

            // Passing four floats sets the color without changing the shared DOT_COLOR
            // object. (DOT_COLOR is `final`, but that only stops the field pointing at
            // a different Color; the Color's own r, g, b, a could still be changed.)
            // The alpha only shows because WorldRenderer turned on GL blending.
            shapes.setColor(DOT_COLOR.r, DOT_COLOR.g, DOT_COLOR.b, alpha);
            shapes.circle(x, y, radius, DOT_SEGMENTS);
        }
    }
}
