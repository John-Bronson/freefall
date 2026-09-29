package com.johnbronson.freefall;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.ChainShape;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class Planet {
    private static final float DEFAULT_RADIUS = 600;
    float minHeight = 1.0f;
    float maxHeight = 1.3f;
    float landingHeight = 1.15f;
    float atmosphereHeight = 1.8f;

    float x, y, radius, mass;
    Color color;
    Color atmosphereColor;
    float[] terrainPoints = new float[Constants.MILS_PER_CIRCLE];
    
    Body physicsBody;

    public Planet(float x, float y, float radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.mass = 1.0f;
        this.color = Color.GREEN;
        this.atmosphereColor = new Color(0.5f, 0.5f, 1f, 0.3f);
        terrainPoints = generateTerrain();
        makeLandingZones();
        System.out.println("First terrain point: " + terrainPoints[0]);
        System.out.println("Last terrain point: " + terrainPoints[6399]);
        
        PhysicsWorld world = PhysicsWorld.getInstance();
        physicsBody = world.createBody(true, x, y);
        
        ChainShape chain = createTerrainChain();
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = chain;
        fixtureDef.isSensor = false;
        physicsBody.createFixture(fixtureDef);
    }

    public Planet(float x, float y) {
        this(x, y, DEFAULT_RADIUS);
    }
    
    private ChainShape createTerrainChain() {
        int step = 64;
        int numVertices = (terrainPoints.length / step) + 1;
        com.badlogic.gdx.math.Vector2[] vertices = new com.badlogic.gdx.math.Vector2[numVertices];
        
        for (int i = 0; i < terrainPoints.length; i += step) {
            float distance = terrainPoints[i] * radius;
            float angle = i * Constants.MILS_TO_RADIANS;
            
            int idx = i / step;
            vertices[idx] = new com.badlogic.gdx.math.Vector2(
                (x + distance * (float)Math.cos(angle)) / PhysicsWorld.PIXELS_PER_METER,
                (y + distance * (float)Math.sin(angle)) / PhysicsWorld.PIXELS_PER_METER
            );
        }
        
        vertices[numVertices - 1] = vertices[0];
        
        ChainShape chain = new ChainShape();
        chain.createChain(vertices);
        
        return chain;
    }

    public void draw(ShapeRenderer shape) {

        // Draw shapes
        shape.begin(ShapeRenderer.ShapeType.Filled);

        // Draw atmosphere
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.setColor(atmosphereColor);
        shape.circle(x, y, radius * atmosphereHeight);

        shape.setColor(color);



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

        shape.end();

        // Draw lines
        shape.begin(ShapeRenderer.ShapeType.Line);

        for (int currentMil = 0; currentMil < terrainPoints.length; currentMil += 1) {

            if ((currentMil > 0 && currentMil < 401) ||
                (currentMil > 2133 && currentMil < 2533) ||
                (currentMil > 4267 && currentMil < 4667)) {

                float currentDistance = terrainPoints[currentMil] + 1;
                float currentAngle = currentMil * Constants.MILS_TO_RADIANS;
                float currentX = this.x + currentDistance * (float)Math.cos(currentAngle);
                float currentY = this.y + currentDistance * (float)Math.sin(currentAngle);

                float nextAngle = (currentMil+1) * Constants.MILS_TO_RADIANS;
                float nextX = this.x + currentDistance * (float)Math.cos(nextAngle);
                float nextY = this.y + currentDistance * (float)Math.sin(nextAngle);

                shape.setColor(Color.YELLOW);
                shape.line(currentX, currentY, nextX, nextY);
            }
        }
        shape.end();
    }


    private float[] generateTerrain () {
        float[] calculatedTerrainPoints = new float[Constants.MILS_PER_CIRCLE];
        float max = DEFAULT_RADIUS * maxHeight;
        float min = DEFAULT_RADIUS * minHeight;

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
            terrainPoints[i] = DEFAULT_RADIUS * landingHeight;
        }
    }
}
