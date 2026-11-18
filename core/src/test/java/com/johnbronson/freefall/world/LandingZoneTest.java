package com.johnbronson.freefall.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LandingZoneTest {

    @Test
    void containsBothEndVerticesAndNothingOutside() {
        LandingZone zone = new LandingZone(10, 8);
        assertFalse(zone.containsVertex(9));
        assertTrue(zone.containsVertex(10));
        assertTrue(zone.containsVertex(18));
        assertFalse(zone.containsVertex(19));
        assertEquals(9, zone.vertexCount());
    }

    @Test
    void wrapsPastTheLastVertex() {
        LandingZone zone = new LandingZone(124, 8);
        assertFalse(zone.containsVertex(123));
        assertTrue(zone.containsVertex(127));
        assertTrue(zone.containsVertex(0));
        assertTrue(zone.containsVertex(4));
        assertFalse(zone.containsVertex(5));
    }

    @Test
    void rejectsOutOfRangeValues() {
        // assertThrows runs the lambda and passes only if it throws that exception type,
        // like Jest's expect(() => ...).toThrow().
        assertThrows(IllegalArgumentException.class, () -> new LandingZone(-1, 8));
        assertThrows(IllegalArgumentException.class, () -> new LandingZone(Terrain.VERTEX_COUNT, 8));
        assertThrows(IllegalArgumentException.class, () -> new LandingZone(0, 0));
        assertThrows(IllegalArgumentException.class, () -> new LandingZone(0, Terrain.VERTEX_COUNT));
    }
}
