package com.johnbronson.freefall;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.ChainShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.graphics.OrthographicCamera;

public class PhysicsWorld {
    private static PhysicsWorld instance;
    
    private final World world;
    private Box2DDebugRenderer debugRenderer;
    
    public static final float PIXELS_PER_METER = 10f;
    
    private PhysicsWorld() {
        // No global gravity: planet gravity is applied manually in Ship.applyGravity
        world = new World(new Vector2(0, 0), false);
        debugRenderer = new Box2DDebugRenderer();
    }
    
    public static PhysicsWorld getInstance() {
        if (instance == null) {
            instance = new PhysicsWorld();
        }
        return instance;
    }
    
    public World getWorld() {
        return world;
    }
    
    public void setGravity(float x, float y) {
        world.setGravity(new Vector2(x, y));
    }
    
    public Body createBody(boolean isStatic, float x, float y) {
        BodyDef def = new BodyDef();
        def.type = isStatic ? BodyDef.BodyType.StaticBody : BodyDef.BodyType.DynamicBody;
        def.position.set(x / PIXELS_PER_METER, y / PIXELS_PER_METER);
        return world.createBody(def);
    }
    
    public void update(float deltaTime) {
        world.step(deltaTime, 6, 2);
    }
    
    public void renderDebug(OrthographicCamera camera) {
        // Camera works in pixels, Box2D in meters: scale the matrix to match
        debugRenderer.render(world, camera.combined.cpy().scl(PIXELS_PER_METER));
    }
    
    public void dispose() {
        if (debugRenderer != null) {
            debugRenderer.dispose();
        }
        world.dispose();
    }
}
