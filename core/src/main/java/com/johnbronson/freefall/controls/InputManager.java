package com.johnbronson.freefall.controls;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerAdapter;
import com.badlogic.gdx.controllers.Controllers;

/**
 * Merges keyboard and gamepad input into game actions. Register it with
 * {@code Controllers.addListener} so it follows controllers being plugged in and out.
 */
public final class InputManager extends ControllerAdapter {

    private static final String LOG_TAG = "InputManager";

    private final KeyboardBindings keyboardBindings = new KeyboardBindings();

    /** Both null while no gamepad is connected. */
    private Controller activeController;
    private ControllerBindings controllerBindings;

    public InputManager() {
        if (Controllers.getControllers().notEmpty()) {
            connected(Controllers.getControllers().first());
        }
    }

    @Override
    public void connected(Controller controller) {
        Gdx.app.log(LOG_TAG, "Using controller: " + controller.getName());
        activeController = controller;
        controllerBindings = new ControllerBindings(controller);
    }

    @Override
    public void disconnected(Controller controller) {
        if (controller != activeController) return;

        Gdx.app.log(LOG_TAG, "Controller disconnected: " + controller.getName());
        activeController = null;
        controllerBindings = null;
    }

    public Controller getActiveController() {
        return activeController;
    }

    /** True if either the gamepad or the keyboard has the action pressed. */
    public boolean isPressed(InputAction action) {
        boolean onGamepad = controllerBindings != null && controllerBindings.isPressed(action);
        return onGamepad || keyboardBindings.isPressed(action);
    }

    /** The gamepad's value if it's being used, otherwise the keyboard's. */
    public float getAxisValue(InputAxis axis) {
        float onGamepad = controllerBindings != null ? controllerBindings.getAxisValue(axis) : 0f;
        return onGamepad != 0f ? onGamepad : keyboardBindings.getAxisValue(axis);
    }
}
