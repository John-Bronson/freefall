package com.johnbronson.freefall.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;

/**
 * The shape of a planet's surface. The surface is a closed loop of
 * {@link #VERTEX_COUNT} vertices spaced evenly by angle. Each vertex stores its
 * distance from the planet's center, in meters.
 *
 * <p>This class is plain data with no Box2D in it. {@link Planet} turns it into a
 * {@code ChainShape} for collisions, and the renderer draws the same vertices, so what
 * you see is exactly what you collide with.
 */
public final class Terrain {

    public static final int VERTEX_COUNT = 128;
    public static final float ANGLE_BETWEEN_VERTICES = MathUtils.PI2 / VERTEX_COUNT;

    /** Floating-point slack so a zone ending at exactly 22.5° includes the vertex at 22.5°. */
    private static final float ANGLE_EPSILON = 1e-4f;

    private final float[] radii = new float[VERTEX_COUNT];
    private final boolean[] landingVertex = new boolean[VERTEX_COUNT];

    /**
     * @param seed          same seed, same terrain. Keeps restarts and tests repeatable.
     * @param minRadius     lowest valley, meters from the center
     * @param maxRadius     highest peak, meters from the center
     * @param landingRadius height of every landing zone, meters from the center
     */
    public Terrain(long seed, float minRadius, float maxRadius, float landingRadius, LandingZone... landingZones) {
        RandomXS128 random = new RandomXS128(seed);
        for (int i = 0; i < VERTEX_COUNT; i++) {
            radii[i] = MathUtils.lerp(minRadius, maxRadius, random.nextFloat());
        }

        for (LandingZone zone : landingZones) {
            for (int i = firstVertexOf(zone); i <= lastVertexOf(zone); i++) {
                radii[i % VERTEX_COUNT] = landingRadius;
                landingVertex[i % VERTEX_COUNT] = true;
            }
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

    /** True if this vertex was flattened into a landing zone. */
    public boolean isLandingVertex(int index) {
        return landingVertex[index];
    }

    /** Surface height at any angle, linearly interpolated between the two nearest vertices. */
    public float surfaceRadiusAt(float angle) {
        float position = normalize(angle) / ANGLE_BETWEEN_VERTICES;
        int index = (int) position;
        float t = position - index;
        return MathUtils.lerp(radii[index % VERTEX_COUNT], radii[(index + 1) % VERTEX_COUNT], t);
    }

    /**
     * The best place to set a ship down in a zone: the middle of its central segment.
     * A vertex would be a small ridge between two sloped segments, and a ship left
     * there would roll off it.
     */
    public float landingSiteAngle(LandingZone zone) {
        int middleSegment = (firstVertexOf(zone) + lastVertexOf(zone)) / 2;
        return (middleSegment + 0.5f) * ANGLE_BETWEEN_VERTICES;
    }

    private static int firstVertexOf(LandingZone zone) {
        return MathUtils.ceil(zone.startAngle() / ANGLE_BETWEEN_VERTICES - ANGLE_EPSILON);
    }

    private static int lastVertexOf(LandingZone zone) {
        return MathUtils.floor(zone.endAngle() / ANGLE_BETWEEN_VERTICES + ANGLE_EPSILON);
    }

    private static float normalize(float angle) {
        float wrapped = angle % MathUtils.PI2;
        return wrapped < 0f ? wrapped + MathUtils.PI2 : wrapped;
    }
}
