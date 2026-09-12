package com.johnbronson.freefall;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;

import static com.johnbronson.freefall.Constants.DEGREES_TO_MILS;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private Texture image;
    private BitmapFont font;
    private OrthographicCamera cam;
    private OrthographicCamera hudCam;
    private Planet planet1;
    private Planet planet2;
    private Ship ship;
    private ShapeRenderer shape;
    private HUD hud;

    @Override
    public void create() {
        batch = new SpriteBatch();
        image = new Texture("libgdx.png");
        font = new BitmapFont();
        cam = new OrthographicCamera(800, 600);
        cam.position.set(0,0, 0);
        hudCam = new OrthographicCamera(800, 600);
        hudCam.setToOrtho(false, 800, 600);
        planet1 = new Planet(-400, 0);
        planet2 = new Planet(400, 0);
        shape = new ShapeRenderer();
        ship = new Ship(0, 0, 45);
        hud = new HUD(ship);
    }

    @Override
    public void render() {
        handleInput();
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        cam.update();
        batch.setProjectionMatrix(cam.combined);
        shape.setProjectionMatrix(cam.combined);

        ship.applyGravity(planet1, planet2, Gdx.graphics.getDeltaTime());
        ship.update(Gdx.graphics.getDeltaTime());

        planet1.draw(shape);
        planet2.draw(shape);
        ship.draw(shape);

        hud.render(batch);
    }

    public void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            Gdx.app.exit();
        } if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            ship.setAngle(ship.getAngle() + Constants.DEGREES_TO_MILS * 360 * Gdx.graphics.getDeltaTime());
        } if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            ship.setAngle(ship.getAngle() - Constants.DEGREES_TO_MILS * 360 * Gdx.graphics.getDeltaTime());
        }
        
        float thrustAmount = 0;
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            ship.useSolidBoost();
        } else if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            float fuelConsumptionRate = 1.0f;
            if (ship.consumeMainFuel(fuelConsumptionRate * Gdx.graphics.getDeltaTime())) {
                thrustAmount = ship.acceleration;
            }
        }
        
        if (thrustAmount != 0) {
            double angleRad = ship.angle * Constants.MILS_TO_RADIANS;
            ship.vx += (float)Math.cos(angleRad) * thrustAmount * Gdx.graphics.getDeltaTime();
            ship.vy += (float)Math.sin(angleRad) * thrustAmount * Gdx.graphics.getDeltaTime();
        }
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            cam.translate(cam.zoom * -100, 0, 0);
        } if (Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            cam.translate(cam.zoom * 100, 0, 0);
        } if (Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            cam.translate(0, cam.zoom * 100, 0);
        } if (Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            cam.translate(0, cam.zoom * -100, 0);
        } if (Gdx.input.isKeyJustPressed(Input.Keys.Z)) {
            cam.zoom += 0.1f;
        } if (Gdx.input.isKeyJustPressed(Input.Keys.X)) {
            cam.zoom -= 0.1f;
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        image.dispose();
        shape.dispose();
        hud = null;
    }
}
