package com.johnbronson.freefall.controls;

/** One input device's mapping from physical keys, buttons and sticks to game inputs. */
public interface InputBinding {
    boolean isPressed(InputAction action);

    /** 0 when idle, up to 1 at full deflection. */
    float getAxisValue(InputAxis axis);
}
