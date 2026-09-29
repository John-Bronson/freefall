package com.johnbronson.freefall.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.johnbronson.freefall.world.GameWorld;

/**
 * Developer view, toggled with '.
 *
 * <p>{@link Box2DDebugRenderer} draws what Box2D believes exists: collision shapes,
 * body centers and contact points. Our own drawing can be wrong; this can't. It draws
 * raw (non-interpolated) positions, so it can trail the art by up to one physics step.
 * Asleep bodies are drawn in a different color from awake ones.
 */
public final class DebugOverlay implements Disposable {

    private static final float MARGIN = 10f;
    private static final float LINE_HEIGHT = 20f;
    private static final int CONTROLLER_BUTTONS_TO_SHOW = 16;

    private final Box2DDebugRenderer box2DRenderer = new Box2DDebugRenderer();
    private final ScreenViewport screenViewport = new ScreenViewport();
    private final BitmapFont font = new BitmapFont();
    private final SpriteBatch batch;

    public DebugOverlay(SpriteBatch batch) {
        this.batch = batch;
    }

    public void resize(int width, int height) {
        screenViewport.update(width, height, true);
    }

    public void render(GameWorld world, Viewport worldViewport, Controller controller) {
        // Box2D and the camera both use meters, so the camera matrix is passed as is.
        worldViewport.apply();
        box2DRenderer.render(world.getBox2DWorld(), worldViewport.getCamera().combined);

        screenViewport.apply();
        batch.setProjectionMatrix(screenViewport.getCamera().combined);
        batch.begin();
        float y = screenViewport.getWorldHeight() - MARGIN;
        font.draw(batch, "DEBUG  " + Gdx.graphics.getFramesPerSecond() + " fps", MARGIN, y);
        y -= LINE_HEIGHT;
        font.draw(batch, "Controllers found: " + Controllers.getControllers().size, MARGIN, y);
        if (controller != null) {
            y -= LINE_HEIGHT;
            font.draw(batch, describe(controller), MARGIN, y);
        }
        batch.end();
    }

    private static String describe(Controller controller) {
        StringBuilder text = new StringBuilder("Active: ").append(controller.getName()).append("  Buttons:");
        for (int i = 0; i < CONTROLLER_BUTTONS_TO_SHOW; i++) {
            if (controller.getButton(i)) text.append(' ').append(i);
        }
        text.append("  Axes:");
        for (int i = 0; i < controller.getAxisCount(); i++) {
            float value = controller.getAxis(i);
            if (Math.abs(value) > 0.1f) text.append(' ').append(i).append('=').append(String.format("%.2f", value));
        }
        return text.toString();
    }

    @Override
    public void dispose() {
        box2DRenderer.dispose();
        font.dispose();
    }
}
