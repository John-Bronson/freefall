# Complete Development Phases
## Phase 0: Foundation ✓ (COMPLETED!)

- [x] Set up world coordinate system with barycenter at origin (0,0)
- [x] Create camera system that views world space at a specific zoom level
- [x] Render two simple circles at fixed world positions relative to barycenter
- [x] Add basic keyboard controls to manually pan the camera (for testing)
- [x] Test: Can you move the camera around and see both planets at different positions?

## Phase 1: Terrain & Atmosphere Zones

- [ ] Generate random terrain as height values around each planetoid's circumference
- [ ] Implement algorithm to create flat landing zones (minimum 3-4 per planet)
- [ ] Render terrain sprites/polygons on each planet
- [ ] Define atmosphere radius for each planet (larger than planet radius)
- [ ] Add visual rendering for atmosphere (semi-transparent gradient/circle)
- [ ] Add debug visualization: color-code flat zones, show atmosphere boundaries
- [ ] Test: Do the landing zones look reasonable? Can you visually identify them?

## Phase 2: Ship Rendering, Orientation & Basic Controls ✓

- [x] Add lander sprite at a starting position on planet 1's surface
- [x] Ship has facing direction/angle (for heat shield orientation)
- [x] Implement rotation controls (left/right arrows rotate the ship)
- [x] Implement thrust (up arrow) - applies force in direction ship is facing
- [x] Render thrust flame/particle effect when thrusting
- [x] Visual indicator for heat shield direction (maybe different colored nose)
- [x] Add velocity vector debug display (arrow showing ship's current velocity)
- [x] Test: Can you thrust around and rotate? Does the ship respond predictably?

## Phase 3: Gravity, Fuel & Atmospheric Drag

- [ ] Implement gravity from both planetoids (inverse-square law or simplified)
- [ ] Add fuel counter to HUD
- [ ] Thrust consumes fuel
- [ ] Detect when ship enters atmosphere zone (distance check from planet center)
- [ ] Apply atmospheric drag when in atmosphere (slows ship down)
- [ ] Calculate heat buildup based on velocity and angle relative to direction of travel
- [ ] Heat shield protection: reduce heat if ship's heat shield is facing the velocity vector
- [ ] Add heat meter to HUD
- [ ] Ship destruction if heat exceeds maximum
- [ ] Add debug toggles: gravity on/off, infinite fuel, invincibility (no heat damage)
- [ ] Test: Does gravity feel strong enough to matter but not overwhelming? Is fuel consumption rate reasonable?

## Phase 3.5: Physics Refactor & Trajectory Prediction

Goal: draw a "coast" line ahead of the ship showing where it will go under gravity (and later drag) if the player stops thrusting. The predictor must run the *exact same* physics code as the real ship, on a throwaway copy of the state, so the line is deterministic.

### Refactor summary (do this first)

The current physics is split across `Ship.applyGravity`, `Ship.update`, and the call order in `Main.render`, and every piece mutates `Ship`'s fields directly. That makes it impossible to "fast forward" without moving the real ship. The refactor:

- [ ] Introduce a small mutable **physics state** type holding `x, y, vx, vy` (and `angle`, `solidBoostTimer` if boost is included in prediction). `Ship` owns one instance. Consider a `copyFrom(other)` method so the predictor can reuse a scratch instance instead of allocating.
- [ ] Extract gravity into a **pure function** that computes acceleration at an arbitrary point and writes it into a caller-supplied `Vector2` (no `new` per call). Signature shape: `gravityAt(px, py, planet, Vector2 out)`. Neither `Ship` nor `Planet` fields are mutated by it.
- [ ] Decide where `step` lives. **Decision: a `PhysicsWorld` class.** It owns the list of planets (and later the station / any other gravity source) and exposes `step(PhysicsState state, float dt)`. This mirrors Box2D's `World.step()` convention and keeps data (`PhysicsState`) separate from behavior (`PhysicsWorld`). `Main` creates one instance and passes it to whatever needs it (ship, predictor); no static singleton.
- [ ] `step` does, in fixed order: sum gravity from all bodies, add boost acceleration if timer > 0, apply drag (Phase 3), update velocity, then update position (semi-implicit Euler). `Ship.update` becomes a thin call to `world.step(state, dt)`. `Main` no longer calls `applyGravity` and `update` separately.
- [ ] `PhysicsWorld` stores planets as a collection (`Array<Planet>` or `List<Planet>`), not `planet1, planet2` fields, so adding bodies later (station, Phase 7+) is free.
- [ ] Switch the game to a **fixed timestep**: accumulate `getDeltaTime()` and call `step` in constant chunks (e.g. 1/120s). The predictor uses the same constant. See Glenn Fiedler, "Fix Your Timestep". This is what makes the prediction match the real flight exactly.
- [ ] Test: ship flies identically before and after the refactor. Gravity, boost, and thrust feel unchanged.

Ownership after the refactor (dependencies point one way, from user to used):

```
Main                 creates everything, wires it together, owns the fixed-timestep accumulator
PhysicsWorld         has-a List<Planet>;  does step(PhysicsState, dt), gravityAt(...)
Ship                 has-a PhysicsState;  delegates update to world.step
TrajectoryPredictor  has-a PhysicsWorld, a scratch PhysicsState, a FloatArray of points
```

### Predictor

- [ ] Add a `TrajectoryPredictor` class (separate responsibility from `Ship`). Constructed with a `PhysicsWorld` reference. Input: ship state. Output: a reused `FloatArray` of predicted x,y pairs.
- [ ] Each frame: copy the ship's state into a scratch state, call `world.step` N times with the fixed dt, record position after each step. N ≈ seconds-ahead / dt (start with 5s).
- [ ] Prediction assumes no player thrust. Include remaining solid boost, since that fires regardless of input.
- [ ] Stop early when the predicted point hits a planet. First pass: distance to center < radius. Later: look up terrain height at the point's mil angle around the planet (Phase 4 reuses this for real collision).
- [ ] Draw the path with `ShapeRenderer` in a single `begin`/`end` block. Fade alpha along the line or dot every other segment.
- [ ] Expose the predicted impact point (if any) for a future HUD marker.
- [ ] Zero allocation per frame: no `new Vector2` / `new ArrayList` inside the loop. Verify with `Gdx.app.getJavaHeap()` on the debug HUD (flat, not sawtooth).
- [ ] Measure cost with `System.nanoTime()` around the predict call; show rolling avg + max on debug HUD. Ignore the first few seconds (JIT warmup). Target: well under 1ms.
- [ ] Test: with hands off the controls, does the ship follow the line exactly? Does the line update sensibly when rotating/thrusting?

### Notes for drag (Phase 3)

Drag depends on velocity, not position, and opposes it. With a large step it can overshoot and reverse velocity, causing jitter or instability. Keep the predictor's dt equal to the game's dt (a correctness requirement, not just accuracy), and clamp the per-step drag impulse so it never removes more speed than the ship has. The `density * speed` product computed for drag is also the input for heat buildup.

## Phase 4: Collision Detection

- [ ] Implement collision detection between ship and terrain (per-planet local space)
- [ ] Ship "crashes" (stops moving, simple visual feedback) on collision
- [ ] Add collision bounds debug visualization
- [ ] Detect when ship is "grounded" vs "flying"
- [ ] Test: Does collision work at different angles? Any gaps or false positives?

## Phase 5: Landing Success Criteria

- [ ] Implement landing validation: check if ship is on flat zone, low velocity, near-vertical angle
- [ ] Also check that ship isn't overheated
- [ ] Add success/failure feedback (text display, color change, sound effect slot)
- [ ] Add respawn/reset functionality after crash or successful landing
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

- [ ] Add collision detection for docking with station orbiting planet 1
- [ ] Implement docking criteria - must match station's velocity (or get close enough) plus low relative velocity and proximity
- [ ] Docking refills fuel AND repairs heat damage/cools ship
- [ ] Add visual feedback for successful dock
- [ ] Consider adding velocity-matching indicators in HUD to help player
- [ ] NEW: Add LibGDX Music class for background music
- [ ] NEW: Load lofi music track
- [ ] NEW: Music plays continuously (will add state-based switching in Phase 10)
- [ ] Test: Can you reach the station? Is docking too hard/easy?

## Phase 8: Orbital Motion

- [ ] Calculate and apply orbital motion to both planetoids around barycenter
- [ ] Start with slow, easily visible orbital speed
- [ ] Update all rendering to account for moving planets
- [ ] Implement orbital motion for space station around planet 1
- [ ] Station's orbital speed should be consistent with orbital mechanics (faster orbit = closer to planet)
- [ ] Atmosphere moves with the planet
- [ ] Camera logic needs to handle moving reference frames
- [ ] Test: Does everything still work? Any coordinate system bugs revealed?

## Phase 9: Planetoid Rotation

- [ ] Add rotation to each planetoid around its own center
- [ ] Update collision detection to transform between world space and rotating local space
- [ ] This is where complexity spikes - expect bugs
- [ ] Landing zones are now moving targets
- [ ] Atmosphere rotates with the planet
- [ ] Test: Can you still land? Is rotation speed balanced with gameplay?

## Phase 9.5: Space Station

- [ ] Position space station sprite in orbit around planet 1
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

## Debug Tools to Build Along the Way

- [ ] F1: Toggle gravity on/off
- [ ] F2: Toggle infinite fuel
- [ ] F3: Toggle collision on/off
- [ ] F4: Toggle debug overlays (vectors, bounds, zones)
- [ ] F4 (or separate key): Toggle trajectory prediction line
- [ ] F5: Slow motion (0.25x speed)
- [ ] F6: Teleport ship to planet 2
- [ ] F7: Refill fuel
- [ ] F8: Reset to starting position
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
