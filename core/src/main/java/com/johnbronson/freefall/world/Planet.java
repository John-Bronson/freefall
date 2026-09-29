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

    /** Gravitational acceleration at landing-zone height, in m/s². */
    public static final float SURFACE_GRAVITY = 18f;

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
     * gravity easy to tune: a = SURFACE_GRAVITY exactly at landing-zone height.
     */
    public float getGravitationalParameter() {
        return SURFACE_GRAVITY * LANDING_ZONE_RADIUS * LANDING_ZONE_RADIUS;
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
