package com.johnbronson.freefall.controls;

/**
 * Digital (on/off) inputs. Main-engine thrust is analog, so it's an {@link InputAxis}
 * instead.
 */
public enum InputAction {
    ROTATE_LEFT,
    ROTATE_RIGHT,
    THRUST_SOLID,
    PAN_CAM_LEFT,
    PAN_CAM_RIGHT,
    PAN_CAM_UP,
    PAN_CAM_DOWN,
    ZOOM_IN,
    ZOOM_OUT,
    TOGGLE_CAMERA_LOCK,
    TOGGLE_DEBUG,
    /** Play one cycle of the predicted coast line (draw out, then clear). */
    TOGGLE_PREDICTOR,
    RESTART,
    EXIT
}
