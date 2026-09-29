package com.johnbronson.freefall.physics;

/**
 * Collision filter bits.
 *
 * <p>Every fixture has a {@code categoryBits} ("what I am") and a {@code maskBits}
 * ("what I collide with"). Two fixtures collide only if each one's category is in the
 * other's mask. This is cheaper than rejecting contacts in a listener, because Box2D
 * never creates the contact in the first place.
 *
 * <p>Planned additions: atmosphere sensors that touch only the ship, and crash debris
 * that hits terrain but passes through the ship.
 */
public final class CollisionCategory {

    public static final short SHIP = 0x0001;
    public static final short TERRAIN = 0x0002;

    private CollisionCategory() {
    }
}
