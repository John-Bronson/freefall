package com.johnbronson.freefall.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.johnbronson.freefall.physics.PhysicsWorld;
import com.johnbronson.freefall.physics.StepListener;

/**
 * The simulation: two planets and a ship in a Box2D world. This is the "model".
 * It knows nothing about input devices, cameras or drawing.
 *
 * <p>Per fixed step: save transforms for interpolation → apply forces → step Box2D →
 * apply game rules. See {@link StepListener}.
 */
public final class GameWorld implements StepListener, Disposable {

    /** The barycenter is the world origin. The planets sit on either side of it. */
    private static final float PLANET_DISTANCE_FROM_ORIGIN = 240f;

    /** Start just above the pad, so the first step settles the ship onto it. */
    private static final float SPAWN_HEIGHT = Ship.HULL_RADIUS + 0.05f;

    private final PhysicsWorld physics = new PhysicsWorld();
    private final Array<Planet> planets = new Array<>();
    private final PlanetaryGravity gravity = new PlanetaryGravity(planets);
    private Ship ship;
    private boolean restartRequested;

    private final Vector2 spawnPoint = new Vector2();

    /** @param terrainSeed decides the terrain; the same seed builds the same planets. */
    public GameWorld(long terrainSeed) {
        World world = physics.getWorld();
        world.setContactListener(new ShipContactListener());

        planets.add(new Planet(world, -PLANET_DISTANCE_FROM_ORIGIN, 0f, terrainSeed));
        planets.add(new Planet(world, PLANET_DISTANCE_FROM_ORIGIN, 0f, terrainSeed + 1));

        ship = spawnShip();
    }

    /** Parks the ship on the first landing zone of the first planet, pointing up. */
    private Ship spawnShip() {
        Planet home = planets.first();
        int zone = 0;
        home.getLandingSite(zone, SPAWN_HEIGHT, spawnPoint);
        return new Ship(physics.getWorld(), spawnPoint, Ship.uprightAngle(home.getLandingSiteAngle(zone)));
    }

    /** Call once per rendered frame. */
    public void update(float frameDelta) {
        physics.update(frameDelta, this);

        // Outside world.step(), so destroying and creating bodies is safe.
        if (restartRequested) {
            restartRequested = false;
            ship.removeFrom(physics.getWorld());
            ship = spawnShip();
        }
    }

    @Override
    public void beforeStep(float dt) {
        ship.beforeStep(dt);
        gravity.applyTo(physics.getWorld());
    }

    @Override
    public void afterStep(float dt) {
        ship.afterStep();
    }

    /** Replaces the ship with a fresh one at the next update. */
    public void requestRestart() {
        restartRequested = true;
    }

    public Planet nearestPlanetTo(Vector2 point) {
        Planet nearest = planets.first();
        for (Planet planet : planets) {
            if (planet.getCenter().dst2(point) < nearest.getCenter().dst2(point)) {
                nearest = planet;
            }
        }
        return nearest;
    }

    public Ship getShip() {
        return ship;
    }

    public Array<Planet> getPlanets() {
        return planets;
    }

    /** Blend factor for drawing between physics steps. See {@link PhysicsWorld#getAlpha()}. */
    public float getAlpha() {
        return physics.getAlpha();
    }

    public World getBox2DWorld() {
        return physics.getWorld();
    }

    @Override
    public void dispose() {
        physics.dispose();
    }
}
