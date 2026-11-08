package com.johnbronson.freefall.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.johnbronson.freefall.physics.PhysicsWorld;
import com.johnbronson.freefall.physics.StepListener;
import com.johnbronson.freefall.world.Ship.FlightState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Flies the real ship in a real Box2D world with no window. Box2D only needs its
 * native library; rendering and input are not involved.
 */
class ShipFlightTest {

    /** One planet at the origin, the ship, and the same per-step wiring as GameWorld. */
    private static final class TestWorld implements StepListener {
        final PhysicsWorld physics = new PhysicsWorld();
        final Planet planet = new Planet(physics.getWorld(), 0f, 0f, 7L);
        final PlanetaryGravity gravity = new PlanetaryGravity(Array.with(planet));
        Ship ship;
        /** When set, every step uses this air density instead of the planet's. */
        Float fixedAirDensity;
        /** Off for tests that only care about rotation, so the ship never hits the ground. */
        boolean gravityOn = true;

        TestWorld() {
            physics.getWorld().setContactListener(new ShipContactListener());
        }

        /** Puts the ship above landing zone 0, tilted, and moving toward the ground. */
        Ship placeShip(float heightAboveGround, float tiltDegrees, float descentSpeed) {
            float siteAngle = planet.getLandingSiteAngle(0);
            Vector2 position = planet.getLandingSite(0, Ship.HULL_RADIUS + heightAboveGround, new Vector2());
            ship = new Ship(physics.getWorld(), position, Ship.uprightAngle(siteAngle) + tiltDegrees * MathUtils.degRad);
            ship.getBody().setLinearVelocity(
                -MathUtils.cos(siteAngle) * descentSpeed, -MathUtils.sin(siteAngle) * descentSpeed);
            return ship;
        }

        void run(float seconds) {
            int steps = Math.round(seconds / PhysicsWorld.TIME_STEP);
            for (int i = 0; i < steps; i++) {
                physics.update(PhysicsWorld.TIME_STEP, this);
            }
        }

        @Override
        public void beforeStep(float dt) {
            float airDensity = fixedAirDensity != null
                ? fixedAirDensity
                : planet.airDensityAt(ship.getBody().getPosition());
            ship.beforeStep(dt, airDensity);
            if (gravityOn) gravity.applyTo(physics.getWorld());
        }

        @Override
        public void afterStep(float dt) {
            ship.afterStep();
        }
    }

    private TestWorld world;

    @BeforeAll
    static void loadNatives() {
        GdxNativesLoader.load();
        Box2D.init();
    }

    @AfterEach
    void disposeWorld() {
        if (world != null) world.physics.dispose();
    }

    @Test
    void gentleUprightTouchdownLandsAndFallsAsleep() {
        world = new TestWorld();
        Ship ship = world.placeShip(0.2f, 0f, 0.2f * Ship.MAX_LANDING_SPEED);

        world.run(1f);
        assertEquals(FlightState.LANDED, ship.getState());

        // Box2D puts a body to sleep after it has been nearly still for half a second.
        world.run(2f);
        assertFalse(ship.getBody().isAwake(), "resting ship should be asleep");
    }

    @Test
    void fastTouchdownCrashes() {
        world = new TestWorld();
        Ship ship = world.placeShip(0.5f, 0f, 2f * Ship.MAX_LANDING_SPEED);

        world.run(1f);

        assertEquals(FlightState.CRASHED, ship.getState());
        assertTrue(ship.getCrashReason().contains("m/s"), ship.getCrashReason());
    }

    @Test
    void tiltedTouchdownCrashes() {
        world = new TestWorld();
        Ship ship = world.placeShip(0.2f, 2f * Ship.MAX_LANDING_TILT * MathUtils.radDeg, 0.1f * Ship.MAX_LANDING_SPEED);

        world.run(1f);

        assertEquals(FlightState.CRASHED, ship.getState());
        assertTrue(ship.getCrashReason().contains("tilted"), ship.getCrashReason());
    }

    /** Time for the RCS to take the spin from zero to full rate, or from full rate to zero. */
    private static final float FULL_TURN_TIME = Ship.MAX_TURN_RATE / Ship.MAX_TURN_ACCELERATION;

    /** Spins the ship up to full counter-clockwise turn rate at the given air density, without gravity. */
    private Ship spinUp(float airDensity) {
        world = new TestWorld();
        world.fixedAirDensity = airDensity;
        world.gravityOn = false;
        Ship ship = world.placeShip(10f, 0f, 0f);
        ship.setTurnInput(1f);
        world.run(FULL_TURN_TIME + 0.5f);
        return ship;
    }

