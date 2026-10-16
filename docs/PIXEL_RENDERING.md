# Pixel-art rendering strategy: native-res framebuffer + nearest rotation

Reference for anyone (human or Claude Code) touching rendering, camera, sprite,
or asset code in this project. Read this before changing any of those.

## TL;DR — rules that apply to every rendering change

1. **The whole scene renders into a low-res `FrameBuffer` at the virtual
   resolution** (`Constants.VIRTUAL_W × VIRTUAL_H`, e.g. 640×480), then that
   buffer is blitted to the window at an **integer** scale with
   `TextureFilter.Nearest`. Nothing draws straight to the window except the blit
   (and optionally HUD text, see below).
2. **Sprites rotate at runtime to a continuous angle** (`body.getAngle()`),
   sampled with `Nearest`. We do **not** quantize the heading and we do **not**
   use pre-rendered rotation frames. The crunchy, slightly flickering outline at
   off-axis angles is the intended look, not a bug.
3. **Every sprite position and the camera position are rounded to integer
   virtual pixels** before drawing. Physics stays in floats; only the draw call
   is snapped.
4. **No fractional scale anywhere**: `camera.zoom == 1`, sprite `scaleX/Y == 1`,
   window scale is `floor(min(bw / VIRTUAL_W, bh / VIRTUAL_H))`, leftover is
   letterboxed. Use the **back-buffer** size, not the logical window size.
5. **All textures use `Nearest` min and mag filters.** No mipmaps, no `Linear`,
   no `MipMapLinearLinear` "for quality".
6. `VIRTUAL_W` and `VIRTUAL_H` stay **even** so an integer camera position puts
   pixel edges on integer coordinates.

If a change would violate one of these, stop and ask before doing it.

## The look we're going for

Reference: Subspace / Continuum (1995–97 2D space combat). That game drew ships
as 36×36 px sprites with 40 pre-drawn rotation frames (one per 9°). We borrow
the sprite scale and the "everything on one pixel grid" feel, but we
deliberately render rotation at runtime instead of using frames.

Rendering approaches we compared (see `tools/rotation_demo.py`, which
generates the comparison sheet):

| Approach | Verdict |
|---|---|
| Scale the camera up and rotate the sprite at window resolution with filtering | **Rejected.** Soft, anti-aliased edges that don't sit on the pixel grid. Looks phony next to crisp pixel art. |
| Native-res FBO, nearest sampling, continuous angle, integer upscale | **Chosen.** On-grid, crisp, and the per-frame shimmer of thin details reads as period-correct (same artifacts as a rotozoomer / SNES Mode 7). |
| Same, but display angle snapped to 9° steps | Not chosen, but a one-line toggle (see *Tuning knobs*). Calmer, more like the original. |
| Pre-generated RotSprite frames (40 per ship) | Not chosen. Cleanest edges, but more asset work and it's the convention we're intentionally not following. |

What the chosen look does to art, by design:

- A 1-px outline turns into dashes at ~20–70°. A 2-px outline survives.
- 1-px stripes/details break up. Chunky details survive.
- The pattern of broken pixels changes every frame while the ship turns. That's
  the shimmer we want.
- Baked-in lighting rotates with the hull. Keep shading flat/symmetric.

## Pipeline

```
Box2D world (meters, floats)
      │  × PPM, round to int
      ▼
OrthographicCamera (VIRTUAL_W × VIRTUAL_H, zoom 1, integer position)
      │
      ▼
FrameBuffer VIRTUAL_W × VIRTUAL_H  ← all sprites, bullets, particles, map
      │  Nearest, integer scale, letterboxed
      ▼
Window back buffer                 ← optional crisp HUD drawn here
```

## Reference implementation

