package com.johnbronson.freefall.physics;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;

/**
 * Remembers where a body was before the latest physics step, so it can be drawn
 * between steps.
 *
 * <p>With a fixed timestep, a 144 Hz monitor sees 2–3 frames per 60 Hz physics step.
 * Drawing the raw body position would show the same spot for several frames and then
 * jump, which reads as stutter. Blending from the previous to the current transform by
 * {@link PhysicsWorld#getAlpha()} gives smooth motion, at the cost of drawing up to
 * one step (about 17 ms) in the past.
 */
public final class InterpolatedBody {

    private final Body body;
    private final Vector2 previousPosition = new Vector2();
    private float previousAngle;

    /** Reused return value, so drawing allocates nothing. Copy it if you need to keep it. */
    private final Vector2 interpolatedPosition = new Vector2();

    public InterpolatedBody(Body body) {
        this.body = body;
        savePreviousTransform();
    }

    /** Call before every physics step. */
    public void savePreviousTransform() {
        previousPosition.set(body.getPosition());
        previousAngle = body.getAngle();
    }

    public Vector2 getPosition(float alpha) {
        return interpolatedPosition.set(previousPosition).lerp(body.getPosition(), alpha);
    }

    public float getAngle(float alpha) {
        // Box2D never wraps a body's angle into [0, 2π): a ship that spins twice
        // reads 4π. The angle is continuous, so a plain lerp is correct.
        return MathUtils.lerp(previousAngle, body.getAngle(), alpha);
    }

    public Body getBody() {
        return body;
    }
}
