package com.johnbronson.freefall.world;

import com.badlogic.gdx.math.RandomXS128;
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

    /**
     * Hands out a new terrain seed on every restart. It's seeded itself (from the seed
     * passed to the constructor), so a given starting seed always gives the same series
     * of worlds: the first one, the one after one restart, and so on. That keeps tests
     * repeatable, while the game still gets a new world each launch, because
     * {@code Main} passes a random starting seed.
     */
    private final RandomXS128 restartSeeds;

    /** @param terrainSeed decides the terrain; the same seed builds the same planets. */
    public GameWorld(long terrainSeed) {
        physics.getWorld().setContactListener(new ShipContactListener());
        restartSeeds = new RandomXS128(terrainSeed);

        buildPlanets(terrainSeed);
        ship = spawnShip();
    }

    /**
     * Creates both planets from one seed. Each planet's seed decides its hills and where
     * its landing zone is. The second planet uses {@code seed + 1}, so the two planets
     * don't look the same.
     */
    private void buildPlanets(long seed) {
        World world = physics.getWorld();
        planets.add(new Planet(world, -PLANET_DISTANCE_FROM_ORIGIN, 0f, seed));
        planets.add(new Planet(world, PLANET_DISTANCE_FROM_ORIGIN, 0f, seed + 1));
    }

    /**
     * Throws the whole world away and builds a new one: new hills, new landing sites,
     * and a fresh ship on planet 1's new site.
     *
     * <p>Only runs outside {@code world.step()} (see {@link #update}): Box2D locks the
     * world while it steps, and destroying or creating bodies then crashes.
     */
    private void restart() {
        // 1. Destroy the old Box2D bodies: the ship first, then the planets.
        ship.removeFrom(physics.getWorld());
        for (Planet planet : planets) {
            planet.removeFrom(physics.getWorld());
        }

        // 2. Empty the list and refill it. The list itself must stay the same object:
        //    PlanetaryGravity and TrajectoryPredictor were handed this exact list in
        //    their constructors and keep reading it. Assigning a new Array to `planets`
        //    would leave them looking at the old, destroyed planets. (The `final` on the
        //    field enforces this: the compiler won't allow `planets = new Array<>()`.)
        //    The renderer, camera and HUD ask GameWorld for planets every frame, so
        //    they pick up the new ones on their own.
        planets.clear();
        buildPlanets(restartSeeds.nextLong());

        // 3. A new ship, on planet 1's new landing site.
        ship = spawnShip();
    }

    /** Parks the ship on the first planet's landing zone, pointing up. */
    private Ship spawnShip() {
        Planet home = planets.first();
        home.getLandingSite(SPAWN_HEIGHT, spawnPoint);
        return new Ship(physics.getWorld(), spawnPoint, Ship.uprightAngle(home.getLandingSiteAngle()));
    }

    /** Call once per rendered frame. */
    public void update(float frameDelta) {
        physics.update(frameDelta, this);

        // Outside world.step(), so destroying and creating bodies is safe.
        if (restartRequested) {
            restartRequested = false;
            restart();
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

    /** Rebuilds the world (new planets, new ship) at the next update. See {@link #restart()}. */
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