```java
package com.example.game.render;   // adjust to the project's package

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ScreenUtils;

/** Low-res render target + integer-scaled blit. One instance per game screen. */
public class PixelScreen implements Disposable {
    public final int W, H;                       // virtual resolution, must be even
    private final FrameBuffer fbo;
    private final TextureRegion fboRegion;
    public final OrthographicCamera worldCam = new OrthographicCamera();
    private final OrthographicCamera screenCam = new OrthographicCamera();
    private int scale = 1, offX, offY;

    public PixelScreen(int w, int h) {
        W = w; H = h;
        fbo = new FrameBuffer(Pixmap.Format.RGBA8888, W, H, false);
        fbo.getColorBufferTexture().setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
        fboRegion = new TextureRegion(fbo.getColorBufferTexture());
        fboRegion.flip(false, true);             // FBO color textures are upside down
        worldCam.setToOrtho(false, W, H);
        resize();
    }

    /** Call from Screen.resize(). Uses the back buffer size so HiDPI works. */
    public void resize() {
        int bw = Gdx.graphics.getBackBufferWidth();
        int bh = Gdx.graphics.getBackBufferHeight();
        scale = Math.max(1, Math.min(bw / W, bh / H));   // integer only
        offX = (bw - W * scale) / 2;                     // letterbox remainder
        offY = (bh - H * scale) / 2;
        screenCam.setToOrtho(false, bw, bh);
        screenCam.update();
    }

    /** Begin drawing the world. camX/camY are in virtual pixels (already × PPM). */
    public void begin(SpriteBatch batch, float camX, float camY) {
        worldCam.position.set(MathUtils.round(camX), MathUtils.round(camY), 0);
        worldCam.update();
        fbo.begin();
        ScreenUtils.clear(0, 0, 0, 1);
        batch.setProjectionMatrix(worldCam.combined);
        batch.begin();
    }

    /** End world drawing and blit the buffer to the window. */
    public void end(SpriteBatch batch) {
        batch.end();
        fbo.end();                               // restores the back-buffer viewport
        ScreenUtils.clear(0, 0, 0, 1);
        batch.setProjectionMatrix(screenCam.combined);
        batch.disableBlending();
        batch.begin();
        batch.draw(fboRegion, offX, offY, W * scale, H * scale);
        batch.end();
        batch.enableBlending();
    }

    public int getScale() { return scale; }

    @Override public void dispose() { fbo.dispose(); }
}
```

Usage in a `Screen`:

```java
@Override public void render(float delta) {
    world.step(1 / 60f, 6, 2);       // physics in floats, untouched by any of this
    clampSpeed(playerBody);

    Vector2 p = playerBody.getPosition();
    pixelScreen.begin(batch, p.x * PPM, p.y * PPM);   // camera follows player
    drawMap();
    for (Ship s : ships) drawShip(s);
    drawBullets();
    drawParticles();
    pixelScreen.end(batch);

    // optional: crisp HUD at window resolution goes here, after end()
}

@Override public void resize(int width, int height) { pixelScreen.resize(); }
```

## Drawing a rotated sprite

```java
void drawShip(Ship s) {
    float deg = s.body.getAngle() * MathUtils.radiansToDegrees;  // CCW from +x, same as libGDX
    int px = MathUtils.round(s.body.getPosition().x * PPM);
    int py = MathUtils.round(s.body.getPosition().y * PPM);
    int half = SHIP_PX / 2;                                      // 18 for a 36 px sprite
    batch.draw(s.region, px - half, py - half, half, half, SHIP_PX, SHIP_PX, 1, 1, deg);
}
```

