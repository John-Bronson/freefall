package com.johnbronson.freefall.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.ChainShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.World;
import com.johnbronson.freefall.physics.CollisionCategory;
import com.johnbronson.freefall.physics.FixtureRole;

/**
 * A planetoid: a static Box2D body whose single fixture is a closed chain of terrain
 * segments. It also carries the numbers that {@link PlanetaryGravity} needs.
 *
 * <p>A planet only holds physics and data. Drawing it is the renderer's job.
 */
public final class Planet {

    // All sizes are meters from the planet's center.
    public static final float MIN_TERRAIN_RADIUS = 60f;
    public static final float MAX_TERRAIN_RADIUS = 78f;
    public static final float LANDING_ZONE_RADIUS = 69f;
    public static final float ATMOSPHERE_RADIUS = 108f;

    /**
     * Gravitational acceleration at and below {@link #GRAVITY_PLATEAU_RADIUS}, so everywhere
     * near the ground, in m/s².
     */
    public static final float SURFACE_GRAVITY = 18f;

    /**
     * Below this distance from the center, gravity stops increasing and holds at
     * {@link #SURFACE_GRAVITY}. Above it, gravity falls off with the square of the distance.
     */
    public static final float GRAVITY_PLATEAU_RADIUS = 88.5f;

    /**
     * Air density at and below the landing zones.
     * A relative scale: 1 is the reference atmosphere that ship drag is tuned against.
     * Raise it for a thicker atmosphere, lower it for a thinner one.
     */
    public static final float SURFACE_AIR_DENSITY = 1f;

    /**
     * Shape of the air density curve. 1 = linear. Higher values keep the upper air thin
     * for longer, then thicken it quickly near the surface.
     */
    public static final float AIR_DENSITY_FALLOFF_EXPONENT = 1f;

    /** Rock is grippy. Box2D mixes two fixtures' friction as sqrt(a * b). */
    private static final float TERRAIN_FRICTION = 0.8f;

    private static final LandingZone[] LANDING_ZONES = {
        LandingZone.ofDegrees(0f, 22.5f),
        LandingZone.ofDegrees(120f, 142.5f),
        LandingZone.ofDegrees(240f, 262.5f),
    };

    private final Vector2 center;
    private final Terrain terrain;
    private final Body body;

    public Planet(World world, float centerX, float centerY, long terrainSeed) {
        this.center = new Vector2(centerX, centerY);
        this.terrain = new Terrain(terrainSeed, MIN_TERRAIN_RADIUS, MAX_TERRAIN_RADIUS,
            LANDING_ZONE_RADIUS, LANDING_ZONES);
        this.body = createBody(world);
    }

    private Body createBody(World world) {
        // A static body never moves and acts as if infinitely heavy. The solver skips
        // it when moving things, which makes it the cheapest body type.
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.StaticBody;
        bodyDef.position.set(center);

        Body planetBody = world.createBody(bodyDef);
        planetBody.setUserData(this);

        // A chain is hollow: only the line collides, not the inside. Convex polygons
        // are limited to 8 vertices, and bumpy terrain is not convex. A chain has
        // neither limit, and it joins its segments smoothly, so sliding objects don't
        // catch on the seams.
        ChainShape surface = new ChainShape();
        surface.createLoop(terrainVertices());

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = surface;
        fixtureDef.friction = TERRAIN_FRICTION;
        fixtureDef.filter.categoryBits = CollisionCategory.TERRAIN;
        fixtureDef.filter.maskBits = CollisionCategory.SHIP;

        planetBody.createFixture(fixtureDef).setUserData(FixtureRole.TERRAIN);

        // createFixture copied the shape into Box2D's native memory, so our copy can go.
        surface.dispose();
        return planetBody;
    }

    /**
     * Shape vertices are in the body's local space. The body is already placed at the
     * planet's center, so the vertices are relative to it.
     */
    private Vector2[] terrainVertices() {
        Vector2[] vertices = new Vector2[Terrain.VERTEX_COUNT];
        for (int i = 0; i < vertices.length; i++) {
            vertices[i] = terrain.getVertex(i, new Vector2());
        }
        return vertices;
    }

    /**
     * The "GM" in Newton's a = GM / r². Working back from the surface gravity makes
     * gravity easy to tune: a = {@link #SURFACE_GRAVITY} exactly at
     * {@link #GRAVITY_PLATEAU_RADIUS}, where the inverse-square curve meets the plateau.
     */
    public float getGravitationalParameter() {
        return SURFACE_GRAVITY * GRAVITY_PLATEAU_RADIUS * GRAVITY_PLATEAU_RADIUS;
    }

    /** Angle from the planet's center to the middle of a landing zone, in radians. */
    public float getLandingSiteAngle(int zoneIndex) {
        return terrain.landingSiteAngle(LANDING_ZONES[zoneIndex]);
    }

    /** World position {@code height} meters straight above the middle of a landing zone. */
    public Vector2 getLandingSite(int zoneIndex, float height, Vector2 out) {
        float angle = getLandingSiteAngle(zoneIndex);
        // The site is the midpoint of a straight segment between two vertices. That
        // midpoint is slightly closer to the center than the vertices are.
        float surfaceRadius = LANDING_ZONE_RADIUS * MathUtils.cos(Terrain.ANGLE_BETWEEN_VERTICES / 2f);
        return out.set(MathUtils.cos(angle), MathUtils.sin(angle)).scl(surfaceRadius + height).add(center);
    }

    /** True if {@code worldPoint} is inside the planet's atmosphere */
    public boolean isInAtmosphere(Vector2 worldPoint) {
        return getCenter().dst(worldPoint) < ATMOSPHERE_RADIUS;
    }

    /**
     * Relative air density at {@code worldPoint}: 0 outside the atmosphere, rising to
     * {@link #SURFACE_AIR_DENSITY} at the landing zones.
     */
    public float airDensityAt(Vector2 worldPoint) {
        float distance = center.dst(worldPoint);
        float t = (ATMOSPHERE_RADIUS - distance) / (ATMOSPHERE_RADIUS - LANDING_ZONE_RADIUS);
        t = MathUtils.clamp(t, 0f, 1f);
        return SURFACE_AIR_DENSITY * (float) Math.pow(t, AIR_DENSITY_FALLOFF_EXPONENT);
    }

    /** Height of {@code worldPoint} above the terrain directly below it, in meters. */
    public float altitudeOf(Vector2 worldPoint) {
        float dx = worldPoint.x - center.x;
        float dy = worldPoint.y - center.y;
        float angle = (float) Math.atan2(dy, dx);
        return (float) Math.sqrt(dx * dx + dy * dy) - terrain.surfaceRadiusAt(angle);
    }

    /** The planet's own vector, not a copy. Treat it as read-only. */
    public Vector2 getCenter() {
        return center;
    }

    public Terrain getTerrain() {
        return terrain;
    }

    public Body getBody() {
        return body;
    }
}
