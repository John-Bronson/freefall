package com.johnbronson.freefall.controls;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.utils.Array;

public class InputManager {
    private KeyboardBindings keyboardBindings;
    private ControllerBindings controllerBindings;

    public InputManager() {
        this.keyboardBindings = new KeyboardBindings();
        this.controllerBindings = null;
    }

    public void setController(Controller controller) {
        if (controller != null) {
            this.controllerBindings = new ControllerBindings(controller);
        }
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
