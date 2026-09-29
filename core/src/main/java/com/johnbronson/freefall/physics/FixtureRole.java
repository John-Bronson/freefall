package com.johnbronson.freefall.physics;

import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.Fixture;

/**
 * What a fixture represents in the game. Stored as the fixture's user data.
 *
 * <p>A body's user data says <em>who</em> is touching (which Ship, which Planet). A
 * fixture's role says <em>which part</em> is touching. When the ship gets landing
 * legs, a foot touching the ground is a landing and the hull touching the ground is a
 * crash. The two cases are told apart here, without comparing fixture references.
 */
public enum FixtureRole {
    SHIP_HULL,
    TERRAIN;

    private static FixtureRole of(Fixture fixture) {
        return fixture.getUserData() instanceof FixtureRole role ? role : null;
    }

    /**
     * Returns whichever of the contact's two fixtures has this role, or null.
     * Box2D doesn't order A and B, so a contact listener has to check both.
     */
    public Fixture findIn(Contact contact) {
        if (of(contact.getFixtureA()) == this) return contact.getFixtureA();
        if (of(contact.getFixtureB()) == this) return contact.getFixtureB();
        return null;
    }
}
