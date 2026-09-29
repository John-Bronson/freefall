package com.johnbronson.freefall.controls;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerAdapter;
import com.badlogic.gdx.controllers.Controllers;

/**
 * Merges keyboard and gamepad input into game actions. Register it with
 * {@code Controllers.addListener} so it follows controllers being plugged in and out.
 */
public class InputManager extends ControllerAdapter {
    private final KeyboardBindings keyboardBindings = new KeyboardBindings();
    private ControllerBindings controllerBindings;
    private Controller activeController;

    public InputManager() {
        if (Controllers.getControllers().size > 0) {
            setController(Controllers.getControllers().first());
        }
    }

    private void setController(Controller controller) {
        activeController = controller;
        controllerBindings = controller == null ? null : new ControllerBindings(controller);
        if (controller != null) {
            Gdx.app.log("InputManager", "Using controller: " + controller.getName());
        }
    }

    @Override
    public void connected(Controller controller) {
        setController(controller);
    }

    @Override
    public void disconnected(Controller controller) {
        if (controller == activeController) {
            Gdx.app.log("InputManager", "Controller disconnected: " + controller.getName());
            setController(null);
        }
    }

    public Controller getActiveController() {
        return activeController;
    }

    public boolean isPressed(InputAction action) {
        if (controllerBindings != null && controllerBindings.isPressed(action)) {
            return true;
        }
        return keyboardBindings.isPressed(action);
    }

    public float getAxisValue(InputAxis axis) {
        if (controllerBindings != null) {
            float controllerValue = controllerBindings.getAxisValue(axis);
            if (controllerValue != 0.0f) {
                return controllerValue;
            }
        }
        return keyboardBindings.getAxisValue(axis);
    }
}
