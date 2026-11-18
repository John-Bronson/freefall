package com.johnbronson.freefall.world;

/**
 * A flat stretch of terrain, given as a run of terrain segments.
 *
 * <h2>Why vertices, not angles</h2>
 * The terrain is a loop of {@link Terrain#VERTEX_COUNT} vertices spaced evenly by angle
 * (see {@link Terrain}). Flattening works one vertex at a time, so a zone can only ever
 * start and end on a vertex. Storing the zone as vertex indices says that directly.
 * (An older version stored angles and rounded them to the nearest vertex, with a small
 * epsilon so that 22.5° landed on vertex 8 and not vertex 7.)
 *
 * <h2>Picture</h2>
 * A zone with {@code firstVertex = 3} and {@code segmentCount = 4}:
 * <pre>
 *   vertex:   2     3     4     5     6     7     8
 *             ·     ●─────●─────●─────●─────●     ·
 *                   └──────── 4 segments ─────┘
 *                   5 flat vertices: 3, 4, 5, 6, 7
 * </pre>
 * A segment is the straight line between two neighbouring vertices, so {@code n}
 * segments need {@code n + 1} vertices. That's the classic "fence posts and fence
 * panels" count.
 *
 * <h2>Wrapping past vertex 0</h2>
 * The terrain is a closed loop: after the last vertex (127) comes vertex 0 again.
 * A zone with {@code firstVertex = 124} and {@code segmentCount = 8} covers vertices
 * 124, 125, 126, 127, 0, 1, 2, 3, 4. Every method here handles that wrap.
 *
 * <h2>Java notes</h2>
 * A {@code record} is an immutable data class. Java writes the constructor, the
 * accessors ({@code zone.firstVertex()}, with no "get" prefix), {@code equals},
 * {@code hashCode} and {@code toString} for you. In TypeScript terms it's close to a
 * class with only {@code readonly} constructor parameters, plus value equality.
 *
 * @param firstVertex  index of the zone's first flat vertex, 0 to VERTEX_COUNT − 1
 * @param segmentCount how many flat segments the zone spans (at least 1)
 */
public record LandingZone(int firstVertex, int segmentCount) {

    /**
     * The "compact canonical constructor": it runs before the record's fields are
     * assigned, and here it's used only for validation. It has no parameter list, since
     * the parameters are the record components declared above.
     */
    public LandingZone {
        if (firstVertex < 0 || firstVertex >= Terrain.VERTEX_COUNT) {
            throw new IllegalArgumentException(
                "firstVertex must be 0.." + (Terrain.VERTEX_COUNT - 1) + ", was " + firstVertex);
        }
        // A zone covering every segment would make the whole planet flat. The limit is
        // VERTEX_COUNT - 1, so at least one vertex (with its rough slopes) stays outside.
        if (segmentCount < 1 || segmentCount >= Terrain.VERTEX_COUNT) {
            throw new IllegalArgumentException(
                "segmentCount must be 1.." + (Terrain.VERTEX_COUNT - 1) + ", was " + segmentCount);
        }
    }

    /** How many vertices the zone flattens: one more than the segments (fence posts). */
    public int vertexCount() {
        return segmentCount + 1;
    }

    /**
     * True if vertex {@code index} is one of the zone's flat vertices.
     *
     * <p>The trick: measure how far {@code index} is <em>past</em> {@code firstVertex},
     * going round the loop, and check that distance is at most {@code segmentCount}.
     * For the wrapping zone (first 124, 8 segments):
     * <ul>
     *   <li>index 126 → 126 − 124 = 2 steps past the start → inside</li>
     *   <li>index 2 → 2 − 124 = −122, which is 6 steps once wrapped (−122 + 128) → inside</li>
     *   <li>index 10 → 10 − 124 = −114, which is 14 steps once wrapped → outside</li>
     * </ul>
     *
     * <p>Why {@link Math#floorMod} and not {@code %}: in Java (and in JavaScript),
     * {@code %} keeps the sign of the left side, so {@code -122 % 128} is {@code -122}.
     * {@code Math.floorMod(-122, 128)} gives {@code 6}, which is the "steps round the
     * loop" number we want. JavaScript has no built-in for this; you'd write
     * {@code ((a % n) + n) % n}.
     */
    public boolean containsVertex(int index) {
        int stepsPastFirst = Math.floorMod(index - firstVertex, Terrain.VERTEX_COUNT);
        return stepsPastFirst <= segmentCount;
    }
}
