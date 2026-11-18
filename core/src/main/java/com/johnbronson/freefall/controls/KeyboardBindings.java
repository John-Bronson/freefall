package com.johnbronson.freefall.controls;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;

/**
 * Keyboard layout. Held keys (thrust, rotate) use {@code isKeyPressed}, which is true on
 * every frame the key is down. One-shot actions use {@code isKeyJustPressed}, which is
 * true only on the frame the key goes down; with {@code isKeyPressed}, holding R would
 * restart the game every frame.
 */
public final class KeyboardBindings implements InputBinding {

    @Override
    public boolean isPressed(InputAction action) {
        // A switch *expression* (Java 14+): each `case X -> value` yields a value, with no
        // `break` or fall-through to worry about. The compiler checks that every enum
        // value is covered, so adding an InputAction without a key here won't compile.
        // TypeScript has no direct equivalent; think of an object map lookup that the
        // compiler checks for completeness.
        return switch (action) {
            case ROTATE_LEFT -> held(Keys.LEFT);
            case ROTATE_RIGHT -> held(Keys.RIGHT);
            case THRUST_SOLID -> tapped(Keys.SPACE);
            case PAN_CAM_LEFT -> tapped(Keys.A);
            case PAN_CAM_RIGHT -> tapped(Keys.D);
            case PAN_CAM_UP -> tapped(Keys.W);
            case PAN_CAM_DOWN -> tapped(Keys.S);
            // =/- are the keys marked +/− on most keyboards.
            case ZOOM_IN -> tapped(Keys.EQUALS);
            case ZOOM_OUT -> tapped(Keys.MINUS);
            case TOGGLE_PREDICTOR -> tapped(Keys.Z);
            case TOGGLE_CAMERA_LOCK -> tapped(Keys.L);
            case TOGGLE_DEBUG -> tapped(Keys.APOSTROPHE);
            case RESTART -> tapped(Keys.R);
            case EXIT -> tapped(Keys.Q);
        };
    }

    @Override
    public float getAxisValue(InputAxis axis) {
        return switch (axis) {
            // A key is all or nothing: full throttle while Up is held.
            case THRUST_MAIN_AXIS -> held(Keys.UP) ? 1f : 0f;
        };
    }

    private static boolean held(int key) {
        return Gdx.input.isKeyPressed(key);
    }

    private static boolean tapped(int key) {
        return Gdx.input.isKeyJustPressed(key);
    }
}
