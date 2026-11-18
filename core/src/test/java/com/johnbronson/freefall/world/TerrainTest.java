package com.johnbronson.freefall.world;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.MathUtils;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TerrainTest {

    private static final float MIN_RADIUS = 60f;
    private static final float MAX_RADIUS = 78f;
    private static final float LANDING_RADIUS = 69f;
    private static final int SEGMENTS = 8;

    /** Vertices 0..8 flat: 8 segments, 22.5°, starting on the +X axis. */
    private static final LandingZone FIRST_QUADRANT_PAD = new LandingZone(0, SEGMENTS);

    /** Starts 4 vertices before the end, so it covers 124..127 and then 0..4. */
    private static final LandingZone WRAPPING_PAD = new LandingZone(Terrain.VERTEX_COUNT - 4, SEGMENTS);

    /** Terrain with the landing zone at a known place (the package-private constructor). */
    private static Terrain terrain(long seed, LandingZone zone) {
        return new Terrain(seed, MIN_RADIUS, MAX_RADIUS, LANDING_RADIUS, zone);
    }

    /** Terrain as the game builds it: the landing zone is rolled from the seed. */
    private static Terrain randomSiteTerrain(long seed) {
        return new Terrain(seed, MIN_RADIUS, MAX_RADIUS, LANDING_RADIUS, SEGMENTS);
    }

    private static float[] radii(Terrain terrain) {
        float[] radii = new float[Terrain.VERTEX_COUNT];
        for (int i = 0; i < radii.length; i++) radii[i] = terrain.getRadius(i);
        return radii;
    }

    private static int countLandingVertices(Terrain terrain) {
        int count = 0;
        for (int i = 0; i < Terrain.VERTEX_COUNT; i++) {
            if (terrain.isLandingVertex(i)) count++;
        }
        return count;
    }

    @Test
    void landingZoneVerticesAreFlatAndIncludeBothEnds() {
        Terrain terrain = terrain(1L, FIRST_QUADRANT_PAD);
        for (int i = 0; i <= 8; i++) {
            assertTrue(terrain.isLandingVertex(i), "vertex " + i);
            assertEquals(LANDING_RADIUS, terrain.getRadius(i));
        }
        assertFalse(terrain.isLandingVertex(9));
        assertFalse(terrain.isLandingVertex(Terrain.VERTEX_COUNT - 1));
    }

    @Test
    void zoneWrappingPastVertexZeroFlattensBothEnds() {
        Terrain terrain = terrain(1L, WRAPPING_PAD);
        for (int i = 0; i < Terrain.VERTEX_COUNT; i++) {
            boolean expected = i >= 124 || i <= 4;
            assertEquals(expected, terrain.isLandingVertex(i), "vertex " + i);
            if (expected) assertEquals(LANDING_RADIUS, terrain.getRadius(i), "vertex " + i);
        }
    }

    @Test
    void randomSiteAlwaysFlattensExactlyOneZone() {
        for (long seed = 0; seed < 50; seed++) {
            Terrain terrain = randomSiteTerrain(seed);
            assertEquals(SEGMENTS + 1, countLandingVertices(terrain), "seed " + seed);
            assertEquals(SEGMENTS, terrain.getLandingZone().segmentCount());
        }
    }

    @Test
    void sameSeedGivesSameLandingSite() {
        assertEquals(randomSiteTerrain(42L).getLandingZone(), randomSiteTerrain(42L).getLandingZone());
    }

    @Test
    void differentSeedsSpreadTheLandingSiteAround() {
        // A Set keeps only distinct values, like a JS Set. With 20 seeds and 128 places,
        // a handful of repeats is fine, but all 20 landing in one place would mean the
        // site isn't really random.
        Set<Integer> firstVertices = new HashSet<>();
        for (long seed = 0; seed < 20; seed++) {
            firstVertices.add(randomSiteTerrain(seed).getLandingZone().firstVertex());
        }
        assertTrue(firstVertices.size() > 10, "only " + firstVertices.size() + " distinct sites");
    }

    /**
     * Rolling the site happens after the hills, so the random site doesn't change any
     * hill outside the zone: compare against the same seed with the zone placed by hand.
     */
    @Test
    void rollingTheSiteDoesNotChangeTheHills() {
        Terrain rolled = randomSiteTerrain(7L);
        Terrain placed = terrain(7L, rolled.getLandingZone());
        assertArrayEquals(radii(placed), radii(rolled));
    }

    @Test
    void sameSeedBuildsSameTerrain() {
        assertArrayEquals(radii(randomSiteTerrain(42L)), radii(randomSiteTerrain(42L)));
    }

    @Test
    void differentSeedsBuildDifferentTerrain() {
        assertFalse(Arrays.equals(radii(randomSiteTerrain(1L)), radii(randomSiteTerrain(2L))));
    }

    @Test
    void terrainStaysWithinItsHeightRange() {
        for (float radius : radii(randomSiteTerrain(3L))) {
            assertTrue(radius >= MIN_RADIUS && radius <= MAX_RADIUS, "radius " + radius);
        }
    }

    @Test
    void landingSiteIsMidSegmentInsideTheZone() {
        float angle = terrain(1L, FIRST_QUADRANT_PAD).landingSiteAngle();
        assertEquals(4.5f * Terrain.ANGLE_BETWEEN_VERTICES, angle, 1e-6f);
    }

    @Test
    void landingSiteOfAWrappingZoneIsWrappedIntoRange() {
        // First vertex 124 + 4 segments = segment 128, which is segment 0 once wrapped.
        float angle = terrain(1L, WRAPPING_PAD).landingSiteAngle();
        assertEquals(0.5f * Terrain.ANGLE_BETWEEN_VERTICES, angle, 1e-5f);
    }

    @Test
    void surfaceRadiusWrapsNegativeAngles() {
        Terrain terrain = terrain(5L, FIRST_QUADRANT_PAD);
        int last = Terrain.VERTEX_COUNT - 1;
        float lastVertexAngle = last * Terrain.ANGLE_BETWEEN_VERTICES;
        assertEquals(terrain.getRadius(last), terrain.surfaceRadiusAt(lastVertexAngle - MathUtils.PI2), 1e-3f);
        assertEquals(LANDING_RADIUS, terrain.surfaceRadiusAt(0.1f), 1e-4f);
    }
}
