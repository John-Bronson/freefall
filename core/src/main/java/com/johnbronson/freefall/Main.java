package com.johnbronson.freefall;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private Texture image;
    private BitmapFont font;
    private OrthographicCamera cam;

    @Override
    public void create() {
        batch = new SpriteBatch();
        image = new Texture("libgdx.png");
        font = new BitmapFont();
        cam = new OrthographicCamera(800, 600);
        cam.position.set(0,0, 0);
    }

    @Override
    public void render() {
        handleInput();
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);

        cam.update();
        batch.setProjectionMatrix(cam.combined);

        batch.begin();
        batch.draw(image, Gdx.graphics.getWidth()-image.getWidth(), 0);
        font.draw(batch, "Hello World!", 100, 200);
        font.draw(batch, "Hello World!", 100, 300);
        batch.end();
    }

    public void handleInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            Gdx.app.exit();
        } if (Gdx.input.isKeyJustPressed(Input.Keys.A)) {
            cam.translate(-100, 0, 0);
        } if (Gdx.input.isKeyJustPressed(Input.Keys.D)) {
            cam.translate(100, 0, 0);
        } if (Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            cam.translate(0, 100, 0);
        } if (Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            cam.translate(0, -100, 0);
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        image.dispose();
    }
}
