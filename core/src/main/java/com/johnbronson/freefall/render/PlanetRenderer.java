package com.johnbronson.freefall.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Vector2;
import com.johnbronson.freefall.world.Planet;
import com.johnbronson.freefall.world.Terrain;

/** Placeholder planet art: atmosphere disc, terrain fan and landing-zone stripes. */
final class PlanetRenderer {

    private static final Color ATMOSPHERE_COLOR = new Color(0.5f, 0.5f, 1f, 0.3f);
    private static final Color GROUND_COLOR = new Color(Color.GREEN);
    private static final Color LANDING_ZONE_COLOR = new Color(Color.YELLOW);

    private static final int ATMOSPHERE_SEGMENTS = 96;
    /** Draw landing stripes this far above the ground (meters), so the terrain edge doesn't cover them. */
    private static final float LANDING_STRIPE_LIFT = 0.1f;

    private final Vector2 vertex = new Vector2();
    private final Vector2 nextVertex = new Vector2();

    void draw(ShapeRenderer shapes, Planet planet) {
        Vector2 center = planet.getCenter();
        Terrain terrain = planet.getTerrain();

        shapes.set(ShapeType.Filled);
        shapes.setColor(ATMOSPHERE_COLOR);
        shapes.circle(center.x, center.y, Planet.ATMOSPHERE_RADIUS, ATMOSPHERE_SEGMENTS);

        // One triangle from the center to each pair of neighbouring surface vertices.
        // These are the same vertices as the Box2D chain.
        shapes.setColor(GROUND_COLOR);
        for (int i = 0; i < Terrain.VERTEX_COUNT; i++) {
            int next = (i + 1) % Terrain.VERTEX_COUNT;
            terrain.getVertex(i, vertex).add(center);
            terrain.getVertex(next, nextVertex).add(center);
            shapes.triangle(center.x, center.y, vertex.x, vertex.y, nextVertex.x, nextVertex.y);
        }

        shapes.set(ShapeType.Line);
        shapes.setColor(LANDING_ZONE_COLOR);
        for (int i = 0; i < Terrain.VERTEX_COUNT; i++) {
            int next = (i + 1) % Terrain.VERTEX_COUNT;
            if (!terrain.isLandingVertex(i) || !terrain.isLandingVertex(next)) continue;

            liftedVertex(terrain, i, vertex).add(center);
            liftedVertex(terrain, next, nextVertex).add(center);
            shapes.line(vertex, nextVertex);
        }
    }

    private static Vector2 liftedVertex(Terrain terrain, int index, Vector2 out) {
        terrain.getVertex(index, out);
        return out.setLength(terrain.getRadius(index) + LANDING_STRIPE_LIFT);
    }
}
