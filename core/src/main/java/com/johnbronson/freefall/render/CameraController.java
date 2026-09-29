package com.johnbronson.freefall.render;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.johnbronson.freefall.world.GameWorld;
import com.johnbronson.freefall.world.Planet;

/**
 * Moves the world camera. Two modes:
 * <ul>
 *   <li>Following (default): centers on the ship, zooms in inside an atmosphere and
 *       out in open space.</li>
 *   <li>Free: manual pan and zoom, for looking around.</li>
 * </ul>
 * Camera zoom is a multiplier: 2 shows twice as many meters as 1.
 */
public final class CameraController {

    private static final float APPROACH_ZOOM = 1f;
    private static final float CRUISE_ZOOM = 4f;
    /** How quickly zoom eases toward its target. Higher is snappier. */
    private static final float ZOOM_RESPONSE = 2f;

    private static final float MIN_ZOOM = 0.25f;
    private static final float MAX_ZOOM = 20f;
    private static final float MANUAL_ZOOM_STEP = 0.1f;
    /** Meters per pan press at zoom 1. Scales with zoom so panning feels the same at any zoom. */
    private static final float MANUAL_PAN_STEP = 10f;

    private final OrthographicCamera camera;
    private boolean following = true;

    public CameraController(OrthographicCamera camera) {
        this.camera = camera;
        camera.zoom = APPROACH_ZOOM;
    }

    public void update(GameWorld world, float frameDelta) {
        if (!following) return;

        // Follow the interpolated position, the same one the ship is drawn at, or the
        // ship would jitter against the camera.
        Vector2 shipPosition = world.getShip().getHull().getPosition(world.getAlpha());
        camera.position.set(shipPosition, 0f);

        Planet nearest = world.nearestPlanetTo(shipPosition);
        boolean inAtmosphere = nearest.getCenter().dst(shipPosition) < Planet.ATMOSPHERE_RADIUS;
        float targetZoom = inAtmosphere ? APPROACH_ZOOM : CRUISE_ZOOM;
        camera.zoom = MathUtils.lerp(camera.zoom, targetZoom, Math.min(1f, ZOOM_RESPONSE * frameDelta));
    }

    public void toggleFollowing() {
        following = !following;
    }

    /** Free mode only. Directions are −1, 0 or 1. */
    public void pan(float directionX, float directionY) {
        if (following) return;
        float step = MANUAL_PAN_STEP * camera.zoom;
        camera.translate(directionX * step, directionY * step);
    }

    /** Free mode only. */
    public void zoomIn() {
        if (following) return;
        camera.zoom = MathUtils.clamp(camera.zoom - MANUAL_ZOOM_STEP, MIN_ZOOM, MAX_ZOOM);
    }

    /** Free mode only. */
    public void zoomOut() {
        if (following) return;
        camera.zoom = MathUtils.clamp(camera.zoom + MANUAL_ZOOM_STEP, MIN_ZOOM, MAX_ZOOM);
    }
}
