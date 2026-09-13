# Controller Support: Why the 8BitDo Ultimate 2C Wasn't Detected

**Date:** 2026-09-12
**Status:** Root cause identified and both candidate fixes verified end-to-end on this machine.
**Scope:** The uncommitted gdx-controllers work (`gdxControllersVersion=2.0.1`, `Main implements ControllerListener`, debug HUD on `'`).

---

## 1. Summary (start here)

Your Java code was never the problem. `Controllers.addListener(this)` in `create()` is correct and sufficient
to initialize the backend, and your `ControllerListener` implementation would have received events.

The pad was invisible one layer down, in SDL:

> gdx-controllers' desktop backend (Jamepad → SDL2) reports **only devices SDL recognizes as a
> *game controller*** — i.e. devices it has a **mapping** for. A joystick with no mapping is silently
> skipped: it is not counted, not opened, and generates no button events.

Your dependency stack ships a **2020-vintage SDL** and a **2021 mapping database**. The 8BitDo Ultimate 2C
is a much newer device, absent from both. AntiMicroX sees the pad because it reads raw evdev/joydev, which
needs no mapping at all — so "AntiMicroX works" was never evidence that SDL would.

### What to do

Do **both** of these:

1. **Download a current `gamecontrollerdb.txt` to `assets/gamecontrollerdb.txt`.**
   Source: <https://github.com/mdqinc/SDL_GameControllerDB> (raw file: `gamecontrollerdb.txt`).
   No code change. Works even on your current 2.0.1.
2. **Bump `gdxControllersVersion` from `2.0.1` to `2.2.3`** in `gradle.properties`.
   No code change — `ControllerListener` is identical in 2.2.x.

Either one alone fixes *detection* (both proven below). Do both because:

