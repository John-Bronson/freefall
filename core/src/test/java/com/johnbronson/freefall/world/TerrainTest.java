package com.johnbronson.freefall.world;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.MathUtils;
import org.junit.jupiter.api.Test;

class TerrainTest {

    private static final LandingZone FIRST_QUADRANT_PAD = LandingZone.ofDegrees(0f, 22.5f);

    private static Terrain terrain(long seed) {
        return new Terrain(seed, 60f, 78f, 69f, FIRST_QUADRANT_PAD);
    }

    private static float[] radii(Terrain terrain) {
        float[] radii = new float[Terrain.VERTEX_COUNT];
        for (int i = 0; i < radii.length; i++) radii[i] = terrain.getRadius(i);
        return radii;
    }

    @Test
    void landingZoneVerticesAreFlatAndIncludeBothEnds() {
        Terrain terrain = terrain(1L);
        // 22.5° is exactly 8 steps of 2.8125°, so vertices 0..8 are all in the zone.
        for (int i = 0; i <= 8; i++) {
            assertTrue(terrain.isLandingVertex(i), "vertex " + i);
            assertEquals(69f, terrain.getRadius(i));
        }
        assertFalse(terrain.isLandingVertex(9));
        assertFalse(terrain.isLandingVertex(Terrain.VERTEX_COUNT - 1));
    }

    @Test
    void sameSeedBuildsSameTerrain() {
        assertArrayEquals(radii(terrain(42L)), radii(terrain(42L)));
    }

    @Test
    void differentSeedsBuildDifferentTerrain() {
        assertFalse(java.util.Arrays.equals(radii(terrain(1L)), radii(terrain(2L))));
    }

    @Test
    void terrainStaysWithinItsHeightRange() {
        for (float radius : radii(terrain(3L))) {
            assertTrue(radius >= 60f && radius <= 78f, "radius " + radius);
        }
    }

    @Test
    void landingSiteIsMidSegmentInsideTheZone() {
        float angle = terrain(1L).landingSiteAngle(FIRST_QUADRANT_PAD);
        assertEquals(4.5f * Terrain.ANGLE_BETWEEN_VERTICES, angle, 1e-6f);
    }

    @Test
    void surfaceRadiusWrapsNegativeAngles() {
        Terrain terrain = terrain(5L);
        int last = Terrain.VERTEX_COUNT - 1;
        float lastVertexAngle = last * Terrain.ANGLE_BETWEEN_VERTICES;
        assertEquals(terrain.getRadius(last), terrain.surfaceRadiusAt(lastVertexAngle - MathUtils.PI2), 1e-3f);
        assertEquals(69f, terrain.surfaceRadiusAt(0.1f), 1e-4f);
    }
}
