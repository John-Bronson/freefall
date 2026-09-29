package com.johnbronson.freefall.physics;

/**
 * Hooks around each fixed physics step. {@link PhysicsWorld#update} calls these once
 * per step, not once per rendered frame.
 */
public interface StepListener {

    /**
     * Apply forces here. Box2D clears every body's accumulated force at the end of
     * each step, so continuous forces such as gravity and engines must be re-applied
     * before every step.
     */
    void beforeStep(float dt);

    /**
     * Run game rules here, such as landing and crash checks. The world is unlocked
     * again, so it is safe to create or destroy bodies. Inside a contact callback it
     * is not.
     */
    void afterStep(float dt);
}
