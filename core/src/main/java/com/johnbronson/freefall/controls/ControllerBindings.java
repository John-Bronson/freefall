package com.johnbronson.freefall.controls;

import com.badlogic.gdx.controllers.Controller;

/**
 * Gamepad layout. Button and axis numbers are the raw indices libGDX reports; press '
 * in game to see them live in the debug overlay.
 */
public final class ControllerBindings implements InputBinding {

    /** How far a stick or trigger must move before it counts, from 0 to 1. */
    private static final float DEAD_ZONE = 0.5f;

    private static final int LEFT_STICK_Y_AXIS = 1;

    private final Controller controller;

    /** @param controller never null; {@link InputManager} only builds this for a real controller. */
    public ControllerBindings(Controller controller) {
        this.controller = controller;
    }

    @Override
    public boolean isPressed(InputAction action) {
        // See KeyboardBindings for how a switch expression works. `default` covers the
        // actions that have no gamepad binding.
        return switch (action) {
            case ROTATE_LEFT -> controller.getButton(13);
            case ROTATE_RIGHT -> controller.getButton(14);
            case THRUST_SOLID -> controller.getButton(0);
            case PAN_CAM_LEFT -> controller.getButton(1);
            case PAN_CAM_RIGHT -> controller.getButton(2);
            case PAN_CAM_UP -> controller.getButton(3);
            case ZOOM_IN -> controller.getAxis(5) > DEAD_ZONE;
            case ZOOM_OUT -> controller.getAxis(2) > DEAD_ZONE;
            case EXIT -> controller.getButton(7);
            default -> false;
        };
    }

    @Override
    public float getAxisValue(InputAxis axis) {
        return switch (axis) {
            case THRUST_MAIN_AXIS -> {
                // Stick up reads negative, so flip it: full up = full throttle.
                // `yield` hands back the value from a block of statements in a switch
                // expression, where a one-line case would use `->` alone.
                float thrust = -controller.getAxis(LEFT_STICK_Y_AXIS);
                yield thrust > DEAD_ZONE ? thrust : 0f;
            }
        };
    }
}
