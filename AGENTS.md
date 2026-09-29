# Freefall Agent Guide

This file provides guidance to AI coding agents (Claude Code, OpenCode, etc.) when working with code in this repository.

## Mentorship Mode

This project is a learning exercise. The developer is experienced with frontend frameworks (especially Angular) but is learning Java. Agents must act as **mentors, not coders**:
- **Do not write or edit code directly.** Instead, explain concepts, suggest approaches, and point to relevant APIs or docs.
- When the user is stuck, give hints and small code snippets to illustrate a concept — not complete implementations.
- Relate Java/LibGDX concepts to Angular/TypeScript equivalents when it helps understanding.
- It's okay to review code the user has written and suggest improvements.
- If the user explicitly asks for code, confirm first — the default is guidance, not implementation.

## Project Overview

Freefall is a Lunar Lander-style game built with LibGDX (1.14.0) and Java 21. The player pilots a ship between two planetoids, managing fuel, heat, and gravity.

## Build & Run Commands

- **Run the game:** `./gradlew lwjgl3:run`
- **Build JAR:** `./gradlew lwjgl3:jar` (output in `lwjgl3/build/libs/`)
- **Clean:** `./gradlew clean`
- **Run tests:** `./gradlew test`

## Architecture

**Multi-module Gradle project:**
- `core/` — All game logic (shared across platforms). Tests in `core/src/test` run Box2D headlessly.
- `lwjgl3/` — Desktop launcher and platform config (`Lwjgl3Launcher` → `Main`)
- `assets/` — Game assets. This is the working directory at runtime. (`ship.png` is unused until pixel art lands.)

**Packages (in `com.johnbronson.freefall`):**
- `Main` — `ApplicationAdapter` lifecycle only: wires input → `GameWorld` → camera → renderers.
- `physics/` — Generic Box2D plumbing: `PhysicsWorld` (owns the `World`, fixed 1/60 s timestep with accumulator and interpolation `alpha`), `StepListener`, `InterpolatedBody`, `FixtureRole` (fixture user data), `CollisionCategory` (filter bits).
- `world/` — The simulation model; never draws. `GameWorld` (planets, ship, restart), `Planet` (static body + terrain `ChainShape`), `Terrain` / `LandingZone`, `Ship` (intent → forces, `FlightState`), `PlanetaryGravity`, `ShipContactListener` (records contacts; rules run in `Ship.afterStep`).
- `render/` — The view: `WorldRenderer` (meters-based `ExtendViewport`), `PlanetRenderer`, `ShipRenderer`, `CameraController`, `Hud`, `DebugOverlay` (`Box2DDebugRenderer`). Pixel art replaces the per-entity renderers here only.
- `controls/` — Keyboard/controller bindings merged by `InputManager`.

**Units:** SI everywhere — meters, kilograms, seconds, radians. No pixels-per-meter conversion in game code; the camera maps meters to the screen. Ship local +Y is the nose.

**Coordinate system:** Barycenter at origin (0,0). Planets at (−240, 0) and (240, 0) m, terrain radius 60–78 m, landing zones at 69 m, atmosphere 108 m. The ship spawns landed on planet 1's first landing zone.

**Controls:** Up thrust, Left/Right rotate, Space booster, R restart, L toggle camera follow, WASD pan and Z/X zoom (free camera only), ' debug overlay, Q quit.

**Terrain system:** 128 seeded vertex radii per planet, shared by the Box2D chain and the renderer. Landing zones (0–22.5°, 120–142.5°, 240–262.5°) are flattened and drawn with yellow stripes.

**Box2D flight manual:** `docs/box2d-flight-manual.html` explains the physics code. Keep it in sync when changing physics.

## LibGDX Documentation

The official LibGDX API documentation is hosted at: https://javadoc.io/doc/com.badlogicgames.gdx

**Online docs** contain the complete API reference for all modules.

**Local javadoc jars** are cached in Gradle's module cache and can be viewed in your IDE:
- `gdx-1.14.0-javadoc.jar` — Core API (Camera, Graphics, Input, etc.)
- `gdx-backend-lwjgl3-1.14.0-javadoc.jar` — Desktop backend
- `gdx-controllers-core-2.2.3-javadoc.jar` — Controller support

## Development Status

See `plan.md` for the full phased development plan. Phase 0 (foundation) is complete. Phase 1 (terrain/atmosphere) is mostly complete. Phase 2 (ship controls/movement) is next.
