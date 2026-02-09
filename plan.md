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
- [ ] Position space station sprite in orbit around planet 1 (static for now, will move in Phase 8)
- [ ] Test: Do the landing zones look reasonable? Can you visually identify them?

## Phase 2: Ship Rendering, Orientation & Basic Controls

- [ ] Add lander sprite at a starting position on planet 1's surface
- [ ] Ship has facing direction/angle (for heat shield orientation)
- [ ] Implement rotation controls (left/right arrows rotate the ship)
- [ ] Implement thrust (up arrow) - applies force in direction ship is facing
- [ ] Render thrust flame/particle effect when thrusting
- [ ] Visual indicator for heat shield direction (maybe different colored nose)
- [ ] Add velocity vector debug display (arrow showing ship's current velocity)
- [ ] Test: Can you thrust around and rotate? Does the ship respond predictably?

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