- Origin is the sprite's center so it rotates about the body position.
- Art must point along **+x** at 0°. If a sprite is drawn pointing up, use
  `deg - 90` (don't rotate the art file; keep one convention per atlas).
- Position is rounded **per sprite**. The camera is rounded separately in
  `PixelScreen.begin()`. For the player ship these round to the same value, so
  the ship never jitters against the screen center.
- If using `Sprite` instead of `batch.draw`: `setOriginCenter()`,
  `setRotation(deg)`, `setPosition(px - half, py - half)`. Same rules.

## Box2D integration

- Ship fixture is a **circle**. Collisions are rotation-symmetric, so Box2D's
  rotation is only used to carry the heading for drawing.
- Turning has no inertia (Subspace feel): set it directly rather than applying
  torque.
  ```java
  body.setAngularVelocity(turnInput * TURN_RATE_RAD);   // turnInput ∈ {-1, 0, 1}
  // bodyDef.angularDamping = 0; bodyDef.fixedRotation = false;
  ```
  Equivalent alternative: `fixedRotation = true` and keep a `float heading`
  on the ship; thrust uses the heading, draw uses it too.
- Thrust: `body.applyForceToCenter(cos(a) * THRUST, sin(a) * THRUST, true)`
  where `a = body.getAngle()`.
- Box2D has no top speed. Clamp after each step:
  ```java
  Vector2 v = body.getLinearVelocity();
  if (v.len2() > MAX_SPEED * MAX_SPEED) body.setLinearVelocity(v.setLength(MAX_SPEED));
  ```
- `PPM` (pixels per meter) lives in `Constants`. Everything physics-side is
  meters; the only place `× PPM` and rounding happen is the draw code and the
  camera call.

## Asset rules

- Ship sprites: 36×36 px cells (Subspace scale). Each cell keeps at least a
  1–2 px fully transparent margin; the hull does not touch the cell edge.
- Texture atlas settings (`pack.json` for TexturePacker):
  `"filterMin": "Nearest", "filterMag": "Nearest", "paddingX": 2, "paddingY": 2`.
  Transparent padding between regions prevents stray pixels when nearest
  sampling lands on the boundary of a rotated quad.
- Individual `Texture` loads: `tex.setFilter(Nearest, Nearest)` immediately.
- Outlines **2 px** if they must survive rotation; 1 px if shimmer is desired.
- Flat, mostly symmetric shading. Avoid a strong single light direction.
- Art faces +x. Palette-style flat colors, no anti-aliasing in the source.
- Don't author at a higher resolution and downscale; author at native size.

## Camera rules

- `worldCam.zoom` is always `1`. Zooming in/out, if ever wanted, means changing
  `VIRTUAL_W/H` (a different `PixelScreen`), not the zoom field.
- Camera position is rounded to integers in `PixelScreen.begin()`. Pass it
  virtual-pixel coordinates (`meters × PPM`).
- Don't use `FitViewport` / `ExtendViewport` / `ScalingViewport` for the world
  camera. They scale fractionally. `PixelScreen` owns the scaling.
- `Gdx.graphics.getWidth()/getHeight()` are logical sizes and can be wrong on
  HiDPI. `PixelScreen.resize()` uses `getBackBufferWidth()/Height()` for a
  reason.
- Scrolling: because everything is integer-snapped, slow objects move in 1 px
  steps. That is expected and matches the period look.

## What goes inside the buffer

Everything that is part of the game world: map tiles, ships, bullets, mines,
particles, explosions, prizes, names-over-ships if we have them. Mixed
resolutions (low-res ships, high-res particles) are the fastest way to break
the look.

HUD text may be drawn **after** `pixelScreen.end()` at window resolution if it
should be crisp, or inside the buffer if it should look like the original.
Decide once, then be consistent.

## Tuning knobs

| Knob | Where | Effect |
|---|---|---|
| `VIRTUAL_W/H` | `Constants` | Field of view and how chunky the window scale is. 640×480 ≈ original. Keep even. |
| Sprite size | art | Bigger sprites shimmer less at the same shape. 36 px is deliberately crunchy. |
| Outline width | art | 1 px = breaks up (shimmer), 2 px = stable. |
| `HEADING_SNAP_DEG` | draw code | `0` = continuous (current). `9` = Subspace-style 40 steps. Implement as `if (SNAP > 0) deg = MathUtils.round(deg / SNAP) * SNAP;` right before the draw call. Physics is untouched either way. |
| Window scale | `PixelScreen` | Always an integer; nothing to tune, but check letterboxing on odd window sizes. |

## Checklist when reviewing rendering changes

- [ ] Nothing draws to the window between `pixelScreen.begin()` and `end()`
      except through the batch bound to `worldCam`.
- [ ] Every `batch.draw` / `Sprite` position is an `int` (rounded), width and
      height are the region's native size, scale is 1.
- [ ] Rotated draws use the sprite center as origin.
- [ ] New textures/atlases are `Nearest`/`Nearest` with transparent padding.
- [ ] `worldCam.zoom == 1`, no `Viewport` classes applied to `worldCam`.
- [ ] Resize uses back-buffer dimensions; scale is integer; leftover is
      letterboxed.
- [ ] Screenshot a ship at ~27° and ~45°: ship pixels and background pixels are
      the same size and on the same grid; edges are hard, not soft.

## Tooling

`tools/rotation_demo.py` (Pillow) reproduces the renderer's math offline. To
preview how a sprite will look at each angle before putting it in the game:

```python
# in rotation_demo.py, replace make_ship() with:
ship = Image.open("path/to/ship.png").convert("RGBA")   # 36x36, transparent bg
```

then run it. `rotation_comparison.png` middle row = what the FBO will produce.
`rotation_demo.gif` panel 2 = the look in motion.

## Background: why this is period-correct

Runtime nearest-neighbor rotation is inverse-mapped texture sampling with
fixed-point stepping — the same technique as early-90s rotozoomers, Zone 66's
rotating playfield (1993), SNES Mode 7, and Doom's wall columns. A 36 px sprite
costs ~30k cycles per frame on a 486, i.e. it was affordable; games of the era
mostly pre-drew ship frames for 286 compatibility, VGA bus budget, and artist
control, not because rotation was expensive. So the look says "a DOS game that
spent its cycles on the ships."
