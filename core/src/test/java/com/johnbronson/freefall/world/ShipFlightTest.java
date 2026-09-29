package com.johnbronson.freefall.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
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
            ship.beforeStep(dt);
            gravity.applyTo(physics.getWorld());
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
        Ship ship = world.placeShip(0.2f, 0f, 2f);

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
        Ship ship = world.placeShip(0.2f, 30f, 1f);

        world.run(1f);

        assertEquals(FlightState.CRASHED, ship.getState());
        assertTrue(ship.getCrashReason().contains("tilted"), ship.getCrashReason());
    }

    @Test
    void thrustWakesALandedShipAndLiftsOff() {
        world = new TestWorld();
        Ship ship = world.placeShip(0.05f, 0f, 0f);
        world.run(3f);
        assertFalse(ship.getBody().isAwake());

        ship.setThrottle(1f);
        world.run(1f);

        assertTrue(ship.getBody().isAwake());
        assertEquals(FlightState.FLYING, ship.getState());
        assertTrue(world.planet.altitudeOf(ship.getBody().getPosition()) > 2f);
    }

    @Test
    void attitudeControlTurnsAtTheRequestedRateThenHolds() {
        world = new TestWorld();
        Ship ship = world.placeShip(30f, 0f, 0f);

        ship.setTurnInput(1f);
        world.run(0.5f);
        assertEquals(MathUtils.PI2, ship.getBody().getAngularVelocity(), 0.01f);

        ship.setTurnInput(0f);
        world.run(0.5f);
        assertEquals(0f, ship.getBody().getAngularVelocity(), 0.01f);
    }

    @Test
    void restartReplacesACrashedShipWithAFreshLandedOne() {
        GameWorld game = new GameWorld(1L);
        try {
            Ship crashed = game.getShip();
            crashed.getBody().setLinearVelocity(0f, -30f);
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
