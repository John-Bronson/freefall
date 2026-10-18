package com.johnbronson.freefall.world;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.johnbronson.freefall.physics.PhysicsWorld;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PlanetaryGravityTest {

    private final PhysicsWorld physics = new PhysicsWorld();

    @BeforeAll
    static void loadNatives() {
        GdxNativesLoader.load();
        Box2D.init();
    }

    @AfterEach
    void disposeWorld() {
        physics.dispose();
    }

    @Test
    void pullsWithSurfaceGravityAtLandingZoneHeight() {
        Planet planet = new Planet(physics.getWorld(), 0f, 0f, 1L);
        PlanetaryGravity gravity = new PlanetaryGravity(Array.with(planet));

        Vector2 a = gravity.accelerationAt(new Vector2(Planet.LANDING_ZONE_RADIUS, 0f), new Vector2());

        assertEquals(-Planet.SURFACE_GRAVITY, a.x, 1e-3f);
        assertEquals(0f, a.y, 1e-3f);
    }

    @Test
    void fallsOffWithTheSquareOfDistanceAboveThePlateau() {
        Planet planet = new Planet(physics.getWorld(), 0f, 0f, 1L);
        PlanetaryGravity gravity = new PlanetaryGravity(Array.with(planet));

        // Twice the plateau radius: a quarter of the plateau's pull.
        Vector2 a = gravity.accelerationAt(new Vector2(0f, 2f * Planet.GRAVITY_PLATEAU_RADIUS), new Vector2());

        assertEquals(0f, a.x, 1e-3f);
        assertEquals(-Planet.SURFACE_GRAVITY / 4f, a.y, 1e-3f);
    }

    @Test
    void holdsSteadyBelowThePlateau() {
        Planet planet = new Planet(physics.getWorld(), 0f, 0f, 1L);
        PlanetaryGravity gravity = new PlanetaryGravity(Array.with(planet));

        // Two heights below the plateau, in different directions. Both should feel the
        // full surface gravity, pointing straight at the center. If the direction were
        // divided by the clamped distance instead of the real one, the pull would come
        // out weaker the closer the point is (about 14.2 instead of 18 at 70 m).
        Vector2 low = gravity.accelerationAt(new Vector2(70f, 0f), new Vector2());
        Vector2 higher = gravity.accelerationAt(new Vector2(0f, -85f), new Vector2());

        assertEquals(-Planet.SURFACE_GRAVITY, low.x, 1e-3f);
        assertEquals(0f, low.y, 1e-3f);
        assertEquals(0f, higher.x, 1e-3f);
        assertEquals(Planet.SURFACE_GRAVITY, higher.y, 1e-3f);
    }

    @Test
    void cancelsOutHalfwayBetweenTwoEqualPlanets() {
        Planet left = new Planet(physics.getWorld(), -240f, 0f, 1L);
        Planet right = new Planet(physics.getWorld(), 240f, 0f, 2L);
        PlanetaryGravity gravity = new PlanetaryGravity(Array.with(left, right));

        Vector2 a = gravity.accelerationAt(new Vector2(0f, 0f), new Vector2());

        assertEquals(0f, a.len(), 1e-4f);
    }
}
