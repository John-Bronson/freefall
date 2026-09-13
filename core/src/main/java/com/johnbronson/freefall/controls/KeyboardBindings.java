package com.johnbronson.freefall.controls;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.Gdx;

public class KeyboardBindings implements InputBinding {
    @Override
    public boolean isPressed(InputAction action) {
        switch (action) {
            case ROTATE_LEFT:
                return Gdx.input.isKeyPressed(Input.Keys.LEFT);
            case ROTATE_RIGHT:
                return Gdx.input.isKeyPressed(Input.Keys.RIGHT);
            case THRUST_MAIN:
                return Gdx.input.isKeyPressed(Input.Keys.UP);
            case THRUST_SOLID:
                return Gdx.input.isKeyJustPressed(Input.Keys.SPACE);
            case PAN_CAM_LEFT:
                return Gdx.input.isKeyJustPressed(Input.Keys.A);
            case PAN_CAM_RIGHT:
                return Gdx.input.isKeyJustPressed(Input.Keys.D);
            case PAN_CAM_UP:
                return Gdx.input.isKeyJustPressed(Input.Keys.W);
            case PAN_CAM_DOWN:
                return Gdx.input.isKeyJustPressed(Input.Keys.S);
            case ZOOM_IN:
                return Gdx.input.isKeyJustPressed(Input.Keys.Z);
            case ZOOM_OUT:
                return Gdx.input.isKeyJustPressed(Input.Keys.X);
            case EXIT:
                return Gdx.input.isKeyJustPressed(Input.Keys.Q);
            default:
                return false;
        }
    }

    @Override
    public float getAxisValue(InputAxis axis) {
        switch (axis) {
            case THRUST_MAIN_AXIS:
                return Gdx.input.isKeyPressed(Input.Keys.UP) ? 1.0f : 0.0f;
            case PAN_CAM_HORIZONTAL:
                return 0.0f;
            case PAN_CAM_VERTICAL:
                return 0.0f;
            case ZOOM_AXIS:
                return 0.0f;
            default:
                return 0.0f;
        }
    }
}
