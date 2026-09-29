package com.johnbronson.freefall;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.math.Vector2;

public class GameContactListener implements ContactListener {
    @Override
    public void beginContact(Contact contact) {
        Fixture fixtureA = contact.getFixtureA();
        Fixture fixtureB = contact.getFixtureB();
        
        Body bodyA = fixtureA.getBody();
        Body bodyB = fixtureB.getBody();
        
        if (isShipBody(bodyA) || isShipBody(bodyB)) {
            Fixture shipFixture = isShipBody(bodyA) ? fixtureA : fixtureB;
            Fixture planetFixture = isShipBody(bodyA) ? fixtureB : fixtureA;
            
            checkLandingOrCrash(contact, bodyA, bodyB);
        }
    }
    
    @Override
    public void endContact(Contact contact) {
    }
    
    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
    }
    
    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {
    }
    
    private boolean isShipBody(Body body) {
        Object userData = body.getUserData();
        return userData != null && userData instanceof Ship;
    }
    
    private void checkLandingOrCrash(Contact contact, Body bodyA, Body bodyB) {
        Body shipBody = isShipBody(bodyA) ? bodyA : bodyB;
        Body planetBody = isShipBody(bodyA) ? bodyB : bodyA;
        
        Ship ship = (Ship) shipBody.getUserData();
        
        if (ship == null) return;
        
        float impactSpeed = calculateImpactSpeed(contact);
        
        if (impactSpeed < 50f) {
            checkLandingOrientation(ship, planetBody);
        } else {
            ship.crash("Impact velocity too high: " + Math.round(impactSpeed));
        }
    }
    
    private float calculateImpactSpeed(Contact contact) {
        Body bodyA = contact.getFixtureA().getBody();
        Body bodyB = contact.getFixtureB().getBody();
        
        Vector2 velA = bodyA.getLinearVelocityFromWorldPoint(
            contact.getWorldManifold().getPoints()[0]);
        Vector2 velB = bodyB.getLinearVelocityFromWorldPoint(
            contact.getWorldManifold().getPoints()[0]);
        
        // cpy(): Box2D reuses these Vector2 instances, so don't mutate them
        Vector2 relativeVel = velA.cpy().sub(velB);

        // Box2D speeds are in m/s; convert to px/s to match the rest of the game
        return relativeVel.len() * PhysicsWorld.PIXELS_PER_METER;
    }

    private void checkLandingOrientation(Ship ship, Body planetBody) {
        Body shipBody = ship.getBody();

        Vector2 shipPos = shipBody.getPosition();
        Vector2 planetPos = planetBody.getPosition();

        // "Up" at the landing site points from the planet center out to the ship
        Vector2 up = shipPos.cpy().sub(planetPos);
        float upAngle = (float)Math.atan2(up.y, up.x);

        // Wrap the difference into [-PI, PI] so e.g. 359° vs 1° reads as 2°
        float diff = shipBody.getAngle() - upAngle;
        float angleDiff = Math.abs((float)Math.atan2(Math.sin(diff), Math.cos(diff)));
        
        if (angleDiff < Constants.MAX_LANDING_ANGLE) {
            ship.land();
        } else {
            ship.crash("Ship tilted too much: " + Math.round(angleDiff * Constants.RADIANS_TO_DEGREES) + " degrees");
        }
    }
}
