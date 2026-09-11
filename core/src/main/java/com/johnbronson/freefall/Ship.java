package com.johnbronson.freefall;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Ship {

    float x, y, angle;
    float vx = 0; // horizontal velocity
    float vy = 0; // vertical velocity

    float maxSpeed = 300;
    float acceleration = 250;

    Texture shipTexture;


    public Ship(float x, float y, int angle) {
        this.x = x;
        this.y = y;
        this.angle = angle;
        shipTexture = new Texture("ship.png");
    }

    public void setAngle(float angle) {
        this.angle = angle;
    }

    public float getAngle() {
        return angle;
    }

    public void draw(SpriteBatch batch) {
        batch.draw(shipTexture, x, y);
    }
}
