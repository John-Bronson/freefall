package com.johnbronson.freefall;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class Planet {
    private static final float DEFAULT_RADIUS = 250f;

    float x, y, radius;
    Color color;
    float[] terrainPoints = new float[Constants.MILS_PER_CIRCLE];

    public Planet(float x, float y, float radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = Color.WHITE;
        terrainPoints = generateTerrain();
        System.out.println("First terrain point: " + terrainPoints[0]);
        System.out.println("Last terrain point: " + terrainPoints[6399]);
    }

    public Planet(float x, float y) {
        this(x, y, DEFAULT_RADIUS);
    }

    public void draw(ShapeRenderer shape) {
        shape.setColor(color);
//         shape.circle(x, y, radius);
        // draw all the triangles
        for (int currentMil = 0; currentMil < terrainPoints.length; currentMil++) {

            // Get current point
            float currentDistance = terrainPoints[currentMil];
            float currentAngle = currentMil * Constants.MILS_TO_RADIANS;
            float currentX = this.x + currentDistance * (float)Math.cos(currentAngle);
            float currentY = this.y + currentDistance * (float)Math.sin(currentAngle);

            // Get the next point (wrapping around at the end)
            int nextMil = (currentMil +1) % terrainPoints.length; // wraps to first member of array if we're at the end
            float nextDistance = terrainPoints[nextMil];
            float nextAngle = nextMil * Constants.MILS_TO_RADIANS;
            float nextX = x + nextDistance * (float)Math.cos(nextAngle);
            float nextY = y + nextDistance * (float)Math.sin(nextAngle);

            // Draw the triangle with our three points
            shape.triangle(this.x, this.y, currentX, currentY, nextX, nextY);
        }

    }


    private float[] generateTerrain () {
        float[] calculatedTerrainPoints = new float[Constants.MILS_PER_CIRCLE];
        float max = 255;
        float min = 250;

        for (int i = 0; i < calculatedTerrainPoints.length; i++) {
            calculatedTerrainPoints[i] = randomFloat(min, max);
        }

        return calculatedTerrainPoints;
    }

    private float randomFloat(float min, float max) {
        return min + (float)Math.random() * (max - min);
    }
}
