package com.johnbronson.freefall.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.johnbronson.freefall.physics.PhysicsWorld;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Checks the predicted line against the real thing: predict, then let Box2D fly the
 * ship hands-off and compare positions step by step.
 *
 * <p>The comparison allows a small error. Box2D is C++ and may do the float math in a
 * slightly different order than Java, and the tiny differences add up over hundreds
 * of steps.
 *
 * <h2>Reading a JUnit test</h2>
 * JUnit is Java's standard test framework, the counterpart of Jasmine or Jest.
 * <ul>
 *   <li>{@code @Test} marks a test method, like {@code it('...', () => {...})}. The
 *       method name is the description, so it's written as a sentence.</li>
 *   <li>{@code @BeforeEach} / {@code @AfterEach} run around every test, like
 *       {@code beforeEach} / {@code afterEach}. {@code @BeforeAll} runs once, first,
 *       and must be {@code static} because no test object exists yet.</li>
 *   <li>{@code assertEquals(expected, actual, tolerance, message)} passes when the two
 *       numbers are within {@code tolerance} of each other. Floats are rarely exactly
 *       equal, so number checks almost always pass a tolerance.</li>
 *   <li>{@code assertTrue(condition, message)}: the message is shown only on failure.</li>
 * </ul>
 * The {@code import static} lines at the top let us write {@code assertEquals(...)}
 * instead of {@code Assertions.assertEquals(...)}.
 *
 * <p>The class and its methods have no {@code public}: JUnit 5 doesn't need it, and
 * package-private lets the tests read package-private tuning constants such as
 * {@code Ship.SURFACE_TERMINAL_SPEED}.
 */
class TrajectoryPredictorTest {

    /**
     * Allowed gap between predicted and real position, in meters. In practice the gap
     * is around 0.00001 m, because both sides do the same float math.
     */
    private static final float TOLERANCE = 0.01f;

    /**
     * Box2D's hard limit on how far a body may move in one step ({@code b2_maxTranslation}
     * in its C++ source). Faster bodies are slowed down, and the line would run ahead.
     * It's not exposed through the Java API, so it's copied here.
     */
    private static final float BOX2D_MAX_TRANSLATION = 2f;

    /** Short name for the physics step length, since the tests use it a lot. */
    private static final float DT = PhysicsWorld.TIME_STEP;

    /**
     * A fresh world for each test. JUnit creates a new test object per test, and
     * {@code @BeforeEach} fills this in, so no test can leak state into another.
     */
    private GameWorld game;

    @BeforeAll
    static void loadNatives() {
        // Box2D is a native (C++) library, not Java. These two calls load the compiled
        // library files so Java can call into them, without opening a game window.
        GdxNativesLoader.load();
        Box2D.init();
    }

    @BeforeEach
    void createWorld() {
        // The argument is the terrain seed; a fixed seed gives the same terrain every
        // run, so tests are repeatable. `1L` is a `long` literal (the L suffix), since
        // the constructor takes a long.
        game = new GameWorld(1L);
    }

    @AfterEach
    void disposeWorld() {
        // Box2D memory lives outside Java, so the garbage collector can't free it.
        // dispose() frees it by hand. libGDX uses this pattern (the Disposable
        // interface) for anything holding native or graphics memory.
        game.dispose();
    }

    /**
     * Teleports the freshly spawned ship. It hasn't been stepped yet, so it hasn't
     * touched the pad and is still FLYING.
     */
    private Body placeShip(Vector2 position, float vx, float vy) {
        Body body = game.getShip().getBody();
        // setTransform teleports a body: new position, and an angle (0 = nose along +Y).
        // Fine in a test setup; in the game it would skip collisions.
        body.setTransform(position, 0f);
        body.setLinearVelocity(vx, vy);
        return body;
    }

    /**
     * A point {@code height} m above landing zone 0 of the given planet. Planet 0 is
     * the home planet (where the ship spawns), planet 1 the other one.
     */
    private Vector2 aboveLandingZone(int planetIndex, float height) {
        return game.getPlanets().get(planetIndex).getLandingSite(0, height, new Vector2());
    }

