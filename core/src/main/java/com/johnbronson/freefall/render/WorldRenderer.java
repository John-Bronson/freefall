package com.johnbronson.freefall.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.johnbronson.freefall.world.GameWorld;
import com.johnbronson.freefall.world.Planet;

/**
 * Draws the game world. The camera works in meters, the same unit as Box2D, so
 * physics positions go straight to the screen with no conversion factor.
 *
 * <p>Pixel art slots in here, not in the physics code. A sprite's size in meters is
 * its texture size in pixels divided by the art's pixels-per-meter. Swap
 * {@link PlanetRenderer} and {@link ShipRenderer} for sprite-based versions and nothing
 * in {@code world} or {@code physics} changes.
 */
public final class WorldRenderer implements Disposable {

    /**
     * Smallest area shown at zoom 1, in meters. ExtendViewport keeps the aspect ratio
     * and shows extra world on wider or taller windows instead of stretching.
     */
    private static final float MIN_VIEW_WIDTH = 80f;
    private static final float MIN_VIEW_HEIGHT = 60f;

    private final OrthographicCamera camera = new OrthographicCamera();
    private final Viewport viewport = new ExtendViewport(MIN_VIEW_WIDTH, MIN_VIEW_HEIGHT, camera);
    private final ShapeRenderer shapes = new ShapeRenderer();
    private final PlanetRenderer planetRenderer = new PlanetRenderer();
    private final ShipRenderer shipRenderer = new ShipRenderer();

    public WorldRenderer() {
        // Let each renderer switch between filled and outline shapes inside one begin/end.
        shapes.setAutoShapeType(true);
    }

    public void resize(int width, int height) {
        // Don't re-center: CameraController decides where the camera looks.
        viewport.update(width, height);
    }

    public void render(GameWorld world) {
        viewport.apply();
        camera.update();
        shapes.setProjectionMatrix(camera.combined);

        // Needed for the translucent atmosphere.
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapes.begin();
        for (Planet planet : world.getPlanets()) {
            planetRenderer.draw(shapes, planet);
        }
        shipRenderer.draw(shapes, world.getShip(), world.getAlpha());
        shapes.end();
    }

    public OrthographicCamera getCamera() {
        return camera;
    }

    public Viewport getViewport() {
        return viewport;
    }

    @Override
    public void dispose() {
        shapes.dispose();
    }
}
