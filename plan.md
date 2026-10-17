# Complete Development Phases

**Visual inspiration:** *Teleglitch* and *Deep Space Exploitation* — chunky, low-resolution pixels with freely rotating sprites and a restricted palette. Details are in "Art Direction: Chunky Pixel Rendering" below.

## Phase 0: Foundation ✓ (COMPLETED!)

- [x] Set up world coordinate system with barycenter at origin (0,0)
- [x] Create camera system that views world space at a specific zoom level
- [x] Render two simple circles at fixed world positions relative to barycenter
- [x] Add basic keyboard controls to manually pan the camera (for testing)
- [x] Test: Can you move the camera around and see both planets at different positions?

## Phase 1: Terrain & Atmosphere Zones

- [x] Generate random terrain as height values around each planetoid's circumference
- [x] Implement algorithm to create flat landing zones (minimum 3-4 per planet)
- [x] Render terrain sprites/polygons on each planet
- [x] Define atmosphere radius for each planet (larger than planet radius)
- [x] Add visual rendering for atmosphere (semi-transparent gradient/circle)
- [x] Add debug visualization: color-code flat zones, show atmosphere boundaries
- [x] Test: Do the landing zones look reasonable? Can you visually identify them?

## Phase 2: Ship Rendering, Orientation & Basic Controls

Rebuilt on Box2D (2026-10): the ship is a dynamic body with a circular hull fixture. Thrust and the booster are forces along local +Y, and rotation is a torque-driven attitude controller (rate-limited "RCS"). Placeholder art is an outlined triangle in `ShipRenderer`.

