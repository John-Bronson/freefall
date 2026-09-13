package com.johnbronson.freefall.controls;

public interface InputBinding {
    boolean isPressed(InputAction action);
    float getAxisValue(InputAxis axis);
}
