package com.johnbronson.freefall.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;

/**
 * Newtonian gravity toward every planet, applied as a force on every dynamic body.
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
            // Don't let the distance fall below the planet's lowest terrain. Nothing
            // solid can get that close, and it keeps a = GM / r² finite if something
            // ever ends up at the center.
            float distanceSquared = Math.max(dx * dx + dy * dy,
                Planet.MIN_TERRAIN_RADIUS * Planet.MIN_TERRAIN_RADIUS);
            float distance = (float) Math.sqrt(distanceSquared);

            float magnitude = planet.getGravitationalParameter() / distanceSquared;
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
