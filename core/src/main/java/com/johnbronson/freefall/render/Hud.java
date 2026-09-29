package com.johnbronson.freefall.render;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.johnbronson.freefall.world.GameWorld;
import com.johnbronson.freefall.world.Planet;
import com.johnbronson.freefall.world.Ship;

/**
 * Flight instruments, drawn in screen pixels. The HUD has its own viewport, so text
 * stays the same size however far the world camera zooms.
 */
public final class Hud implements Disposable {

    private static final float MARGIN = 16f;
    private static final float LINE_HEIGHT = 20f;

    private final ScreenViewport viewport = new ScreenViewport();
    private final BitmapFont font = new BitmapFont();
    private final SpriteBatch batch;

    public Hud(SpriteBatch batch) {
        this.batch = batch;
    }

    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    public void render(GameWorld world) {
        Ship ship = world.getShip();
        Vector2 position = ship.getBody().getPosition();
        Planet nearest = world.nearestPlanetTo(position);
        // Clamped: a flat segment dips slightly below the curve through its vertices.
        float altitude = Math.max(0f, nearest.altitudeOf(position) - Ship.HULL_RADIUS);

        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();

        float y = viewport.getWorldHeight() - MARGIN;
        y = line(ship.getState().name(), y);
        y = line(String.format("Fuel %.0f%%", 100f * ship.getMainFuel() / Ship.MAIN_FUEL_CAPACITY), y);
        y = line(ship.isBoosterBurning() ? "Booster BURNING" : "Booster x" + ship.getBoosterCharges(), y);
        y = line(String.format("Speed %.1f m/s", ship.getBody().getLinearVelocity().len()), y);
        y = line(String.format("Altitude %.1f m", altitude), y);

        if (ship.getState() == Ship.FlightState.CRASHED) {
            y = line(ship.getCrashReason(), y);
            line("Press R to restart", y);
        }

        batch.end();
    }

    /** Draws one right-aligned line and returns the y for the next one. */
    private float line(String text, float y) {
        font.draw(batch, text, MARGIN, y, viewport.getWorldWidth() - 2f * MARGIN, Align.right, false);
        return y - LINE_HEIGHT;
    }

    @Override
    public void dispose() {
        font.dispose();
    }
}