    @Test
    void attitudeControlTurnsAtTheRequestedRateInVacuum() {
        Ship ship = spinUp(0f);
        assertEquals(Ship.MAX_TURN_RATE, ship.getBody().getAngularVelocity(), 0.01f);
    }

    @Test
    void spinCoastsAfterReleaseInVacuum() {
        Ship ship = spinUp(0f);

        ship.setTurnInput(0f);
        world.run(1f);
        assertEquals(Ship.MAX_TURN_RATE, ship.getBody().getAngularVelocity(), 0.01f);
    }

    @Test
    void counterSteeringStopsTheSpinInVacuum() {
        Ship ship = spinUp(0f);

        // Full opposite input for exactly as long as it takes to cancel the spin.
        ship.setTurnInput(-1f);
        world.run(FULL_TURN_TIME);
        assertEquals(0f, ship.getBody().getAngularVelocity(), 0.05f);
    }

    @Test
    void airDampsTheSpinAfterRelease() {
        Ship ship = spinUp(1f);
        float spinAtRelease = ship.getBody().getAngularVelocity();

        // Three time constants (1 / (SPIN_DAMPING × density)) leave about 5% of the spin.
        ship.setTurnInput(0f);
        world.run(3f / Ship.SPIN_DAMPING);
        float spin = ship.getBody().getAngularVelocity();
        assertTrue(spin < 0.1f * spinAtRelease, "spin should mostly decay within three time constants, was " + spin);
        assertTrue(spin >= 0f, "damping must never reverse the spin, was " + spin);
    }

    @Test
    void dampingStopsButNeverReversesTheSpinInVeryThickAir() {
        Ship ship = spinUp(0f);

        ship.setTurnInput(0f);
        // Thick enough that one step's damping would overshoot tenfold without the cap.
        world.fixedAirDensity = 10f / (Ship.SPIN_DAMPING * PhysicsWorld.TIME_STEP);
        world.run(PhysicsWorld.TIME_STEP);
        assertEquals(0f, ship.getBody().getAngularVelocity(), 0.01f);
    }

    @Test
    void restartReplacesACrashedShipWithAFreshLandedOne() {
        GameWorld game = new GameWorld(1L);
        try {
            Ship crashed = game.getShip();
            crashed.getBody().setLinearVelocity(0f, -3f * Ship.MAX_LANDING_SPEED);
            for (int i = 0; i < 120; i++) game.update(PhysicsWorld.TIME_STEP);
            assertEquals(FlightState.CRASHED, crashed.getState());

            game.requestRestart();
            game.update(PhysicsWorld.TIME_STEP);
            for (int i = 0; i < 60; i++) game.update(PhysicsWorld.TIME_STEP);

            assertTrue(game.getShip() != crashed);
            assertEquals(FlightState.LANDED, game.getShip().getState());
            assertEquals(1, game.getBox2DWorld().getBodyCount() - game.getPlanets().size, "old ship body destroyed");
        } finally {
            game.dispose();
        }
    }

    /**
     * Regression: the hull is a circle, and the other planet's pull plus the slightly
     * curved pad made a landed ship roll away slowly. Landing locks the rotation.
     */
    @Test
    void landedShipDoesNotRollAndFallsAsleep() {
        GameWorld game = new GameWorld(1L);
        try {
            for (int i = 0; i < 60; i++) game.update(PhysicsWorld.TIME_STEP);
            Body body = game.getShip().getBody();
            float angleAfterLanding = body.getAngle();

            for (int i = 0; i < 600; i++) game.update(PhysicsWorld.TIME_STEP);
            float rolledDegrees = Math.abs(body.getAngle() - angleAfterLanding) * MathUtils.radDeg;
            assertTrue(rolledDegrees < 0.1f, "rolled " + rolledDegrees + "° in 10 s");
            assertFalse(body.isAwake(), "a resting ship should fall asleep");
        } finally {
            game.dispose();
        }
    }

    @Test
    void restingShipStaysLandedWithoutFlickering() {
        GameWorld game = new GameWorld(1L);
        try {
            for (int i = 0; i < 60; i++) game.update(PhysicsWorld.TIME_STEP);
            for (int i = 0; i < 600; i++) {
                game.update(PhysicsWorld.TIME_STEP);
                assertEquals(FlightState.LANDED, game.getShip().getState(), "step " + i);
            }
        } finally {
            game.dispose();
        }
    }

    @Test
    void gameWorldSpawnsTheShipLandedOnAPad() {
        GameWorld game = new GameWorld(1L);
        try {
            for (int i = 0; i < 60; i++) game.update(PhysicsWorld.TIME_STEP);
            assertEquals(FlightState.LANDED, game.getShip().getState());
        } finally {
            game.dispose();
        }
    }
}
