package com.johnbronson.freefall.controls;

import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.Gdx;

public class ControllerBindings implements InputBinding {
    private final Controller controller;

    public ControllerBindings(Controller controller) {
        this.controller = controller;
    }

    @Override
    public boolean isPressed(InputAction action) {
        if (controller == null) return false;

        switch (action) {
            case ROTATE_LEFT:
                return controller.getButton(13);
            case ROTATE_RIGHT:
                return controller.getButton(14);
            case THRUST_MAIN:
                return controller.getAxis(1) < -0.5f;
            case THRUST_SOLID:
                return controller.getButton(0);
            case PAN_CAM_LEFT:
                return controller.getButton(1);
            case PAN_CAM_RIGHT:
                return controller.getButton(2);
            case PAN_CAM_UP:
                return controller.getButton(3);
            case ZOOM_IN:
                return controller.getAxis(5) > 0.5f;
            case ZOOM_OUT:
                return controller.getAxis(2) > 0.5f;
            case EXIT:
                return controller.getButton(7);
            default:
                return false;
        }
    }

    @Override
    public float getAxisValue(InputAxis axis) {
        if (controller == null) return 0.0f;

        switch (axis) {
            case THRUST_MAIN_AXIS:
                float thrust = controller.getAxis(1);
                return thrust < -0.5f ? -thrust : 0.0f;
            case PAN_CAM_HORIZONTAL:
                return controller.getAxis(0);
            case PAN_CAM_VERTICAL:
                return controller.getAxis(1);
            case ZOOM_AXIS:
                float zoomIn = controller.getAxis(5);
                float zoomOut = controller.getAxis(2);
                if (zoomIn > 0.5f) return zoomIn;
                if (zoomOut > 0.5f) return -zoomOut;
                return 0.0f;
            default:
                return 0.0f;
        }
    }
}
