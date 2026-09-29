package com.johnbronson.freefall;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ScreenUtils;
import com.johnbronson.freefall.controls.InputAction;
import com.johnbronson.freefall.controls.InputAxis;
import com.johnbronson.freefall.controls.InputManager;
import com.johnbronson.freefall.render.CameraController;
import com.johnbronson.freefall.render.DebugOverlay;
import com.johnbronson.freefall.render.Hud;
import com.johnbronson.freefall.render.WorldRenderer;
import com.johnbronson.freefall.world.GameWorld;
import com.johnbronson.freefall.world.Ship;

/**
 * Application entry point. It wires the pieces together and runs the frame:
 * input → simulation → camera → drawing.
 */
public class Main extends ApplicationAdapter {

    private static final Color SPACE_COLOR = new Color(0.15f, 0.15f, 0.2f, 1f);

    private InputManager input;
    private GameWorld world;
    private WorldRenderer worldRenderer;
    private CameraController cameraController;
    private SpriteBatch batch;
    private Hud hud;
    private DebugOverlay debugOverlay;
    private boolean debugVisible;

    @Override
    public void create() {
        input = new InputManager();
        Controllers.addListener(input);

        world = new GameWorld(MathUtils.random.nextLong());
        worldRenderer = new WorldRenderer();
        cameraController = new CameraController(worldRenderer.getCamera());

        batch = new SpriteBatch();
        hud = new Hud(batch);
        debugOverlay = new DebugOverlay(batch);
    }

    @Override
    public void resize(int width, int height) {
        worldRenderer.resize(width, height);
        hud.resize(width, height);
        debugOverlay.resize(width, height);
    }

    @Override
    public void render() {
        float frameDelta = Gdx.graphics.getDeltaTime();

        handleInput();
        world.update(frameDelta);
        cameraController.update(world, frameDelta);

        ScreenUtils.clear(SPACE_COLOR);
        worldRenderer.render(world);
        if (debugVisible) {
            debugOverlay.render(world, worldRenderer.getViewport(), input.getActiveController());
        }
        hud.render(world);
    }

    private void handleInput() {
        if (input.isPressed(InputAction.EXIT)) {
            Gdx.app.exit();
            return;
        }
        if (input.isPressed(InputAction.TOGGLE_DEBUG)) debugVisible = !debugVisible;
        if (input.isPressed(InputAction.TOGGLE_CAMERA_LOCK)) cameraController.toggleFollowing();
        if (input.isPressed(InputAction.RESTART)) world.requestRestart();

        Ship ship = world.getShip();
        ship.setThrottle(input.getAxisValue(InputAxis.THRUST_MAIN_AXIS));
        ship.setTurnInput(turnInput());
        if (input.isPressed(InputAction.THRUST_SOLID)) ship.fireBooster();

        if (input.isPressed(InputAction.PAN_CAM_LEFT)) cameraController.pan(-1f, 0f);
        if (input.isPressed(InputAction.PAN_CAM_RIGHT)) cameraController.pan(1f, 0f);
        if (input.isPressed(InputAction.PAN_CAM_UP)) cameraController.pan(0f, 1f);
        if (input.isPressed(InputAction.PAN_CAM_DOWN)) cameraController.pan(0f, -1f);
        if (input.isPressed(InputAction.ZOOM_IN)) cameraController.zoomIn();
        if (input.isPressed(InputAction.ZOOM_OUT)) cameraController.zoomOut();
    }

    /** +1 = counter-clockwise (left), −1 = clockwise (right). */
    private float turnInput() {
        float turn = 0f;
        if (input.isPressed(InputAction.ROTATE_LEFT)) turn += 1f;
        if (input.isPressed(InputAction.ROTATE_RIGHT)) turn -= 1f;
        return turn;
    }

    @Override
    public void dispose() {
        Controllers.removeListener(input);
        world.dispose();
        worldRenderer.dispose();
        hud.dispose();
        debugOverlay.dispose();
        batch.dispose();
    }
}
