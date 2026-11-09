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
    private static final float PLANET_DISTANCE_FROM_ORIGIN = 960f;

    /** Start just above the pad, so the first step settles the ship onto it. */
    private static final float SPAWN_HEIGHT = Ship.HULL_RADIUS + 0.05f;

    private final PhysicsWorld physics = new PhysicsWorld();
    private final Array<Planet> planets = new Array<>();
    private final PlanetaryGravity gravity = new PlanetaryGravity(planets);
    // Java runs field initializers top to bottom, so `planets` and `gravity` above
    // already exist when this line runs.
    private final TrajectoryPredictor predictor = new TrajectoryPredictor(planets, gravity);
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
        ship.beforeStep(dt, airDensityAt(ship.getBody().getPosition()));
        gravity.applyTo(physics.getWorld());
    }

    @Override
    public void afterStep(float dt) {
        ship.afterStep();
    }

    /** Relative air density at a point, from the nearest planet */
    public float airDensityAt(Vector2 point) {
        return nearestPlanetTo(point).airDensityAt(point);
    }

    /** Replaces the ship with a fresh one at the next update. */
    public void requestRestart() {
        restartRequested = true;
    }

    public Planet nearestPlanetTo(Vector2 point) {
        return Planet.nearestTo(planets, point);
    }

    /**
     * Works out the ship's coast line for the next few seconds and returns the
     * predictor holding the result. Renderers call this only while the line is on
     * screen, so a hidden line costs nothing.
     *
     * <p>The predictor is reused every call, so this allocates nothing. Read its
     * results the same frame; the next call overwrites them.
     */
    public TrajectoryPredictor predictTrajectory() {
        predictor.predict(ship);
        return predictor;
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