    /**
     * Predicts, then steps the real world and checks it follows the line.
     *
     * <p>Comparison stops if the real ship touches the ground. On a slope the round
     * hull touches a little before its center is {@code HULL_RADIUS} above the ground
     * straight below, which is what the predictor checks, so the line can run a step
     * or two past the real touchdown. That's accurate enough for a guide line.
     *
     * @return the number of predicted points, for further checks.
     */
    private int assertShipFollowsPrediction() {
        TrajectoryPredictor prediction = game.predictTrajectory();
        // toArray() copies the floats into a plain Java array (float[]), so later
        // predict() calls can't change what we compare against. Plain arrays are
        // indexed with [i], like in TypeScript.
        float[] predicted = prediction.getPoints().toArray();
        int count = prediction.getPointCount();
        assertTrue(count > 1, "expected a line, got " + count + " points");

        Body body = game.getShip().getBody();
        // Point 0 is where the ship is now, so start comparing at point 1.
        for (int i = 1; i < count; i++) {
            // Passing exactly one step's worth of time runs exactly one physics step
            // (see PhysicsWorld's accumulator).
            game.update(DT);
            // The step that hits the ground is changed by the collision. Stop there.
            if (game.getShip().getState() != Ship.FlightState.FLYING) break;
            Vector2 actual = body.getPosition();
            // Points are packed [x0, y0, x1, y1, ...]: point i's x is at 2i, its y at 2i + 1.
            // `"x at step " + i` builds the failure message; Java's + joins a string
            // and a number, like TypeScript's.
            assertEquals(predicted[2 * i], actual.x, TOLERANCE, "x at step " + i);
            assertEquals(predicted[2 * i + 1], actual.y, TOLERANCE, "y at step " + i);
        }
        return count;
    }

    @Test
    void predictsTheFullHorizonInSpace() {
        // The barycenter (origin) is between the planets, far outside both atmospheres.
        // Any velocity will do; this one isn't straight at a planet, so the path bends.
        placeShip(new Vector2(0f, 0f), 15f, 25f);

        int count = assertShipFollowsPrediction();

        // Point 0 is "now", plus one point per step.
        assertEquals(TrajectoryPredictor.STEP_COUNT + 1, count);
        assertFalse(game.predictTrajectory().hitsGround());
    }

    @Test
    void includesDragInTheAtmosphere() {
        // Inside the air, halfway between the landing zones and the atmosphere's edge,
        // moving sideways fast enough that drag clearly matters.
        float height = 0.5f * (Planet.ATMOSPHERE_RADIUS - Planet.LANDING_ZONE_RADIUS);
        Vector2 start = aboveLandingZone(1, height);
        placeShip(start, 0f, 0f);
        // "Up" is the direction from the planet's center to the ship. Read left to right:
        // cpy() makes a copy (so we don't change `start`), sub() subtracts the center,
        // and nor() ("normalize") scales it to length 1, keeping only the direction.
        Vector2 up = start.cpy().sub(game.getPlanets().get(1).getCenter()).nor();
        // Sideways = perpendicular to up. Rotating (x, y) by 90° counter-clockwise gives
        // (−y, x). Twice the terminal speed, so drag is strong and clearly matters.
        float speed = 2f * Ship.SURFACE_TERMINAL_SPEED;
        game.getShip().getBody().setLinearVelocity(-up.y * speed, up.x * speed);

        assertShipFollowsPrediction();
    }

    @Test
    void includesTheBoosterBurn() {
        float startSpeed = 10f;
        Body body = placeShip(new Vector2(0f, 0f), startSpeed, 0f);
        // Light the booster and run one step so it's burning when we predict.
        game.getShip().fireBooster();
        game.update(DT);
        assertTrue(game.getShip().isBoosterBurning());

        assertShipFollowsPrediction();
        // Sanity check that the burn really happened: the ship sped up. The thrust is
        // along the nose (+Y, since the angle is 0), across the starting velocity, so
        // it can only add speed.
        assertTrue(body.getLinearVelocity().len() > startSpeed);
    }

