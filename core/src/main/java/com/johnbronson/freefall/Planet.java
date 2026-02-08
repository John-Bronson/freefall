package com.johnbronson.freefall;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class Planet {

    float x, y, radius;
    Color color;

    public Planet(float x, float y, float radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = Color.WHITE;
    }

    public void draw(ShapeRenderer shape) {
        shape.setColor(color);
        shape.circle(x, y, radius);
    }
}
