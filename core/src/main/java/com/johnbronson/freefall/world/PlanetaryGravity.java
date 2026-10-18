package com.johnbronson.freefall.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;

/**
 * Newtonian gravity toward every planet, applied as a force on every dynamic body.
 * Close to a planet the pull is capped: below {@link Planet#GRAVITY_PLATEAU_RADIUS} it
 * stays at {@link Planet#SURFACE_GRAVITY} instead of growing, so a descent doesn't feel
 * like the planet suddenly grabs the ship.
 *
 * <p>Box2D's built-in gravity is one fixed vector for the whole world, which suits a
 * platformer. Here "down" depends on where you are, so the world's gravity is zero and
 * this class does the work before each step.
 */
public final class PlanetaryGravity {

    private final Array<Planet> planets;

    private final Array<Body> bodies = new Array<>();
    private final Vector2 acceleration = new Vector2();

    public PlanetaryGravity(Array<Planet> planets) {
        this.planets = planets;
    }

    /**
     * Gravitational acceleration at {@code point}, in m/s², written into {@code out}.
     *
     * <p>This is a pure function, with no Box2D state involved, so a trajectory
     * predictor can call it at points the ship hasn't reached yet.
     */
    public Vector2 accelerationAt(Vector2 point, Vector2 out) {
        out.setZero();
        for (Planet planet : planets) {
            float dx = planet.getCenter().x - point.x;
            float dy = planet.getCenter().y - point.y;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            // Skip the planet if the distance is 0 to avoid potentially dividing by zero later
            if (distance == 0f) {
                continue;
            }

            // Clamp only the distance used for strength, so gravity stays flat below the plateau. The direction must
            // still use the real distance, or the unit vector shrinks and gravity weakens near the ground.
            float pullDistance = Math.max(distance, Planet.GRAVITY_PLATEAU_RADIUS);
            float magnitude = planet.getGravitationalParameter() / (pullDistance * pullDistance);
            out.add(dx / distance * magnitude, dy / distance * magnitude);
        }
        return out;
    }

    /** Call once before every physics step. Box2D clears forces after each step. */
    public void applyTo(World world) {
        world.getBodies(bodies);
        for (Body body : bodies) {
            if (body.getType() != BodyDef.BodyType.DynamicBody) continue;

            // Gravity is an acceleration, but Box2D takes forces: F = m * a.
            accelerationAt(body.getWorldCenter(), acceleration).scl(body.getMass());

            // wake = false: a sleeping body stays asleep. A ship resting on a landing
            // zone is in balance, so gravity has nothing to wake it up for.
            body.applyForceToCenter(acceleration, false);
        }
    }
}
