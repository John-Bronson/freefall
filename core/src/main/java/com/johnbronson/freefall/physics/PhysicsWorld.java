package com.johnbronson.freefall.physics;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Disposable;

/**
 * Owns the Box2D {@link World} and advances it with a fixed timestep.
 *
 * <p>Box2D is most stable and repeatable when every step is the same length, but the
 * frame rate isn't. So real frame time is collected in an accumulator and spent in
 * {@link #TIME_STEP}-sized steps. The time left over is less than one step, and is
 * exposed as {@link #getAlpha()} so renderers can blend between the last two physics
 * states. See Glenn Fiedler, "Fix Your Timestep!".
 *
 * <p>All units are SI: meters, kilograms, seconds, radians.
 */
public final class PhysicsWorld implements Disposable {

    /** Length of one physics step in seconds. */
    public static final float TIME_STEP = 1f / 60f;

    /** Solver passes per step. The Box2D manual recommends 8 and 3. */
    private static final int VELOCITY_ITERATIONS = 8;
    private static final int POSITION_ITERATIONS = 3;

    /**
     * Longest frame we try to catch up on. After a long hitch (a breakpoint, a
     * dragged window), running every missed step would take longer than the frame
     * itself, and the game would never catch up (the "spiral of death").
     */
    private static final float MAX_FRAME_TIME = 0.25f;

    private final World world;
    private float accumulator;
    private float alpha;

    public PhysicsWorld() {
        // Gravity is zero because Box2D's gravity is one fixed direction for the
        // whole world. Planet gravity points at each planet, so PlanetaryGravity
        // applies it as a force instead.
        // doSleep = true lets bodies at rest drop out of the simulation until
        // something wakes them.
        world = new World(new Vector2(0f, 0f), true);
    }

    /**
     * Advances the simulation by one rendered frame's worth of time. Depending on
     * the frame rate this runs zero, one or several physics steps.
     */
    public void update(float frameDelta, StepListener listener) {
        accumulator += Math.min(frameDelta, MAX_FRAME_TIME);

        while (accumulator >= TIME_STEP) {
            listener.beforeStep(TIME_STEP);
            world.step(TIME_STEP, VELOCITY_ITERATIONS, POSITION_ITERATIONS);
            listener.afterStep(TIME_STEP);
            accumulator -= TIME_STEP;
        }

        alpha = accumulator / TIME_STEP;
    }

    /**
     * How far the current frame is between the previous physics step (0) and the
     * latest one (1). Pass to {@link InterpolatedBody} when drawing.
     */
    public float getAlpha() {
        return alpha;
    }

    public World getWorld() {
        return world;
    }

    /** The World is native (C++) memory; Java's garbage collector can't free it. */
    @Override
    public void dispose() {
        world.dispose();
    }
}
