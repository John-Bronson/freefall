package com.johnbronson.freefall;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import static com.johnbronson.freefall.PhysicsWorld.PIXELS_PER_METER;

public class Ship {

    Body body;
    
    float solidBoostFuel = 1.0f;
    float mainFuel = 5.0f;

    static final float SOLID_BOOST_DURATION = 2f;
    float solidBoostTimer = 0f;
    
    boolean landed = false;
    
    public Ship(float x, float y, int angle) {
        PhysicsWorld world = PhysicsWorld.getInstance();
        
        BodyDef def = new BodyDef();
        def.type = BodyDef.BodyType.DynamicBody;
        def.position.set(x / PIXELS_PER_METER, y / PIXELS_PER_METER);
        def.angle = angle * Constants.MILS_TO_RADIANS;
        def.fixedRotation = false;
        def.bullet = true;
        
        body = world.getWorld().createBody(def);
        body.setUserData(this);
        
        CircleShape shape = new CircleShape();
        shape.setRadius(6f / PIXELS_PER_METER);
        
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 1.0f;
        fixtureDef.friction = 0.3f;
        fixtureDef.restitution = 0.1f;
        
        body.createFixture(fixtureDef);
        shape.dispose();
    }
    
    public void setAngle(float angle) {
        if (landed) return;
        float radians = angle * Constants.MILS_TO_RADIANS;
        body.setTransform(body.getPosition(), radians);
    }
    
    public float getAngle() {
        return body.getAngle() * Constants.RADIANS_TO_MILS;
    }
    
    public void draw(ShapeRenderer shape) {
        Vector2 pos = body.getPosition();
        float angle = body.getAngle();
        
        float size = 12;
        float noseX = (float)(pos.x * PIXELS_PER_METER + size * Math.cos(angle));
        float noseY = (float)(pos.y * PIXELS_PER_METER + size * Math.sin(angle));

        float leftAngle = angle - 1600 * Constants.MILS_TO_RADIANS;
        float leftX = (float)(pos.x * PIXELS_PER_METER + size * 0.5f * Math.cos(leftAngle));
        float leftY = (float)(pos.y * PIXELS_PER_METER + size * 0.5f * Math.sin(leftAngle));

        float rightAngle = angle + 1600 * Constants.MILS_TO_RADIANS;
        float rightX = (float)(pos.x * PIXELS_PER_METER + size * 0.5f * Math.cos(rightAngle));
        float rightY = (float)(pos.y * PIXELS_PER_METER + size * 0.5f * Math.sin(rightAngle));

        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(Color.WHITE);
        shape.triangle(noseX, noseY, leftX, leftY, rightX, rightY);
        shape.end();

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(Color.RED);
        shape.circle(pos.x * PIXELS_PER_METER, pos.y * PIXELS_PER_METER, 2);
        shape.end();
    }
    
    public void update(float deltaTime) {
        if (landed) return;
        
        if (solidBoostTimer > 0) {
            solidBoostTimer -= deltaTime;
            float thrust = Constants.SOLID_BOOST_ACCELERATION;
            applyThrust(thrust, deltaTime);
        }
    }
    
    public void applyMainThrust(float deltaTime) {
        if (landed) return;
        float thrust = Constants.MAIN_THRUST_ACCELERATION;
        applyThrust(thrust, deltaTime);
    }
    
    private void applyThrust(float thrustAmount, float deltaTime) {
        float angle = body.getAngle();
        Vector2 force = new Vector2(
            (float)Math.cos(angle) * thrustAmount,
            (float)Math.sin(angle) * thrustAmount
        );
        body.applyForceToCenter(force, true);
    }
    
    public void applyGravity(Planet planet1, Planet planet2, float deltaTime) {
        if (landed) return;
        
        applyGravityFromPlanet(planet1, deltaTime);
        applyGravityFromPlanet(planet2, deltaTime);
    }
    
    private void applyGravityFromPlanet(Planet planet, float deltaTime) {
        Vector2 shipPos = body.getPosition();
        float planetX = planet.x / PIXELS_PER_METER;
        float planetY = planet.y / PIXELS_PER_METER;
        
        float dx = planetX - shipPos.x;
        float dy = planetY - shipPos.y;
        float distSquared = dx * dx + dy * dy;

        float minDist = (planet.radius + 10f) / PIXELS_PER_METER;
        if (distSquared < minDist * minDist) {
            distSquared = minDist * minDist;
        }

        float gravityAccel = Constants.GRAVITATIONAL_CONSTANT * planet.mass / distSquared;
        float dist = (float)Math.sqrt(distSquared);
        float unitX = dx / dist;
        float unitY = dy / dist;

        Vector2 force = new Vector2(
            unitX * gravityAccel,
            unitY * gravityAccel
        );
        body.applyForceToCenter(force, true);
    }
    
    public void useSolidBoost() {
        if (solidBoostFuel > 0 && !landed) {
            solidBoostFuel -= 1;
            solidBoostTimer = SOLID_BOOST_DURATION;
        }
    }
    
    public boolean consumeMainFuel(float amount) {
        if (mainFuel >= amount && !landed) {
            mainFuel -= amount;
            return true;
        }
        return false;
    }
    
    public float getSolidBoostFuel() {
        return solidBoostFuel;
    }
    
    public float getMainFuel() {
        return mainFuel;
    }
    
    public Body getBody() {
        return body;
    }
    
    public float getX() {
        return body.getPosition().x * PhysicsWorld.PIXELS_PER_METER;
    }
    
    public float getY() {
        return body.getPosition().y * PhysicsWorld.PIXELS_PER_METER;
    }
    
    public void land() {
        landed = true;
        body.setLinearVelocity(0, 0);
        body.setAngularVelocity(0);
        body.setAwake(false);
    }
    
    public void crash(String reason) {
        System.out.println("CRASH: " + reason);
        Gdx.app.exit();
    }
}
