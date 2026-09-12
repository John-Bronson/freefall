package com.johnbronson.freefall;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class Ship {

    float x, y, angle;
    float vx = 0; // horizontal velocity
    float vy = 0; // vertical velocity

    float maxSpeed = 300;
    float acceleration = 250;

    public Ship(float x, float y, int angle) {
        this.x = x;
        this.y = y;
        this.angle = angle;
    }

    public void setAngle(float angle) {
        this.angle = angle;
    }

    public float getAngle() {
        return angle;
    }
    
    public void draw(ShapeRenderer shape) {
        float size = 12;
        float noseX = x + size * (float)Math.cos(angle * Constants.MILS_TO_RADIANS);
        float noseY = y + size * (float)Math.sin(angle * Constants.MILS_TO_RADIANS);
        
        float leftAngle = angle - 1600;
        float leftX = x + size * 0.5f * (float)Math.cos(leftAngle * Constants.MILS_TO_RADIANS);
        float leftY = y + size * 0.5f * (float)Math.sin(leftAngle * Constants.MILS_TO_RADIANS);
        
        float rightAngle = angle + 1600;
        float rightX = x + size * 0.5f * (float)Math.cos(rightAngle * Constants.MILS_TO_RADIANS);
        float rightY = y + size * 0.5f * (float)Math.sin(rightAngle * Constants.MILS_TO_RADIANS);
        
        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(Color.WHITE);
        shape.triangle(noseX, noseY, leftX, leftY, rightX, rightY);
        shape.end();
    }
    
    public void update(float deltaTime) {
        x += vx * deltaTime;
        y += vy * deltaTime;
    }
    
    public void applyGravity(Planet planet1, Planet planet2, float deltaTime) {
        applyGravityFromPlanet(planet1, deltaTime);
        applyGravityFromPlanet(planet2, deltaTime);
    }
    
    private void applyGravityFromPlanet(Planet planet, float deltaTime) {
        float dx = planet.x - x;
        float dy = planet.y - y;
        float distSquared = dx * dx + dy * dy;
        
        float minDist = planet.radius + 10f;
        if (distSquared < minDist * minDist) {
            distSquared = minDist * minDist;
        }
        
        float gravityAccel = Constants.GRAVITATIONAL_CONSTANT * planet.mass / distSquared;
        float dist = (float)Math.sqrt(distSquared);
        float unitX = dx / dist;
        float unitY = dy / dist;
        
        vx += unitX * gravityAccel * deltaTime;
        vy += unitY * gravityAccel * deltaTime;
    }
}
