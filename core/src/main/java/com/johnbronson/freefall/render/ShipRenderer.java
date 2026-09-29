package com.johnbronson.freefall.render;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Vector2;
import com.johnbronson.freefall.world.Ship;

/**
 * Placeholder ship art: an outlined triangle with an engine flame.
 *
 * <p>Shapes are defined in the ship's local frame (meters, +Y = nose) and moved into
 * place by one transform, the same way a sprite is drawn with a position, origin and
 * rotation. The art is not the collision shape. Press ' to see Box2D's actual circle.
 */
final class ShipRenderer {

    // The triangle's base is level with the bottom of the physics circle, so the
    // ship doesn't appear to hover when it rests on the ground.
    private static final float NOSE_Y = 0.9f;
    private static final float BASE_Y = -Ship.HULL_RADIUS;
    private static final float HALF_WIDTH = 0.6f;

    private static final float FLAME_HALF_WIDTH = 0.25f;
    private static final float MAIN_FLAME_LENGTH = 1.5f;
    private static final float BOOSTER_FLAME_LENGTH = 2.5f;

    private static final Color HULL_COLOR = new Color(Color.WHITE);
    private static final Color CRASHED_COLOR = new Color(Color.RED);
    private static final Color MAIN_FLAME_COLOR = new Color(Color.ORANGE);
    private static final Color BOOSTER_FLAME_COLOR = new Color(1f, 0.95f, 0.6f, 1f);

    private final Affine2 localToWorld = new Affine2();
    private final Vector2 a = new Vector2();
    private final Vector2 b = new Vector2();
    private final Vector2 c = new Vector2();

    void draw(ShapeRenderer shapes, Ship ship, float alpha) {
        Vector2 position = ship.getHull().getPosition(alpha);
        localToWorld.setToTrnRotRadScl(position.x, position.y, ship.getHull().getAngle(alpha), 1f, 1f);

        shapes.set(ShapeType.Filled);
        if (ship.isBoosterBurning()) {
            shapes.setColor(BOOSTER_FLAME_COLOR);
            drawFlame(shapes, BOOSTER_FLAME_LENGTH);
        } else if (ship.getMainEngineOutput() > 0f) {
            shapes.setColor(MAIN_FLAME_COLOR);
            drawFlame(shapes, MAIN_FLAME_LENGTH * ship.getMainEngineOutput());
        }

        shapes.set(ShapeType.Line);
        shapes.setColor(ship.getState() == Ship.FlightState.CRASHED ? CRASHED_COLOR : HULL_COLOR);
        drawLocalTriangle(shapes, 0f, NOSE_Y, -HALF_WIDTH, BASE_Y, HALF_WIDTH, BASE_Y);
    }

    private void drawFlame(ShapeRenderer shapes, float length) {
        drawLocalTriangle(shapes, -FLAME_HALF_WIDTH, BASE_Y, FLAME_HALF_WIDTH, BASE_Y, 0f, BASE_Y - length);
    }

    private void drawLocalTriangle(ShapeRenderer shapes, float x1, float y1, float x2, float y2, float x3, float y3) {
        localToWorld.applyTo(a.set(x1, y1));
        localToWorld.applyTo(b.set(x2, y2));
        localToWorld.applyTo(c.set(x3, y3));
        shapes.triangle(a.x, a.y, b.x, b.y, c.x, c.y);
    }
}
