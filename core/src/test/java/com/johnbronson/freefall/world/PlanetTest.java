package com.johnbronson.freefall.world;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2D;
import com.badlogic.gdx.utils.GdxNativesLoader;
import com.johnbronson.freefall.physics.PhysicsWorld;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PlanetTest {

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

    private Planet planetAtOrigin() {
        return new Planet(physics.getWorld(), 0f, 0f, 1L);
    }

    @Test
    void airIsEmptyAtTheTopOfTheAtmosphere() {
        Planet planet = planetAtOrigin();

        assertEquals(0f, planet.airDensityAt(new Vector2(Planet.ATMOSPHERE_RADIUS, 0f)), 1e-6f);
    }

    @Test
    void airIsEmptyOutsideTheAtmosphere() {
        Planet planet = planetAtOrigin();

        // Well outside: the unclamped ramp would go negative here.
        assertEquals(0f, planet.airDensityAt(new Vector2(0f, 150f)), 1e-6f);
    }

    @Test
    void airIsFullAtTheLandingZones() {
        Planet planet = planetAtOrigin();

        float density = planet.airDensityAt(new Vector2(-Planet.LANDING_ZONE_RADIUS, 0f));

        assertEquals(Planet.SURFACE_AIR_DENSITY, density, 1e-6f);
    }

    @Test
    void airStaysFullInValleysBelowTheLandingZones() {
        Planet planet = planetAtOrigin();

        // Below the landing zones: the unclamped ramp would go above full density here.
        float density = planet.airDensityAt(new Vector2(0f, -65f));

        assertEquals(Planet.SURFACE_AIR_DENSITY, density, 1e-6f);
    }

    @Test
    void airFollowsTheFalloffCurveHalfwayDown() {
        Planet planet = planetAtOrigin();
        float halfway = (Planet.LANDING_ZONE_RADIUS + Planet.ATMOSPHERE_RADIUS) / 2f;

        float density = planet.airDensityAt(new Vector2(halfway, 0f));

        // Written in terms of the constants, so it still holds after retuning the curve.
        float expected = Planet.SURFACE_AIR_DENSITY
            * (float) Math.pow(0.5, Planet.AIR_DENSITY_FALLOFF_EXPONENT);
        assertEquals(expected, density, 1e-5f);
    }
}
