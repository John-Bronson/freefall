package com.johnbronson.freefall.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.johnbronson.freefall.world.GameWorld;
import com.johnbronson.freefall.world.Planet;
import com.johnbronson.freefall.world.Ship;
import com.johnbronson.freefall.world.TrajectoryPredictor;

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
    private final TrajectoryRenderer trajectoryRenderer = new TrajectoryRenderer();

    public WorldRenderer() {
        // Let each renderer switch between filled and outline shapes inside one begin/end.
        shapes.setAutoShapeType(true);
    }

    public void resize(int width, int height) {
        // Don't re-center: CameraController decides where the camera looks.
        viewport.update(width, height);
    }

    /**
     * @param frameDelta real seconds since the last frame, for animations that belong
     *                   to the view (the predictor line growing and shrinking).
     */
    public void render(GameWorld world, float frameDelta) {
        trajectoryRenderer.update(frameDelta);

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
        // Before the ship, so the ship is drawn on top of the line. Like layers in a
        // paint program, whatever is drawn later covers what was drawn earlier.
        if (trajectoryRenderer.isActive()) {
            drawTrajectory(world);
        }
        shipRenderer.draw(shapes, world.getShip(), world.getAlpha());
        shapes.end();
    }

    private void drawTrajectory(GameWorld world) {
        // Only ask for a prediction while the line is on screen, so it costs nothing
        // when hidden. The renderer asking the world to compute something is fine: it
        // only reads the result, much like an Angular component calling a service.
        TrajectoryPredictor prediction = world.predictTrajectory();

        // Why an offset? Physics runs in fixed steps, but frames don't line up with
        // them. So the ship is drawn part-way between its previous and current physics
        // positions:
        //   drawn = previous + (current − previous) × alpha,  with alpha from 0 to 1
        // (see InterpolatedBody). The prediction starts at `current`, the raw physics
        // position, which can be up to one step ahead of the drawn ship. Moving every
        // point by (drawn − current) makes the line start right at the drawn ship.
        //
        // Both getters return vectors that Box2D/InterpolatedBody reuse, so we read
        // the numbers out straight away and don't hold on to the vectors.
        Ship ship = world.getShip();
        Vector2 drawn = ship.getHull().getPosition(world.getAlpha());
        Vector2 raw = ship.getBody().getPosition();
        float offsetX = drawn.x - raw.x;
        float offsetY = drawn.y - raw.y;

        trajectoryRenderer.draw(shapes, prediction, offsetX, offsetY, camera.zoom);
    }

    /**
     * Plays one cycle of the predicted coast line: the dots draw out to the end, then
     * clear out to the end. Pressing again mid-way cancels or queues another cycle.
     * See {@link TrajectoryRenderer}.
     */
    public void toggleTrajectory() {
        trajectoryRenderer.toggle();
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