- The upgrade gets you a modern SDL, whose Linux backend can auto-generate a mapping from evdev capabilities.
- The current DB gets you a *correct* mapping for your exact device. SDL's auto-generated mapping is where the
  known 8BitDo Z/RZ axis mix-up bites (right stick behaving as triggers) — see
  [SDL #12219](https://github.com/libsdl-org/SDL/issues/12219).

### How you'll know which mapping source won

Check the controller name your debug HUD prints:

| Name reported | Mapping came from |
|---|---|
| `8BitDo Ultimate 2C` | the `gamecontrollerdb.txt` entry (what you want) |
| `8BitDo Ultimate 2C Wireless Controller` | SDL's auto-generated Linux mapping (kernel device name) |

### Expectations when you test

- `connected()` fires shortly after startup **even for a pad plugged in before launch** — the monitor discovers
  it on its first pass. Watch stdout on launch, not just on button presses.
- Your Thrustmaster TWCS Throttle occupies SDL joystick **index 0**; the pad is at **index 1**. Don't assume
  index 0 is the gamepad.

---

## 2. Environment as measured

| Item | Value |
|---|---|
| OS / session | Linux 7.0.0-31-generic, X11 (`DISPLAY=:0`) |
| JDK | OpenJDK 21.0.12 |
| Pad | `8BitDo Ultimate 2C Wireless Controller` |
| USB ID | `2dc8:310a` |
| Kernel input version | `0114` |
| Device nodes | `/dev/input/event9`, `/dev/input/js1` |
| by-id | `usb-8BitDo_8BitDo_Ultimate_2C_Wireless_Controller_3F8D7200C8-{joystick,event-joystick}` |
| Extra HID interfaces | keyboard `event10`, mouse `event11` (the pad exposes these too) |
| Other joystick present | `Thrustmaster TWCS Throttle` → `event2`, `js0` |

Access permissions were checked and are **fine** (this was not a permissions problem):

- `/dev/input/event9` — ACL grants `user:bronson:rw-`
- `/dev/input/js1` — ACL grants `user:bronson:rw-`
- The pad's hidraw nodes `/dev/hidraw8` and `/dev/hidraw10` are mode `0666` (`root:users`)

---

## 3. The dependency stack you had

From the uncommitted diff:

```properties
# gradle.properties
gdxControllersVersion=2.0.1
```

```groovy
// core/build.gradle
api "com.badlogicgames.gdx-controllers:gdx-controllers-core:$gdxControllersVersion"
// lwjgl3/build.gradle
implementation "com.badlogicgames.gdx-controllers:gdx-controllers-desktop:$gdxControllersVersion"
```

Resolved runtime graph:

```
gdx-controllers-desktop:2.0.1
 +--- gdx-controllers-core:2.0.1
 \--- com.badlogicgames.jamepad:jamepad:2.0.12.1
```

Critical dates and contents:

| Artifact | Native / file date | `8BitDo Ultimate 2C` entries |
|---|---|---|
| `jamepad:2.0.12.1` natives (`libjamepad64.so`, bundled SDL) | **2020-12-19** | 0 (no `8BitDo Ultimate` strings at all) |
| `gamecontrollerdb.txt` inside `gdx-controllers-desktop:2.0.1` | **2021-01-14**, 862 lines | **0** |
| `gamecontrollerdb.txt` inside `gdx-controllers-desktop:2.2.3` | 2023-04-27, 1596 lines | **0** (only `Ultimate Wired` / `Ultimate Wireless`) |
| `jamepad:2.0.20.0` natives (what 2.2.3 pulls) | 2022-04-20 | 0 by string, but SDL auto-maps on Linux |
| `jamepad:2.30.0.0` natives (newest) | 2024-03-06 | 0 by string, but SDL auto-maps on Linux |
| Upstream `SDL_GameControllerDB` today | current, 2287 lines | **10**, incl. your exact GUID |

Version availability at time of writing: `gdx-controllers-desktop` → `2.0.0, 2.0.1, 2.1.0, 2.2.0, 2.2.1, 2.2.2, 2.2.3`;
`jamepad` → `2.0.12.1, 2.0.14.{0,1,2}, 2.0.20.0, 2.26.4.0, 2.26.5.0, 2.30.0.0`.
`gdx-controllers-desktop:2.2.3` depends on `jamepad:2.0.20.0`; `2.2.0` depends on `jamepad:2.0.14.1`.

---

## 4. Your device's GUID and mapping line

SDL identifies devices by a 32-hex-char GUID built from little-endian bus/vendor/product/version:

```
03000000 c82d0000 0a310000 14010000
   |        |        |        |
   |        |        |        +-- version 0x0114  ✔ matches /proc/bus/input/devices
   |        |        +----------- product 0x310a  ✔ 8BitDo Ultimate 2C
   |        +-------------------- vendor  0x2dc8  ✔ 8BitDo
   +----------------------------- bus     0x0003  ✔ USB
```

The matching `platform:Linux` entry in the current upstream DB — **this is the exact line your setup needs**:

```
03000000c82d00000a31000014010000,8BitDo Ultimate 2C,a:b0,b:b1,back:b6,dpdown:h0.4,dpleft:h0.8,dpright:h0.2,dpup:h0.1,guide:b8,leftshoulder:b4,leftstick:b9,lefttrigger:a2,leftx:a0,lefty:a1,rightshoulder:b5,rightstick:b10,righttrigger:a5,rightx:a3,righty:a4,start:b7,x:b2,y:b3,platform:Linux,
```

Note `lefttrigger:a2` / `righttrigger:a5` (analog axes) and `rightx:a3` / `righty:a4` — this is the assignment
SDL's auto-generated mapping is prone to getting wrong.

---

## 5. Mechanism: exactly where the device got dropped

### 5.1 Initialization path (your code is fine)

`gdx-controllers-core` → `Controllers.java`: every public entry point (`getControllers`, `addListener`,
`removeListener`, …) calls `initialize()` first, which for `ApplicationType.Desktop` reflectively instantiates
`com.badlogic.gdx.controllers.desktop.JamepadControllerManager`.

**Consequence:** your `Controllers.addListener(this)` in `create()` is enough to bring the backend up. You did
not need to call `getControllers()` first, and the fact that your debug HUD only polls when debug mode is on
did not matter.

`JamepadControllerManager` constructor:

```java
if (jamepadConfiguration == null) jamepadConfiguration = new com.studiohartman.jamepad.Configuration();
controllerManager = new com.studiohartman.jamepad.ControllerManager(jamepadConfiguration);
controllerManager.initSDLGamepad();
JamepadControllerMonitor monitor = new JamepadControllerMonitor(controllerManager, compositeListener);
monitor.run();
Gdx.app.addLifecycleListener(new JamepadShutdownHook(controllerManager));
Gdx.app.postRunnable(monitor);   // monitor re-posts itself every frame
```

`JamepadControllerMonitor.run()` → `controllerManager.update()`, `checkForNewControllers()`, `update()`, then
re-posts itself. `checkForNewControllers()` loops `0 .. maxNumControllers-1` (default **4**), and for each index
that `isConnected()` and isn't already known, fires `listener.connected(controller)`. **This is why `connected()`
fires for a pad that was already plugged in at launch.**

### 5.2 Jamepad / SDL layer (where it broke)

`ControllerManager` default constructor is `this(4, "/gamecontrollerdb.txt")` — the DB is loaded as a **classpath
resource**.

`initSDLGamepad()`:

1. `SDL_Init(SDL_INIT_EVENTS | SDL_INIT_JOYSTICK | SDL_INIT_GAMECONTROLLER)`
2. drains all pending SDL events (`while (SDL_PollEvent(&event));`)
3. `addMappingsFromFile(mappingsPath)` → `SDL_GameControllerAddMappingsFromFile`.
   If the resource is missing it prints
   `Failed to load mapping with original location "/gamecontrollerdb.txt", Falling back of SDL's built in mappings`
   and continues.
4. constructs `ControllerIndex[i]` for `i < 4`, each calling `SDL_GameControllerOpen(i)`.

The two filters that hid your pad:

```c
/* getNumControllers() */
for (i = 0; i < SDL_NumJoysticks(); i++)
    if (SDL_IsGameController(i)) numGamepads++;      /* false without a mapping */
```

```java
/* ControllerIndex.isConnected() */
return controllerPtr != 0 && nativeIsConnected(controllerPtr);
/* controllerPtr == 0 when SDL_GameControllerOpen() returned NULL */
```

**Failure chain:** no mapping for `2dc8:310a` → `SDL_IsGameController` false → count `0`, `SDL_GameControllerOpen`
returns NULL → `isConnected()` false → `checkForNewControllers()` never fires `connected()` → your HUD shows
`Controllers found: 0` and no button logs. Exactly the symptom you reported.

---

## 6. Experimental evidence

All experiments were run on this machine, in a scratchpad directory, without modifying the project.

### 6.1 Raw Jamepad probe (`ControllerManager` directly)

| Jamepad | `/gamecontrollerdb.txt` on classpath | `getNumControllers()` | Result |
|---|---|---|---|
| 2.0.12.1 (yours) | none | **0** | not detected |
| 2.0.12.1 (yours) | **2021 DB, the one your build ships** | **0** | **not detected — reproduces your bug exactly** |
| 2.0.12.1 (yours) | current upstream DB | **1** | `8BitDo Ultimate 2C`, `connected=true` |
| 2.0.12.1 (yours) | none, but `SDL_GAMECONTROLLERCONFIG` set to the single line | **1** | detected |
| 2.0.20.0 | none | 1 | index 1 = `8BitDo Ultimate 2C Wireless Controller` |
| 2.0.20.0 | current DB | 1 | index 1 = `8BitDo Ultimate 2C` |
| 2.30.0.0 | none | 1 | index 1 = `8BitDo Ultimate 2C Wireless Controller` |
| 2.30.0.0 | current DB | 1 | index 1 = `8BitDo Ultimate 2C` |

Per-index detail on newer Jamepad (explains an early red herring):

```
numControllers(SDL_IsGameController count) = 1
  index 0: connected=false            <-- Thrustmaster TWCS Throttle (js0), not a gamepad
  index 1: connected=true name=8BitDo Ultimate 2C
  index 2: connected=false
  index 3: connected=false
lastNativeError = [Couldn't find mapping for device (3)]
```

### 6.2 End-to-end through the real `Controllers` API

A minimal `Lwjgl3Application` + `ApplicationAdapter implements ControllerListener`, run on the project's
**actual resolved runtime classpath** (60 entries, dumped via a Gradle init script — the project was not edited):

| Run | Stack | DB | `getControllers().size` | Name |
|---|---|---|---|---|
| **A** | yours (2.0.1 / jamepad 2.0.12.1) | bundled 2021 | **0** | — |
| **B** | yours (2.0.1 / jamepad 2.0.12.1) | current DB prepended | **1** | `8BitDo Ultimate 2C` |
| **C** | 2.2.3 / jamepad 2.0.20.0 | its own bundled 2023 DB | **1** | `8BitDo Ultimate 2C Wireless Controller` |
| **D** | 2.2.3 / jamepad 2.0.20.0 | current DB prepended | **1** | `8BitDo Ultimate 2C` |

Run A is your bug. Runs B, C and D are all working fixes. Size was identical at `create()` and after 120 frames,
so nothing here is a timing/warm-up issue.

### 6.3 Classpath ordering (why `assets/` wins over the jar's stale DB)

`lwjgl3/build.gradle` line:

```groovy
sourceSets.main.resources.srcDirs += [ rootProject.file('assets').path ]
```

Resolved `lwjgl3` runtime classpath, first entries in order:

```
1. lwjgl3/build/classes/java/main
2. lwjgl3/build/resources/main          <-- assets/ is copied here
3. gdx-backend-lwjgl3-1.14.0.jar
4. gdx-platform-1.14.0-natives-desktop.jar
5. core-1.0.0.jar
6. gdx-controllers-desktop-2.0.1.jar    <-- contains the stale 2021 gamecontrollerdb.txt
7. gdx-controllers-core-2.0.1.jar
...                                      (60 entries total)
```

`getResourceAsStream("/gamecontrollerdb.txt")` returns the **first** match, so a file at
`assets/gamecontrollerdb.txt` overrides the one bundled in the jar. Verified that `assets/` really is copied:
`ship.png`, `libgdx*.png` and `assets.txt` are all present in `lwjgl3/build/resources/main/`.

---

## 7. Ruled out (so you don't re-investigate these)

- **Device permissions** — ACLs grant your user `rw-` on both `event9` and `js1`.
- **hidraw permissions** — the pad's `hidraw8`/`hidraw10` are `0666`.
- **udev / xpad driver rules** — not needed; the fix is purely a mapping-database issue.
- **Listener wiring in `Main.java`** — correct as written.
- **Needing `getControllers()` to kick off initialization** — `addListener()` already does.
- **Controller mode switching on the pad** — not required for detection once a mapping exists.
- **`index 0` failing to open** — that's the Thrustmaster throttle, not your pad.

---

## 8. Override hooks worth knowing

Useful for quick experiments or if you ever ship your own mapping file:

- **Env var, zero code, great for a one-off test** (verified working on your current stack):
  ```bash
  SDL_GAMECONTROLLERCONFIG='03000000c82d00000a31000014010000,8BitDo Ultimate 2C,a:b0,...,platform:Linux,' ./gradlew lwjgl3:run
  ```
- **`JamepadControllerManager.addMappingsFromFile(String path)`** — static, delegates to
  `ControllerManager.addMappingsFromFile`.
- **`JamepadControllerManager.jamepadConfiguration`** — public static field; assign a
  `com.studiohartman.jamepad.Configuration` **before the first `Controllers` call** to change
  `maxNumControllers` (default 4), `useRawInput`, `loadNativeLibrary`, `loadDatabaseInMemory`.
- **`JamepadControllerManager.logLastNativeGamepadError()`** — prints SDL's own last message. This is what
  surfaced `Couldn't find mapping for device (3)`. Reach for it first next time something controller-shaped
  misbehaves.

---

## 9. Follow-ups and open items

- **Verify the packaged jar.** The `jar` task merges `configurations.runtimeClasspath` zip trees with
  `duplicatesStrategy = DuplicatesStrategy.EXCLUDE` (first copy wins). Project resources should land before the
  dependency jars, so `assets/gamecontrollerdb.txt` ought to win there too — but this was **only verified for
  `lwjgl3:run`**, not for `lwjgl3:jar`. Worth confirming before you distribute a build.
- **Don't hardcode button codes.** They vary by platform and pad. Use the mapping object:
  ```java
  if (buttonCode == controller.getMapping().buttonA) { /* ... */ }
  ```
  `JamepadMapping` exposes `buttonA/B/X/Y`, `buttonL1/R1`, `buttonStart/Back`, `axisLeftX/LeftY/RightX/RightY`, etc.
  (Same spirit as preferring `KeyboardEvent.key` over a raw `keyCode` on the web.)
- **Maintenance cost.** A vendored `gamecontrollerdb.txt` goes stale exactly the way 2.0.1's 2021 copy did. If a
  future pad isn't detected, refreshing that file is the first thing to try.
- **Axis sanity check once detected.** Confirm the triggers are analog and the right stick isn't driving them —
  the known 8BitDo failure mode ([SDL #12219](https://github.com/libsdl-org/SDL/issues/12219)).
- **Event vs. poll.** You're on the event path (`ControllerListener`). For continuous ship thrust/rotation,
  per-frame polling (`controller.getAxis(...)`, `controller.getButton(...)`) in `handleInput()` will fit your
  existing keyboard code better than accumulating event callbacks.

---

## 10. References

- gdx-controllers wiki — <https://github.com/libgdx/gdx-controllers/wiki>
- SDL_GameControllerDB — <https://github.com/mdqinc/SDL_GameControllerDB>
- SDL #12219, 8BitDo Ultimate 2C axis mapping — <https://github.com/libsdl-org/SDL/issues/12219>
- Jamepad — <https://github.com/libgdx/Jamepad>
