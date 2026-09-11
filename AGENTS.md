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
- `core/` — All game logic (shared across platforms)
- `lwjgl3/` — Desktop launcher and platform config (`Lwjgl3Launcher` → `Main`)
- `assets/` — Game assets (`ship.png`, `libgdx.png`). This is the working directory at runtime.

**Key classes (all in `com.johnbronson.freefall`):**
- `Main` — Game entry point (`ApplicationAdapter`). Owns the camera, planets, ship, and game loop.
- `Planet` — Generates terrain and landing zones. Draws itself using `ShapeRenderer` (semi-transparent atmosphere circle, filled triangles for terrain, lines for landing zone callouts).
- `Ship` — Lander with position, angle, velocity, max speed, and acceleration. Renders `ship.png` via `SpriteBatch`.
- `Constants` — Angular measurement system using military mils (6400 mils = 360°). Conversion factors between mils, radians, and degrees.

**Coordinate system:** World coordinates with barycenter at origin (0,0). Planets are at (-400, 0) and (400, 0). Camera is an `OrthographicCamera` with manual pan (WASD) and zoom (Z/X). Left arrow rotates the ship; Q quits.

**Terrain system:** Each planet stores 6400 terrain height values (one per mil). Terrain is rendered as triangles from planet center to adjacent surface points. Landing zones are flat sections at fixed mil ranges (0-400, 2133-2533, 4267-4667) drawn with yellow line overlays.

## Development Status

See `plan.md` for the full phased development plan. Phase 0 (foundation) is complete. Phase 1 (terrain/atmosphere) is mostly complete. Phase 2 (ship controls/movement) is next.
