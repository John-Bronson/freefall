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
    void fallsOffWithTheSquareOfDistance() {
        Planet planet = new Planet(physics.getWorld(), 0f, 0f, 1L);
        PlanetaryGravity gravity = new PlanetaryGravity(Array.with(planet));

        Vector2 a = gravity.accelerationAt(new Vector2(0f, 2f * Planet.LANDING_ZONE_RADIUS), new Vector2());

        assertEquals(0f, a.x, 1e-3f);
        assertEquals(-Planet.SURFACE_GRAVITY / 4f, a.y, 1e-3f);
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