- [x] Add lander sprite at a starting position on planet 1's surface (spawns landed on planet 1's first landing zone)
- [x] Ship has facing direction/angle (for heat shield orientation)
- [x] Implement rotation controls (left/right arrows rotate the ship)
- [x] Implement thrust (up arrow) - applies force in direction ship is facing
- [x] Render thrust flame/particle effect when thrusting (main engine and booster flames)
- [ ] Visual indicator for heat shield direction (maybe different colored nose). *Lost in the Box2D move: the hull is one color.*
- [ ] Add velocity vector debug display (arrow showing ship's current velocity). *Lost in the Box2D move. `DebugOverlay` draws Box2D shapes but not velocity; `Box2DDebugRenderer` has a `drawVelocities` flag.*
- [x] Test: Can you thrust around and rotate? Does the ship respond predictably? (`ShipFlightTest` covers lift-off and attitude hold)

## Phase 3: Gravity, Fuel & Atmospheric Drag

Box2D's world gravity is zero. `PlanetaryGravity` applies Newtonian gravity toward every planet as a force before each step (`GameWorld.beforeStep`). Drag fits the same slot: a force applied before each step, computed from a pure function of position and velocity, which keeps it easy to unit-test. (The trajectory predictor stops at the atmosphere, so it never needs drag.) Don't use Box2D's `linearDamping`; it applies everywhere and ignores the atmosphere. Heat and overheat checks are game rules, so they go in `Ship.afterStep` and reuse `crash(reason)`.

- [x] Implement gravity from both planetoids (inverse-square law, `PlanetaryGravity`; tested in `PlanetaryGravityTest`)
- [x] Add fuel counter to HUD
- [x] Thrust consumes fuel
- [ ] Detect when ship enters atmosphere zone (distance check from planet center)
- [ ] Apply atmospheric drag when in atmosphere (slows ship down)
- [ ] Calculate heat buildup based on velocity and angle relative to direction of travel
- [ ] Heat shield protection: reduce heat if ship's heat shield is facing the velocity vector
- [ ] Add heat meter to HUD
- [ ] Ship destruction if heat exceeds maximum
- [ ] Add debug toggles: gravity on/off, infinite fuel, invincibility (no heat damage)
- [ ] Test: Does gravity feel strong enough to matter but not overwhelming? Is fuel consumption rate reasonable?

## Phase 3.5: Physics Refactor & Trajectory Prediction

Goal: draw a "coast" line ahead of the ship showing where it will go under gravity (and later drag) if the player stops thrusting.

### Refactor ✓ (superseded by the Box2D restructure, 2026-10)

The original plan was a hand-rolled `PhysicsState` plus a `PhysicsWorld.step(state, dt)`. Moving to Box2D got the same goals a different way:

- [x] Fixed timestep: `PhysicsWorld` accumulates frame time and steps Box2D in constant 1/60 s chunks, capped at 0.25 s per frame to avoid the spiral of death. The leftover fraction is `alpha`, and `InterpolatedBody` blends the last two transforms when drawing.
- [x] Gravity is a pure function: `PlanetaryGravity.accelerationAt(point, out)` writes into a caller-supplied `Vector2` and touches no Box2D state.
- [x] Planets are a collection (`Array<Planet>` in `GameWorld`), so more gravity sources are cheap to add.
- [x] One fixed order per step, through the `StepListener` hooks: save transforms for interpolation → ship forces and gravity (`beforeStep`) → `world.step` → game rules (`afterStep`).
- [x] Data and behavior split: Box2D bodies hold the state; `Ship` turns pilot intent into forces; `GameWorld` wires it together. `Main` only does lifecycle.

Ownership now:

```
Main             lifecycle: input → GameWorld → camera → renderers
GameWorld        has-a PhysicsWorld, Array<Planet>, PlanetaryGravity, Ship; is the StepListener
PhysicsWorld     owns the Box2D World and the fixed-timestep accumulator
PlanetaryGravity accelerationAt(point, out) (pure) and applyTo(world) (forces)
Ship             has-a Body; intent → forces; FlightState rules in afterStep
```

### Predictor (not started)

A space-only coast line. Decided 2026-10-17:

- **In an atmosphere: draw nothing.** Close to the ground the player flies by eye.
- **In space: draw until the path either covers a set distance or touches an atmosphere.**

A Box2D `World` can't be cloned and stepped ahead without moving the real ship, so the predictor can't call `world.step`. Instead it integrates a scratch position and velocity itself. Because the line only covers space, gravity (plus any booster burn) is the only force. No drag, no contacts and no terrain ray casts are needed: every atmosphere (108 m) sits above the highest terrain (78 m), so a path always reaches the air before it could reach the ground. The predictor doesn't depend on Phase 3's drag and can be built before or after it.

- [ ] Add a `TrajectoryPredictor` class (separate responsibility from `Ship`). Input: ship position and velocity. Output: a reused `FloatArray` of predicted x,y pairs (empty when nothing should be drawn).
- [ ] If the ship is inside any planet's atmosphere (`Planet.isInAtmosphere`), clear the points and stop.
- [ ] Otherwise copy the ship's position and velocity into scratch `Vector2`s and repeat steps of `PhysicsWorld.TIME_STEP`: `v += a·dt`, then `x += v·dt`. That is semi-implicit Euler, the same integrator Box2D uses, so with the same dt and the same forces the line matches the real flight.
- [ ] Acceleration comes from `PlanetaryGravity.accelerationAt`. Prediction assumes no player thrust; include remaining booster burn, since that fires regardless of input.
- [ ] Stop when either:
    - the predicted point is inside any planet's atmosphere (`isInAtmosphere`), or
    - the path length so far (sum of each step's segment length) reaches the distance limit. Pick a starting value by eye, e.g. the planet separation (480 m).
- [ ] Also cap the number of steps. A nearly stationary ship would take forever to cover the distance limit.
- [ ] Draw the path with `ShapeRenderer` in a single `begin`/`end` block. Fade alpha along the line or dot every other segment.
- [ ] Expose the **atmosphere entry point** (where the line stopped on an atmosphere, if it did) for a future HUD marker. That's where the heat shield needs to face forward.
- [ ] Use the raw Box2D position and velocity as input (it's a physics prediction), but note the line starts up to one step behind the interpolated ship drawing. Start it at the interpolated position if the gap is visible.
- [ ] Zero allocation per frame: no `new Vector2` / `new ArrayList` inside the loop. Verify with `Gdx.app.getJavaHeap()` on the debug HUD (flat, not sawtooth).
- [ ] Measure cost with `System.nanoTime()` around the predict call; show rolling avg + max on debug HUD. Ignore the first few seconds (JIT warmup). Target: well under 1ms.
- [ ] Test: with hands off the controls, does the ship follow the line exactly? Does the line update sensibly when rotating/thrusting? A headless test in the style of `ShipFlightTest` can compare the prediction against real steps. Compare with a small tolerance: Box2D is native C++ and the order of float operations can differ slightly from Java.
- [ ] Known limit: Box2D caps movement at 2 m per step (`b2_maxTranslation`), 120 m/s at 60 Hz. Above that the real ship is clamped and the line runs ahead of it. Orbital speeds here are around 28 m/s, so this shouldn't come up; assert it in the test.
- [ ] Later (level 4, landing clearance): a longer horizon so the player can see whether an orbit closes.

### Notes for drag (Phase 3)

Drag depends on velocity, not position, and opposes it. With a large step it can overshoot and reverse velocity, causing jitter or instability. Clamp the per-step drag so it never removes more speed than the ship has (force × dt / mass ≤ speed). The `density * speed` product computed for drag is also the input for heat buildup. The trajectory predictor doesn't model drag (it stops at the atmosphere), so drag doesn't need to be a pure function for its sake, though keeping it one still makes it easy to unit-test.

## Phase 4: Collision Detection ✓ (via Box2D)

- [x] Implement collision detection between ship and terrain. Each planet is a static body with a terrain `ChainShape`; Box2D's continuous collision keeps the ship from tunneling. Filter bits in `CollisionCategory`, fixture roles in `FixtureRole`.
- [x] Ship crashes on a bad touchdown: `CRASHED` state, red hull, reason shown on the HUD.
- [x] Add collision bounds debug visualization (`DebugOverlay` with `Box2DDebugRenderer`, toggled with ')
- [x] Detect when ship is "grounded" vs "flying" (`FlightState` `FLYING` / `LANDED` / `CRASHED`; `ShipContactListener` counts ground contacts and records the hardest hit, `Ship.afterStep` decides)
- [x] Test: `ShipFlightTest` runs Box2D headlessly (gentle, fast and tilted touchdowns, resting without flicker, lift-off)
- [ ] Hull is a single circle. Landing legs (−Y) as extra fixtures would make touchdown read better and let tilt matter more physically.

## Phase 5: Landing Success Criteria

- [ ] Implement landing validation:
    - [x] Low velocity (`MAX_LANDING_SPEED`, 10 m/s impact speed)
    - [x] Near-vertical angle (`MAX_LANDING_TILT`, 15° from the planet's local up)
    - [ ] On a flat landing zone. `judgeTouchdown` doesn't check this yet, so a gentle touchdown on rough terrain counts as landed. Use the touchdown point's angle around the planet against the `LandingZone` ranges.
- [ ] Also check that ship isn't overheated
- [ ] Add success/failure feedback (text display, color change, sound effect slot). Crash text and red hull exist; nothing celebrates a landing yet, and there's no sound slot.
- [x] Add respawn/reset functionality after crash or successful landing (R → `GameWorld.requestRestart`, applied outside the step)
- [ ] Track which planet you last landed on (for fuel station unlock)
- [ ] Test: Can you reliably land? Is it too easy or too hard?

## Phase 6: Smooth Zoom

- [ ] Implement continuous zoom based on distance to nearest planet
- [ ] Define min/max zoom levels (e.g., zoomed out for traveling between planets, zoomed in for landing)
- [ ] Smooth zoom interpolation using lerp or similar easing function
- [ ] Calculate target zoom level based on distance to nearest planet (closer = more zoomed in)
- [ ] Add smooth transition speed parameter to control how quickly zoom responds
- [ ] Consider using a non-linear curve (exponential/logarithmic) for more natural feel
- [ ] Ensure HUD elements remain visible/scaled appropriately across all zoom levels
- [ ] Optional: Add minimum zoom change threshold to prevent micro-adjustments
- [ ] Test: Does the zoom transition feel smooth? Is it responsive but not jarring? Does it help or hinder gameplay?

## Phase 7: Space Station Docking & Basic Audio

- [ ] Add collision detection for docking with station orbiting planet 1 (one tagged fixture per docking bay; see "Art Direction: Chunky Pixel Rendering")
- [ ] Implement docking criteria - must match station's velocity (or get close enough) plus low relative velocity and proximity
- [ ] Docking refills fuel AND repairs heat damage/cools ship
- [ ] Add visual feedback for successful dock
- [ ] Consider adding velocity-matching indicators in HUD to help player
- [ ] NEW: Add LibGDX Music class for background music
- [ ] NEW: Load lofi music track
- [ ] NEW: Music plays continuously (will add state-based switching in Phase 10)
- [ ] Test: Can you reach the station? Is docking too hard/easy?

## Phase 8: Orbital Motion

With Box2D, the planets switch from static to **kinematic** bodies, moved by setting their velocity each step (never `setTransform`, which teleports them and breaks contacts). `PlanetaryGravity` already reads `planet.getCenter()`, so that needs to track the body's position. A landed ship rides along through friction.

- [ ] Calculate and apply orbital motion to both planetoids around barycenter
- [ ] Start with slow, easily visible orbital speed
- [ ] Update all rendering to account for moving planets
- [ ] Implement orbital motion for space station around planet 1
- [ ] Station's orbital speed should be consistent with orbital mechanics (faster orbit = closer to planet)
- [ ] Atmosphere moves with the planet
- [ ] Camera logic needs to handle moving reference frames
- [ ] Test: Does everything still work? Any coordinate system bugs revealed?

## Phase 9: Planetoid Rotation

- [ ] Add rotation to each planetoid around its own center (`setAngularVelocity` on the kinematic body)
- [ ] Collision needs no manual world/local transform: the terrain chain is a fixture on the planet body, so Box2D rotates it. What does need updating is everything that assumes a fixed planet angle: landing-zone checks (use `Body.getLocalPoint` to get the touchdown angle in planet-local space), the `PlanetRenderer`, and landing speed (judge it relative to the ground's velocity, `Body.getLinearVelocityFromWorldPoint`).
- [ ] This is where complexity spikes - expect bugs
- [ ] Landing zones are now moving targets
- [ ] Atmosphere rotates with the planet
- [ ] Test: Can you still land? Is rotation speed balanced with gameplay?

## Phase 9.5: Space Station

- [ ] Position space station sprite in orbit around planet 1 (drawn through the low-res framebuffer; see "Art Direction: Chunky Pixel Rendering")
- [ ] Station orbits with the moving/rotating planet system
- [ ] Test: Does the station move consistently with the rest of the world?

## Phase 10: Polish, Tuning & Dynamic Music

- [ ] Implement music state system - detect when ship is "cruising" (far from planets, in space)
- [ ] Smooth crossfade between music tracks based on game state:
    - [ ] Cruising music: Lofi/chill when between planets in space
    - [ ] Tension music: More intense when near planet/in atmosphere/low fuel
    - [ ] Landed music: Quiet/ambient when safely landed
- [ ] Add sound effects: thrust, atmospheric whoosh, heat warning beeps, landing/crash, docking
- [ ] Add particle effects (heat glow when entering atmosphere, burn marks, exhaust trails)
- [ ] UI improvements (better heat/fuel gauges, prettier HUD)
- [ ] Balance fuel consumption, gravity strength, orbital/rotation speeds, atmospheric drag, heat buildup rates
- [ ] Tune heat shield effectiveness angle (how precise do you need to be?)
- [ ] Add win condition (land on planet 2 after starting on planet 1)
- [ ] Add restart/menu functionality
- [ ] Victory screen
- [ ] Test: Play the full game loop multiple times

## Phase 11: Difficulty Levels & Ship Types (ideas, 2026-10-17)

Builds on everything above. Each level adds one new skill on top of the last.

- [ ] **Level structure.** A level is a set of rules and limits (starting fuel, which objectives apply, which ship). Consider a small immutable config object per level that `GameWorld` is built from, rather than `if (level == 2)` checks spread through the code. Track progress: completing a level unlocks the next.
- [ ] **Level 1: First descent.** Start docked at the space station and land on either planet. Teaches undocking, deorbiting and landing without needing a take-off first. Depends on the station (Phase 7 / 9.5), including undocking (destroying the dock joint).
- [ ] **Level 2: Basic landing.** The current game: two planets, take off from planet 1, land on planet 2.
- [ ] **Level 3: Fuel and docking.** Starting fuel is restricted so the trip can't be made on one tank. The player must dock with the space station (Phase 7) to refuel on the way.
- [ ] **Level 4: Landing clearance.** Before entering planet 2's atmosphere, the player must request landing clearance. Clearance requires a stable orbit: staying within a distance band around planet 2 (outside the atmosphere) for a set time. Entering the atmosphere without clearance fails the level, or at least warns.
    - Open questions: what counts as "an orbit" (time spent in the band, or a full 360° sweep around the planet)? Is clearance a key press or automatic once the orbit holds? Does clearance name a specific landing zone?
    - The trajectory predictor (Phase 3.5) would help the player see whether their orbit is closed.

### Alternative ship type (sketch)

- [ ] **Glider / lifting body.** No lift-off boosters, but it generates **lift** in the atmosphere, so it can glide and trade speed for altitude.
    - Requires modeling **angle of attack**: the angle between the ship's nose and its velocity. Heat already needs that same angle (Phase 3), so compute it in one place.
    - Lift acts perpendicular to the velocity, scaled by air density × speed² × a lift curve of angle of attack (rises up to a stall angle, then drops). Box2D has no aerodynamics, so lift is a force applied before each step, like drag. Like drag, it only acts in the atmosphere, so the trajectory predictor never needs it.
    - Ship type as data: thrust, fuel, boosters, lift and drag coefficients, hull shape. A level config picks the ship.
    - Gameplay questions: how does it take off without boosters (runway roll on a landing zone? a launch from the station?) and is it a level of its own or a player choice?

## Art Direction: Chunky Pixel Rendering (design notes, 2026-10-10, updated 2026-10-11)

Strategy agreed in discussion; nothing here is implemented yet. Visual references: *Deep Space Exploitation* (procedural, noise-filled rocks drawn in chunky screen-aligned pixels, even when rotated) and *Teleglitch* (tiny, abstract sprites rotating freely on a very coarse grid).

### Decisions

- **No destruction in Freefall.** Planet and station shapes are fixed once generated, so physics stays as it is (Box2D bodies, terrain `ChainShape` from the seeded radii). The art is generated once and never rebuilt at runtime.
- **The chunky look comes from a low-res framebuffer**, not from the assets: render the world into a small `FrameBuffer` with `Nearest` filtering, then draw it to the screen at an integer scale. Anything rotated is re-rasterized onto the coarse screen grid, which is why DSE's edges stay screen-aligned on rotated rocks. The HUD is drawn separately.
  - **Starting size: 640×480.** It is 4:3, matching the 80×60 m minimum view, so it gives 8 px per metre at zoom 1.
  - The window is currently 640×480 too (`Lwjgl3Launcher`), which would be 1:1 and not chunky. Show it at 2× (1280×960) or 3× (1920×1440, exactly the height of the 3440×1440 dev monitor; pillarboxed if fullscreen).
  - Gotchas: the framebuffer's colour texture comes out vertically flipped, and the viewport must be applied against the framebuffer's size (not the window's) while drawing into it.
- **Target is the modern Teleglitch/DSE look, not an authentic 1993 VGA game.** That means:
  - Free rotation through the framebuffer; no pre-baked facings.
  - No palette-index shader and no palette cycling. Assets are ordinary RGBA PNGs.
  - The palette is a style discipline enforced at authoring time: draw in indexed colour mode with one shared palette file kept in the repo. Aim for 16–32 colours built as a few ramps, not 256. Optional: a JUnit test that loads each PNG as a `Pixmap` and asserts every opaque pixel is in the palette.
  - Effects may leave the palette (alpha, tinting, additive glow for re-entry heat and thruster flame).
- **Ship sprite starts at 12×12 px**, drawn at 8 px per metre to match the current 1.5 m tall, 1.2 m wide ship. Expect an icon: silhouette, a differently coloured nose for the heat shield, leg stubs, 3–4 colours. No 1 px details that must survive rotation (they drop out under `Nearest`).
  - The sprite's resolution must match its drawn size at the closest zoom. Larger wastes detail and shimmers; smaller turns texels into fat rotated squares.
  - At cruise zoom (4×) the ship is about 3 px; swap it for a simple marker at that distance if needed.
- **Art tool: Pixelorama** (free, open source; Flatpak `com.orama_interactive.Pixelorama`, hidden in Mint's Software Manager because the Flathub listing is unverified, though it is the developer's own package). Indexed mode exists since v1.1. Keep the `.pxo` project files in git alongside the exported PNGs. Aseprite (~$20) is the upgrade if the tooling becomes limiting.
- **Planet art is a cell grid baked to a texture, once per planet:**
  - Shape mask comes from the same `Terrain` radii the chain shape uses (cell is solid if its distance from the centre is less than the terrain radius at its angle), so art and collision cannot drift apart.
  - Colour comes from fractal noise (several octaves of simplex/Perlin) quantized into 3–4 palette bands, with per-cell random jitter before quantizing to get speckled band edges.
  - Landing-zone stripes are painted into the same grid.
  - Seeded like the terrain. LibGDX has no noise built in; FastNoiseLite (single Java file) is one option. Write into a `Pixmap`, upload as a `Texture`.
- **Ship and station are ordinary sprites** drawn through the same framebuffer. Their physics stays polygon fixtures; no per-pixel collision.
- **Station docking bays are identified by fixture, not by pixel lookup:**
  - Hull is a set of polygon fixtures on one body (kinematic once it orbits in Phase 8).
  - Each docking bay is a "garage": solid walls (part of the hull fixtures) around a square interior, with a door across the opening.
  - Each bay has two sensor fixtures tagged through `FixtureRole` user data, carrying the bay's identity: a large **approach** sensor outside the door (opens it) and a **capture** sensor inside.
  - A sensor only reports overlap, not "fully inside". Either inset the capture sensor so that touching it implies the ship is well inside, or test the ship's position in station-local coordinates against the bay rectangle.
  - Docking rule: ship in the capture zone AND speed *relative to the station* below a threshold, held for N seconds (count fixed steps; reset the timer if either condition breaks). Relative speed matters once the station orbits: compare against the station's velocity at the ship's position (`Body.getLinearVelocityFromWorldPoint`).
  - Capture: once the timer completes, attach the ship to the station (e.g. a `WeldJoint`) so it rides along; destroy the joint to undock.
  - Door: its own solid fixture (or small kinematic body that slides) which stops blocking when open, plus its own sprite frames layered over the hull sprite. Change fixtures only after the step, never in a contact callback.
  - The contact listener only records sensor begin/end; the rules above run after the step, same pattern as `ShipContactListener` → `Ship.afterStep`.
  - If the exact touch point is ever needed: take the contact's world manifold point and convert with `Body.getLocalPoint`.
  - Alternative considered: a second "ID mask" image aligned with the station art, looked up at the contact point. Worth it only if there are many irregular regions; overkill for a few bays.
- All of this lives in `render/` except the docking-bay fixtures (`world/`).

### Open decisions

- **Cell size in metres.** Drives everything. Planet terrain is up to 156 m across: 0.25 m/cell is about 624 cells (fits a 1024 texture), 0.5 m/cell about 312. Pick it from how many pixels wide the ship should be on screen. With the 640×480 framebuffer at 8 px/m, a 1:1 match is 0.125 m/cell, about 1248 cells, which needs a 2048 texture; 0.25 m/cell gives 2×2 framebuffer pixels per cell at zoom 1.
- **Is a 12 px ship enough?** Decide by looking at it rotating through the framebuffer. If it needs to be bigger, the options are:
  - Approach zoom of 0.75 (45 m view): about 16 px.
  - Double the ship to 3 m (retune `HULL_RADIUS`, mass, thrust): about 24 px. Both together: about 32 px.
  - Two-stage zoom: ease in further only when the ship is slow and close to the ground.
  - Zooming in alone cannot reach 32 px at 1.5 m: the view would be 22.5 m tall, narrower than a landing zone (about 27 m), and crossed in about 2 s at the 10 m/s landing limit.
  - Upper bound: keep the ship to roughly 8–15% of the framebuffer height when landing, so the terrain ahead stays visible.
- **Zoom.** Smooth zoom (Phase 6) makes art texels shimmer as the texel-to-screen-pixel ratio passes through non-integers. Options: snap zoom to integer steps, or accept it (rotation already resamples every frame, so it may be tolerable). Decide by trying it.

### Suggested order

1. Low-res framebuffer with the current renderers (small change, most of the look).
2. Noise-filled planet textures in `PlanetRenderer`.
3. Ship sprite.
4. Station sprite and docking-bay fixtures (with Phase 7 / 9.5).

### If destruction is ever revisited

DSE cuts rocks one pixel at a time, which points to the Noita-style approach: a cell grid per body in **body-local space** as the source of truth, with texture and fixtures both derived from it.

- Hit: world point → `Body.getLocalPoint` → cell index → clear cell. The grid data itself never rotates.
- Rebuild: marching squares → Douglas–Peucker simplify → triangulate (`EarClippingTriangulator`) → replace fixtures. Batch to at most once per physics step, per dirty body, never inside a contact callback.
- Fragments: flood fill for detached islands; each becomes a new body with its own grid, the parent's angle, and the parent's velocity at that point.
- Tiny islands become particles (Box2D rejects very small polygons). Planet-sized grids need chunking.
- Reference: GDC talk "Exploring the Tech and Design of Noita".
- Rejected for this: polygon boolean clipping (cannot do per-pixel cuts without vertex counts exploding) and Voronoi fracture (shatters, cannot carve).

## Debug Tools to Build Along the Way

- [ ] F1: Toggle gravity on/off
- [ ] F2: Toggle infinite fuel
- [ ] F3: Toggle collision on/off
- [x] ' (apostrophe): Toggle debug overlay (Box2D shapes, fps, controller state). Velocity vectors and zones aren't drawn yet.
- [ ] F4 (or separate key): Toggle trajectory prediction line
- [ ] F5: Slow motion (0.25x speed)
- [ ] F6: Teleport ship to planet 2
- [ ] F7: Refill fuel
- [x] R: Reset to starting position (restart)
- [x] L: Toggle camera follow; WASD pan and Z/X zoom in free camera
- [ ] F9: Toggle heat damage on/off (invincibility)
- [ ] F10: Clear heat meter (instant cooldown)
- [ ] F11: Toggle atmosphere visualization
- [ ] F12: Mute/unmute music (for testing)

## Music Implementation Notes
LibGDX Audio:

- [ ] Use Music class for background music (streams from file, good for long tracks)
- [ ] Use Sound class for sound effects (loaded into memory, good for short clips)

Music State Detection:

- [ ] "Cruising" = distance from both planets > atmosphere radius + some buffer
- [ ] "Near planet" = within zoom-in range or atmosphere
- [ ] "Critical" = low fuel, overheating, or crash imminent

File Format:

- [ ] OGG format recommended (smaller file size, good quality, works on all platforms)
- [ ] MP3 also works but OGG is preferred for LibGDX
