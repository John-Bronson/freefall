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

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private Texture image;
    private BitmapFont font;
    private OrthographicCamera cam;
    private Planet planet1;
    private Planet planet2;
    private ShapeRenderer shape;

    @Override
    public void create() {
        batch = new SpriteBatch();
        image = new Texture("libgdx.png");
        font = new BitmapFont();
        cam = new OrthographicCamera(3200, 2400);
        cam.position.set(0,0, 0);
        planet1 = new Planet(-900, 0, 250);
        planet2 = new Planet(900, 0, 250);
        shape = new ShapeRenderer();
    }

    @Override
    public void render() {
        handleInput();
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        cam.update();
        batch.setProjectionMatrix(cam.combined);
        shape.setProjectionMatrix(cam.combined);

        batch.begin();
        font.draw(batch, "Hello World!", 0, 0);
        batch.end();

        shape.begin(ShapeRenderer.ShapeType.Filled);
        planet1.draw(shape);
        planet2.draw(shape);
        shape.end();
    }

    public void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            Gdx.app.exit();
        } if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
            cam.translate(-100, 0, 0);
        } if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            cam.translate(100, 0, 0);
        } if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            cam.translate(0, 100, 0);
        } if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            cam.translate(0, -100, 0);
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        image.dispose();
        shape.dispose();
    }
}
