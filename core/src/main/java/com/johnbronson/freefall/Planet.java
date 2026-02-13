package com.johnbronson.freefall;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class Planet {
    private static final float DEFAULT_RADIUS = 100;
    int landingHeight = 105;
    float x, y, radius;
    Color color;
    float[] terrainPoints = new float[Constants.MILS_PER_CIRCLE];

    public Planet(float x, float y, float radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.color = Color.WHITE;
        terrainPoints = generateTerrain();
        makeLandingZones();
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
        int step = 50;
        for (int currentMil = 0; currentMil < terrainPoints.length; currentMil += step) {

            // Get current point
            float currentDistance = terrainPoints[currentMil];
            float currentAngle = currentMil * Constants.MILS_TO_RADIANS;
            float currentX = this.x + currentDistance * (float)Math.cos(currentAngle);
            float currentY = this.y + currentDistance * (float)Math.sin(currentAngle);

            // Get the next point (wrapping around at the end)
            int nextMil = (currentMil + step) % terrainPoints.length; // wraps to first member of array if we're at the end
            float nextDistance = terrainPoints[nextMil];
            float nextAngle = nextMil * Constants.MILS_TO_RADIANS;
            float nextX = x + nextDistance * (float)Math.cos(nextAngle);
            float nextY = y + nextDistance * (float)Math.sin(nextAngle);

            // Draw the triangle with our three points
            shape.triangle(this.x, this.y, currentX, currentY, nextX, nextY);
        }

        for (int currentMil = 0; currentMil < terrainPoints.length; currentMil += step) {

            if ((currentMil > 0 && currentMil < 401) ||
                (currentMil > 2133 && currentMil < 2533) ||
                (currentMil > 4267 && currentMil < 4667)) {


            }
        }

    }


    private float[] generateTerrain () {
        float[] calculatedTerrainPoints = new float[Constants.MILS_PER_CIRCLE];
        float max = 110;
        float min = 100;

        for (int i = 0; i < calculatedTerrainPoints.length; i++) {
            calculatedTerrainPoints[i] = randomFloat(min, max);
        }

        return calculatedTerrainPoints;
    }

    private float randomFloat(float min, float max) {
        return min + (float)Math.random() * (max - min);
    }

    private void makeLandingZones() {
        System.out.println("Generating landing zones...");
        // landing zones will be at 0-400, 2133-2533, and 4267-4667 in the terrain array

        makeOneLandingZone(0, 400);
        makeOneLandingZone(2133, 2533);
        makeOneLandingZone(4267, 4667);
    }

    private void makeOneLandingZone(int start, int finish) {
        for (int i = start; i < finish; i++) {
            terrainPoints[i] = this.landingHeight;
        }
    }
}
