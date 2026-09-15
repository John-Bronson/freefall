package com.johnbronson.freefall;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.controllers.ControllerListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;

import com.johnbronson.freefall.controls.InputAction;
import com.johnbronson.freefall.controls.InputManager;

import static com.johnbronson.freefall.Constants.DEGREES_TO_MILS;

public class Main extends ApplicationAdapter implements ControllerListener {
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

    private boolean debugMode = false;
    private InputManager inputManager;
    private Controller activeController = null;

    @Override
    public void create() {
        batch = new SpriteBatch();
        image = new Texture("libgdx.png");
        font = new BitmapFont();
        cam = new OrthographicCamera(800, 600);
        cam.zoom = 2f;
        cam.position.set(0,0, 0);
        hudCam = new OrthographicCamera(800, 600);
        hudCam.setToOrtho(false, 800, 600);
        planet1 = new Planet(-400, 0);
        planet2 = new Planet(400, 0);
        shape = new ShapeRenderer();
        ship = new Ship(0, 0, 45);
        hud = new HUD(ship);
        inputManager = new InputManager();

        Controllers.addListener(this);

        Array<Controller> controllers = Controllers.getControllers();
        if (controllers.size > 0) {
            activeController = controllers.get(0);
            inputManager.setController(activeController);
            Gdx.app.log("Main", "Using controller: " + activeController.getName());
        }
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

        if (debugMode) {
            renderDebug();
        }
    }

    public void handleInput() {
        float rotationSpeed = Constants.DEGREES_TO_MILS * 360 * Gdx.graphics.getDeltaTime();

        if (inputManager.isPressed(InputAction.EXIT)) {
            Gdx.app.exit();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.APOSTROPHE)) {
            debugMode = !debugMode;
        }

        if (inputManager.isPressed(InputAction.ROTATE_LEFT)) {
            ship.setAngle(ship.getAngle() + rotationSpeed);
        } else if (inputManager.isPressed(InputAction.ROTATE_RIGHT)) {
            ship.setAngle(ship.getAngle() - rotationSpeed);
        }

        float thrustAmount = 0;
        if (inputManager.isPressed(InputAction.THRUST_SOLID)) {
            ship.useSolidBoost();
        } else if (inputManager.isPressed(InputAction.THRUST_MAIN)) {
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

        if (inputManager.isPressed(InputAction.PAN_CAM_LEFT)) {
            cam.translate(cam.zoom * -100, 0, 0);
        } else if (inputManager.isPressed(InputAction.PAN_CAM_RIGHT)) {
            cam.translate(cam.zoom * 100, 0, 0);
        } else if (inputManager.isPressed(InputAction.PAN_CAM_UP)) {
            cam.translate(0, cam.zoom * 100, 0);
        } else if (inputManager.isPressed(InputAction.PAN_CAM_DOWN)) {
            cam.translate(0, cam.zoom * -100, 0);
        }

        if (inputManager.isPressed(InputAction.ZOOM_IN)) {
            cam.zoom += 0.1f;
        } else if (inputManager.isPressed(InputAction.ZOOM_OUT)) {
            cam.zoom -= 0.1f;
        }
    }

    private void renderDebug() {
        batch.setProjectionMatrix(hudCam.combined);
        batch.begin();

        font.draw(batch, "DEBUG", 10, hudCam.viewportHeight - 20);

        Array<com.badlogic.gdx.controllers.Controller> controllers = Controllers.getControllers();
        font.draw(batch, "Controllers found: " + controllers.size, 10, hudCam.viewportHeight - 40);

        if (activeController != null) {
            String controllerInfo = "Active: " + activeController.getName();

            StringBuilder pressedButtons = new StringBuilder(" Buttons:");
            for (int i = 0; i <= 15; i++) {
                if (activeController.getButton(i)) {
                    pressedButtons.append(" ").append(i);
                }
            }

            StringBuilder axisInfo = new StringBuilder(" Axes:");
            for (int i = 0; i < activeController.getAxisCount(); i++) {
                float value = activeController.getAxis(i);
                if (Math.abs(value) > 0.1f) {
                    axisInfo.append(" ").append(i).append("=").append(String.format("%.2f", value));
                }
            }

            font.draw(batch, controllerInfo + pressedButtons.toString() + axisInfo.toString(),
                     10, hudCam.viewportHeight - 60);
        }

        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        image.dispose();
        shape.dispose();
        hud = null;
    }

    @Override
    public void connected(Controller controller) {
        Gdx.app.log("Main", "Controller connected: " + controller.getName());
        activeController = controller;
        inputManager.setController(controller);
    }

    @Override
    public void disconnected(Controller controller) {
        Gdx.app.log("Main", "Controller disconnected: " + controller.getName());
        if (activeController == controller) {
            activeController = null;
            inputManager.setController(null);
        }
    }

    @Override
    public boolean buttonDown(Controller controller, int buttonCode) {
        Gdx.app.log("Main", "Button pressed: " + buttonCode);
        return false;
    }

    @Override
    public boolean buttonUp(Controller controller, int buttonCode) {
        Gdx.app.log("Main", "Button released: " + buttonCode);
        return false;
    }

    @Override
    public boolean axisMoved(Controller controller, int axisCode, float value) {
        Gdx.app.log("Main", "Axis moved: " + axisCode + " = " + value);
        return false;
    }
}
