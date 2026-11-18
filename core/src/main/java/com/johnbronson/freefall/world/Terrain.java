package com.johnbronson.freefall.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;

/**
 * The shape of a planet's surface. The surface is a closed loop of
 * {@link #VERTEX_COUNT} vertices spaced evenly by angle. Each vertex stores its
 * distance from the planet's center, in meters.
 *
 * <p>Each planet has exactly one {@link LandingZone}: a run of vertices flattened to
 * the same radius. Both the hills and the zone's position come from the seed, so the
 * same seed always builds the same surface. A Terrain never changes after it's built:
 * a restart (R) throws the whole planet away and builds a new one from a new seed
 * (see {@link GameWorld}).
 *
 * <p>This class is plain data with no Box2D in it. {@link Planet} turns it into a
 * {@code ChainShape} for collisions, and the renderer draws the same vertices, so what
 * you see is exactly what you collide with.
 */
public final class Terrain {

    public static final int VERTEX_COUNT = 128;
    public static final float ANGLE_BETWEEN_VERTICES = MathUtils.PI2 / VERTEX_COUNT;

    private final float[] radii = new float[VERTEX_COUNT];
    private final LandingZone landingZone;

    /**
     * Builds the hills, then puts the landing zone at a random vertex. This is the one
     * the game uses.
     *
     * @param seed                same seed, same hills and same landing site. Keeps
     *                            tests repeatable.
     * @param minRadius           lowest valley, meters from the center
     * @param maxRadius           highest peak, meters from the center
     * @param landingRadius       height of the landing zone, meters from the center
     * @param landingZoneSegments how wide the landing zone is, in terrain segments
     */
    public Terrain(long seed, float minRadius, float maxRadius, float landingRadius, int landingZoneSegments) {
        RandomXS128 random = new RandomXS128(seed);
        fillWithHills(random, minRadius, maxRadius);

        // One more number from the same generator: the zone's first vertex.
        // nextInt(VERTEX_COUNT) returns 0..127, every vertex equally likely.
        //
        // It's drawn AFTER the hills on purpose. A seeded generator always produces the
        // same sequence of numbers; drawing the site first would shift every height
        // along by one, so each seed would get different hills than it used to.
        landingZone = new LandingZone(random.nextInt(VERTEX_COUNT), landingZoneSegments);
        flatten(landingZone, landingRadius);
    }

    /**
     * Builds the hills with the landing zone exactly where the caller says. For tests,
     * which need to know where the zone is (for example, one that wraps past vertex 0).
     *
     * <p>No {@code public}: "package-private" in Java means only classes in the same
     * package ({@code com.johnbronson.freefall.world}) can call it. Tests live in the
     * same package (under {@code src/test}), so they can; the renderer can't.
     */
    Terrain(long seed, float minRadius, float maxRadius, float landingRadius, LandingZone landingZone) {
        fillWithHills(new RandomXS128(seed), minRadius, maxRadius);
        this.landingZone = landingZone;
        flatten(landingZone, landingRadius);
    }

    /** Rough terrain everywhere: each vertex gets a random height in the range. */
    private void fillWithHills(RandomXS128 random, float minRadius, float maxRadius) {
        for (int i = 0; i < VERTEX_COUNT; i++) {
            radii[i] = MathUtils.lerp(minRadius, maxRadius, random.nextFloat());
        }
    }

    /**
     * Sets every vertex of the zone to the same radius. Walk from the first vertex for
     * vertexCount() steps; {@code % VERTEX_COUNT} wraps 128 back to 0, so a zone that
     * starts at 124 carries on through 127, 0, 1, ... without any special case.
     */
    private void flatten(LandingZone zone, float landingRadius) {
        for (int step = 0; step < zone.vertexCount(); step++) {
            radii[(zone.firstVertex() + step) % VERTEX_COUNT] = landingRadius;
        }
    }

    /** Position of vertex {@code index}, relative to the planet's center. */
    public Vector2 getVertex(int index, Vector2 out) {
        float angle = index * ANGLE_BETWEEN_VERTICES;
        return out.set(MathUtils.cos(angle), MathUtils.sin(angle)).scl(radii[index]);
    }

    public float getRadius(int index) {
        return radii[index];
    }

    public LandingZone getLandingZone() {
        return landingZone;
    }

    /** True if this vertex was flattened into the landing zone. */
    public boolean isLandingVertex(int index) {
        return landingZone.containsVertex(index);
    }

    /** Surface height at any angle, linearly interpolated between the two nearest vertices. */
    public float surfaceRadiusAt(float angle) {
        float position = normalize(angle) / ANGLE_BETWEEN_VERTICES;
        int index = (int) position;
        float t = position - index;
        return MathUtils.lerp(radii[index % VERTEX_COUNT], radii[(index + 1) % VERTEX_COUNT], t);
    }

    /**
     * The best place to set a ship down in the zone: the middle of its central segment,
     * as an angle in 0..2π.
     *
     * <p>Why a segment and not a vertex: a vertex is a small ridge between two segments
     * that slope away slightly (the zone is flat in radius, but the planet is round).
     * A ship left on a ridge would roll off it.
     *
     * <p>Worked example, 8 segments starting at vertex 0: the segments are 0..7, so
     * the central one is 0 + 8 / 2 = segment 4, which runs from vertex 4 to vertex 5.
     * Its middle is at 4.5 vertex spacings. With an odd count, say 7, integer division
     * gives 7 / 2 = 3, which is exactly the middle one of segments 0..6.
     *
     * <p>{@code normalize} matters for a wrapping zone: starting at 126, the central
     * segment is 130, past the end of the loop. Normalizing turns its angle back into
     * the one for segment 2.
     */
    public float landingSiteAngle() {
        int middleSegment = landingZone.firstVertex() + landingZone.segmentCount() / 2;
        return normalize((middleSegment + 0.5f) * ANGLE_BETWEEN_VERTICES);
    }

    /** Wraps any angle into 0..2π. {@code %} alone would leave negative angles negative. */
    private static float normalize(float angle) {
        float wrapped = angle % MathUtils.PI2;
        return wrapped < 0f ? wrapped + MathUtils.PI2 : wrapped;
    }
}
