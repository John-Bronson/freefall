package com.johnbronson.freefall;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.OrthographicCamera;

public class HUD {
    private Ship ship;
    private BitmapFont font;
    private OrthographicCamera hudCam;

    public HUD(Ship ship) {
        this.ship = ship;
        this.font = new BitmapFont();
        this.hudCam = new OrthographicCamera(800, 600);
        hudCam.setToOrtho(false, 800, 600);
    }

    public void render(SpriteBatch batch) {
        hudCam.update();
        batch.setProjectionMatrix(hudCam.combined);
        batch.begin();
        
        font.draw(batch, String.format("Solid Boost: %.0f%%", ship.getSolidBoostFuel() * 100), 550, 570);
        font.draw(batch, String.format("Main Fuel: %.0f", ship.getMainFuel()), 550, 540);
        
        batch.end();
    }
}