    @Test
    void stopsWhereTheHullMeetsTheGround() {
        // Falling straight down onto a pad from a little way up. Velocity = −up × speed
        // points at the planet's center.
        float startHeight = 20f;
        float startSpeed = 5f;
        Vector2 start = aboveLandingZone(0, startHeight);
        Vector2 up = start.cpy().sub(game.getPlanets().first().getCenter()).nor();
        placeShip(start, -up.x * startSpeed, -up.y * startSpeed);

        TrajectoryPredictor prediction = game.predictTrajectory();
        assertTrue(prediction.hitsGround());
        assertTrue(prediction.getPointCount() < TrajectoryPredictor.STEP_COUNT + 1, "line should end early");

        // The line stops on the first step at or below hull height, so the impact
        // point is at most one step's travel below touchdown height. Box2D never moves
        // a body more than BOX2D_MAX_TRANSLATION per step, so that bounds a step.
        float altitude = game.getPlanets().first().altitudeOf(prediction.getImpactPoint());
        assertTrue(altitude <= Ship.HULL_RADIUS, "altitude " + altitude);
        assertTrue(altitude > Ship.HULL_RADIUS - BOX2D_MAX_TRANSLATION, "altitude " + altitude);
    }

    @Test
    void marksWhereTheLineEntersAnAtmosphere() {
        // Just outside the home planet's air, diving straight at it.
        Planet planet = game.getPlanets().first();
        // A direction from the planet's center: +X, pointing toward the other planet.
        Vector2 direction = new Vector2(1f, 0f);
        float gapToAir = 20f;
        float diveSpeed = 30f;
        // start = center + direction × (atmosphere radius + gap). mulAdd(v, s) adds v × s.
        Vector2 start = planet.getCenter().cpy().mulAdd(direction, Planet.ATMOSPHERE_RADIUS + gapToAir);
        placeShip(start, -direction.x * diveSpeed, -direction.y * diveSpeed);

        TrajectoryPredictor prediction = game.predictTrajectory();
        assertTrue(prediction.entersAtmosphere());
        // The marked point is inside the air, at most one step's travel past its edge.
        // dst() is the distance between two points.
        float distance = planet.getCenter().dst(prediction.getAtmosphereEntryPoint());
        assertTrue(distance < Planet.ATMOSPHERE_RADIUS, "distance " + distance);
        assertTrue(distance > Planet.ATMOSPHERE_RADIUS - BOX2D_MAX_TRANSLATION, "distance " + distance);
    }

    @Test
    void drawsNothingForALandedShip() {
        // Let the spawned ship settle onto its pad: one second of steps. (A for loop
        // whose body is a single statement can skip the braces, as in TypeScript.)
        for (int i = 0; i < Math.round(1f / DT); i++) game.update(DT);
        assertEquals(Ship.FlightState.LANDED, game.getShip().getState());

        assertEquals(0, game.predictTrajectory().getPointCount());
    }

    /**
     * The predictor doesn't copy Box2D's per-step speed cap. That's fine as long as
     * normal flight stays well below it. Circular orbit speed just outside the air is
     * about the fastest a ship coasts there.
     *
     * <p>Where v = sqrt(GM / r) comes from: in a circular orbit, gravity is exactly the
     * pull needed to keep bending the path into a circle. Gravity's acceleration is
     * GM / r², and the acceleration needed to move in a circle is v² / r. Set them
     * equal: v² / r = GM / r², so v² = GM / r. (GM is the "gravitational parameter",
     * here called mu, μ.)
     */
    @Test
    void orbitalSpeedStaysUnderBox2DsSpeedCap() {
        float mu = game.getPlanets().first().getGravitationalParameter();
        // Math.sqrt works on doubles (64-bit) and returns one; `(float)` converts the
        // answer back to a 32-bit float. Java makes you write narrowing conversions
        // explicitly because they can lose precision.
        float orbitalSpeed = (float) Math.sqrt(mu / Planet.ATMOSPHERE_RADIUS);
        // Speed cap = max distance per step ÷ step length.
        float box2dSpeedCap = BOX2D_MAX_TRANSLATION / DT;
        assertTrue(orbitalSpeed < 0.75f * box2dSpeedCap,
            "orbital speed " + orbitalSpeed + " m/s is close to Box2D's cap of " + box2dSpeedCap);
    }

    /** STEP_COUNT steps of DT seconds each should add up to the prediction horizon. */
    @Test
    void stepCountMatchesTheHorizon() {
        // Tolerance of half a step: rounding to a whole number of steps can be off by
        // at most that much.
        assertEquals(TrajectoryPredictor.PREDICTION_SECONDS,
            TrajectoryPredictor.STEP_COUNT * DT, 0.5f * DT);
    }
}
