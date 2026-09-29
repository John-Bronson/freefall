package com.johnbronson.freefall.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.WorldManifold;
import com.johnbronson.freefall.physics.FixtureRole;

/**
 * Tells the ship when it touches or leaves the ground.
 *
 * <p>Box2D calls these methods from inside {@code world.step()}, while the world is
 * locked. Creating or destroying bodies here crashes in native code. Changing
 * velocities is allowed but fights the solver. So this listener only <em>reports</em>
 * what happened. {@link Ship#afterStep()} decides what it means once the step is done.
 */
public final class ShipContactListener implements ContactListener {

    private final Vector2 relativeVelocity = new Vector2();

    @Override
    public void beginContact(Contact contact) {
        Fixture hull = FixtureRole.SHIP_HULL.findIn(contact);
        Fixture terrain = FixtureRole.TERRAIN.findIn(contact);
        if (hull == null || terrain == null) return;

        Ship ship = (Ship) hull.getBody().getUserData();
        Planet planet = (Planet) terrain.getBody().getUserData();
        float speed = impactSpeed(contact, hull.getBody(), terrain.getBody());
        ship.onGroundContactBegin(planet, speed);
    }

    @Override
    public void endContact(Contact contact) {
        Fixture hull = FixtureRole.SHIP_HULL.findIn(contact);
        if (hull == null || FixtureRole.TERRAIN.findIn(contact) == null) return;

        ((Ship) hull.getBody().getUserData()).onGroundContactEnd();
    }

    /** Speed of the ship relative to the ground, measured at the point of contact. */
    private float impactSpeed(Contact contact, Body ship, Body ground) {
        WorldManifold manifold = contact.getWorldManifold();
        if (manifold.getNumberOfContactPoints() == 0) {
            return ship.getLinearVelocity().len();
        }
        Vector2 point = manifold.getPoints()[0];

        // Each body returns the same Vector2 instance from every call, so copy before
        // doing arithmetic with it.
        relativeVelocity.set(ship.getLinearVelocityFromWorldPoint(point))
            .sub(ground.getLinearVelocityFromWorldPoint(point));
        return relativeVelocity.len();
    }

    /** Could cancel a contact before it's solved, e.g. for one-way platforms. Unused. */
    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
    }

    /** Reports the impulse used to push bodies apart, a direct measure of how hard they hit. Unused. */
    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {
    }
}
