package com.johnbronson.freefall.world;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.johnbronson.freefall.physics.PhysicsWorld;

/**
 * Predicts where the ship will coast over the next {@link #PREDICTION_SECONDS} if the
 * pilot lets go of the controls. The result is a list of points, one per physics step,
 * that {@code TrajectoryRenderer} draws as a dotted line.
 *
 * <h2>Why not just run Box2D ahead?</h2>
 * A Box2D {@code World} can't be copied. Stepping the real one forward would move the
 * real ship. So this class does its own tiny simulation of a single point, using the
 * same forces and the same update rule Box2D uses. Same inputs, same math, same
 * answer, so a hands-off ship follows the line.
 *
 * <h2>The update rule: semi-implicit Euler</h2>
 * Motion under changing forces usually has no neat formula. A simulation gets around
 * that by chopping time into small steps ({@code dt} = 1/60 s) and, for each step,
 * pretending the acceleration stays constant:
 * <pre>
 *   velocity += acceleration × dt     // forces change the velocity first...
 *   position += velocity × dt         // ...then the NEW velocity moves the ship
 * </pre>
 * A worked example with made-up round numbers (not the game's tuned values), ignoring
 * drag: a ship falls at 10 m/s, and gravity is 20 m/s². One step adds 20 × (1/60)
 * ≈ 0.33 m/s, so it now falls at 10.33 m/s, and it moves 10.33 × (1/60) ≈ 0.17 m that
 * step. Repeat {@link #STEP_COUNT} times and you've covered {@link #PREDICTION_SECONDS}.
 *
 * <p>Using the new velocity in the second line is what makes it "semi-implicit". It's
 * what Box2D does, and it keeps orbits stable. The plain version, which uses the old
 * velocity, adds a little energy every step, and orbits slowly spiral outward.
 *
 * <h2>Forces included</h2>
 * <ul>
 *   <li>Gravity from every planet: {@link PlanetaryGravity#accelerationAt}.</li>
 *   <li>Air drag: {@link Ship#dragDeltaV}, the same function the real ship uses.</li>
 *   <li>The booster, if it's still burning. Once lit it can't be stopped, so it fires
 *       whatever the pilot does. Its direction is held at the ship's current heading.</li>
 * </ul>
 * The main engine and steering are left out on purpose: the line answers the question
 * "where do I end up if I let go now?".
 *
 * <h2>Memory</h2>
 * {@link #predict} runs every frame, so it must not create objects. Every {@code new}
 * leaves garbage behind, and when Java's garbage collector stops to clean it up the
 * game can stutter. So every {@code Vector2} this class needs is created once, as a
 * field, and reused. (In TypeScript you rarely think about this; in a 60 fps game loop
 * you have to.)
 */
public final class TrajectoryPredictor {
    // `final class` means no other class can extend this one. It's a habit for
    // classes that weren't designed to be subclassed.

    /**
     * How far ahead the line looks, in seconds of game time. Tune freely: the step
     * count, the renderer's sweep speed and the tests all derive from it.
     *
     * <p>{@code public static final} is Java's way to write a constant: {@code static}
     * means one copy shared by the whole class (not one per object), {@code final} means
     * it can't be reassigned. The closest TypeScript is a {@code static readonly} field.
     */
    public static final float PREDICTION_SECONDS = 5f;

    /**
     * Number of physics steps in the prediction: the horizon divided by the step
     * length. At 60 steps per second, each second of prediction is 60 steps.
     *
     * <p>{@code Math.round(float)} returns an {@code int}, which is what a step count
     * should be. Float division isn't exact: a result that "should" be a whole number
     * can come out a hair below it (like 299.99997), so we round. Casting with
     * {@code (int)} would just chop off the decimals and could come up one step short.
     */
    public static final int STEP_COUNT = Math.round(PREDICTION_SECONDS / PhysicsWorld.TIME_STEP);

    /**
     * The ship's nose direction in its own frame: straight up its local +Y axis.
     * {@code private}: only this class uses it.
     */
    private static final Vector2 NOSE_DIRECTION = new Vector2(0f, 1f);

    // Inputs, handed over by GameWorld. `final` on a field means it always refers to
    // the same object, like `readonly` in TypeScript. The object itself can still
    // change: GameWorld's planet list is the same list, but planets could be added.
    //
    // Array<Planet> is libGDX's list type. The <Planet> part is a generic, the same
    // idea as Array<Planet> in TypeScript: it says what the list holds.
    private final Array<Planet> planets;
    private final PlanetaryGravity gravity;

    /**
     * The output: the x and y of every predicted point, packed side by side as
     * {@code [x0, y0, x1, y1, ...]}. Point 0 is the ship now; point i is i steps
     * (i/60 seconds) ahead.
     *
     * <p>Why not a list of Vector2s? {@code FloatArray} is a libGDX list of plain
     * {@code float}s stored in one block of memory. Java's {@code ArrayList<Float>}
     * can't hold plain numbers: it would wrap ("box") each one in a separate
     * {@code Float} object, which is slower and makes garbage.
     *
     * <p>The constructor argument is the starting capacity: room for every point (point
     * 0 plus one per step) × 2 floats each, so the list never has to grow.
     */
    private final FloatArray points = new FloatArray(2 * (STEP_COUNT + 1));

    // Extra results for future HUD markers. Each boolean says whether its point is valid.
    private boolean hitsGround;
    private final Vector2 impactPoint = new Vector2();
    private boolean entersAtmosphere;
    private final Vector2 atmosphereEntryPoint = new Vector2();

    // Scratch vectors: the pretend ship's state, plus space for in-between results.
    // They're overwritten on every predict() call.
    private final Vector2 position = new Vector2();
    private final Vector2 velocity = new Vector2();
    private final Vector2 acceleration = new Vector2();
    private final Vector2 dragChange = new Vector2();
    private final Vector2 boosterDirection = new Vector2();

    /**
     * Constructor: Java's version of TypeScript's {@code constructor(...)}. It has the
     * same name as the class and no return type.
     *
     * <p>No access modifier means <em>package-private</em>: only classes in this
     * package ({@code world}) can call it, and {@link GameWorld} is the only one that
     * does. Code elsewhere gets the predictor from {@link GameWorld#predictTrajectory()}.
     */
    TrajectoryPredictor(Array<Planet> planets, PlanetaryGravity gravity) {
        // `this.planets` is the field; plain `planets` is the parameter. Same as in TS.
        this.planets = planets;
        this.gravity = gravity;
    }

    /**
     * Recomputes the line from the ship's current state. Results are read with the
     * getters below and stay valid until the next call.
     */
    public void predict(Ship ship) {
        // Start from a clean slate. clear() empties the list but keeps its memory, so
        // the add() calls below don't need to allocate anything.
        points.clear();
        hitsGround = false;
        entersAtmosphere = false;

        // A landed ship is held up by the ground, which we don't simulate, so its line
        // would sink straight into the pad. A crashed ship has nowhere to go. Either
        // way, draw nothing. `Ship.FlightState.FLYING` is an enum value; enums in Java
        // work much like TypeScript enums, and `!=` compares them safely.
        if (ship.getState() != Ship.FlightState.FLYING) {
            return;
        }

        Body body = ship.getBody();

        // Copy the ship's state into our scratch vectors.
        //
        // In Java, a variable holding an object holds a *reference* to it, like a JS
        // object variable. `position = body.getPosition()` would make `position` point
        // at Box2D's own vector, and moving our pretend ship would then corrupt it
        // (or our field would change under us on the next step). set() instead copies
        // the two numbers into our own vector.
        //
        // These are the raw Box2D values: the state after the latest physics step.
        // The drawn ship lags slightly behind them (see InterpolatedBody), so the
        // renderer shifts the whole line to meet it.
        position.set(body.getPosition());
        velocity.set(body.getLinearVelocity());

        // Box2D works out mass from the fixture's density × area. It's very close to
        // Ship.HULL_MASS, but using Box2D's own number means the booster's
        // acceleration below matches the real one exactly.
        float mass = body.getMass();
        float dt = PhysicsWorld.TIME_STEP;

        // getWorldVector turns a direction in the ship's own frame into world
        // coordinates, i.e. rotates (0, 1) by the ship's angle θ, giving (−sin θ, cos θ).
        // (Rotating any (x, y) by θ gives (x·cos θ − y·sin θ, x·sin θ + y·cos θ); put
        // x = 0, y = 1 into that and you get the line above.)
        // Box2D reuses the vector it returns, so copy it out straight away.
        boosterDirection.set(body.getWorldVector(NOSE_DIRECTION));

        // A `float` is a primitive: assigning it copies the number itself, not a
        // reference. So counting this local copy down doesn't touch the real ship's
        // booster.
        float boosterTimeLeft = ship.getBoosterTimeLeft();

        // Newton's second law, F = m × a, solved for a = F / m: the booster's push as
        // an acceleration, in m/s². A heavier ship gets less acceleration from the
        // same thrust.
        float boosterAcceleration = Ship.BOOSTER_THRUST / mass;

        // Remember whether we start in the air, so we only report an atmosphere
        // *entry* (space → air), not a ship that was already inside.
        boolean wasInAtmosphere = isInAnyAtmosphere(position);

        // Point 0: where the ship is right now. FloatArray.add(x, y) appends two floats.
        points.add(position.x, position.y);

        // A classic C-style for loop, the same as TypeScript's
        // `for (let step = 1; step <= STEP_COUNT; step++)`. One pass per physics step.
        for (int step = 1; step <= STEP_COUNT; step++) {

            // ---- 1. Forces, from the state at the START of the step -------------------
            // Box2D works the same way: Ship.beforeStep and PlanetaryGravity.applyTo
            // both read the body before world.step() moves it, so every force in one
            // step is based on the same position and velocity.

            // The air belongs to whichever planet is closest.
            Planet planet = Planet.nearestTo(planets, position);
            float airDensity = planet.airDensityAt(position);

            // Drag comes back as a ready-made change in velocity (Δv, "delta v") for this
            // step, not as an acceleration. That's why it's added straight to the
            // velocity below without multiplying by dt: dragDeltaV already did that.
            // It's computed from the velocity BEFORE gravity changes it, the same as in
            // the real step, where all forces are worked out first and applied together.
            Ship.dragDeltaV(velocity, airDensity, dt, dragChange);

            // Gravity's pull at this point, in m/s², summed over all planets. Writes
            // into `acceleration` (and returns it, but we don't need the return value).
            gravity.accelerationAt(position, acceleration);

            if (boosterTimeLeft > 0f) {
                // Same bookkeeping as Ship.applyBooster: count down, and push this step.
                // `-=` subtracts in place, as in TypeScript.
                boosterTimeLeft -= dt;
                // mulAdd(v, s) means "this += v × s", done separately on x and y:
                //   acceleration.x += boosterDirection.x × boosterAcceleration
                //   acceleration.y += boosterDirection.y × boosterAcceleration
                // boosterDirection has length 1, so this adds exactly
                // boosterAcceleration along the nose.
                acceleration.mulAdd(boosterDirection, boosterAcceleration);
            }

            // ---- 2. Semi-implicit Euler (see the class comment) ---------------------
            // Vector2 methods change the vector they're called on and return it, so
            // they can be chained. This line reads, left to right:
            //   velocity = velocity + dragChange + acceleration × dt
            velocity.add(dragChange).mulAdd(acceleration, dt);
            // position = position + velocity × dt, using the velocity we just updated.
            position.mulAdd(velocity, dt);

            points.add(position.x, position.y);

            // ---- 3. Stop conditions ----------------------------------------------------

            // altitudeOf is the distance from the planet's center minus the terrain's
            // radius straight below the point. The round hull touches the ground when
            // its center is one HULL_RADIUS above it. Look the ground up on the planet
            // nearest the NEW position, since the ship just moved.
            Planet below = Planet.nearestTo(planets, position);
            if (below.altitudeOf(position) <= Ship.HULL_RADIUS) {
                hitsGround = true;
                // Copy, don't share: `position` keeps changing on the next predict().
                impactPoint.set(position);
                // `break` leaves the for loop early, like in TypeScript. The line ends
                // here, short of the full horizon.
                break;
            }

            // Edge detection: "in the air now, but not on the previous step" means this
            // step crossed the boundary. That's the spot where a future HUD marker will
            // tell the pilot to point the heat shield forward. `!entersAtmosphere`
            // keeps only the first crossing, if the path dips in and out.
            boolean inAtmosphere = isInAnyAtmosphere(position);
            if (inAtmosphere && !wasInAtmosphere && !entersAtmosphere) {
                entersAtmosphere = true;
                atmosphereEntryPoint.set(position);
            }
            wasInAtmosphere = inAtmosphere;
        }
    }

    /**
     * True if {@code point} is inside any planet's air. A {@code private} helper: only
     * this class calls it.
     */
    private boolean isInAnyAtmosphere(Vector2 point) {
        // for-each loop over the list, like TypeScript's `for (const planet of planets)`.
        for (Planet planet : planets) {
            // Returning from inside the loop stops it early; no need to check the rest.
            if (planet.isInAtmosphere(point)) return true;
        }
        return false;
    }

    // ---- Results ---------------------------------------------------------------
    // Getters, Java's version of TypeScript's `get` accessors. Java has no property
    // syntax, so the convention is a method named getX() (or isX()/hasX() for booleans).
    // They hand out the internal objects, not copies, so callers must only read them.

    /**
     * The predicted points as {@code [x0, y0, x1, y1, ...]}. Point i is
     * {@code i × PhysicsWorld.TIME_STEP} seconds ahead. Read-only, and overwritten by
     * the next {@link #predict}. Empty when the ship isn't flying.
     */
    public FloatArray getPoints() {
        return points;
    }

    /**
     * Number of points: half the number of floats, since each point is an x and a y.
     * {@code size} is a public field on libGDX's lists, not a method.
     */
    public int getPointCount() {
        return points.size / 2;
    }

    /** True if the line ends because it reached the ground within the time limit. */
    public boolean hitsGround() {
        return hitsGround;
    }

    /** Where the hull would touch the ground. Only meaningful when {@link #hitsGround()}. */
    public Vector2 getImpactPoint() {
        return impactPoint;
    }

    /** True if the line crosses from space into an atmosphere. */
    public boolean entersAtmosphere() {
        return entersAtmosphere;
    }

    /** Where the line first enters an atmosphere. Only meaningful when {@link #entersAtmosphere()}. */
    public Vector2 getAtmosphereEntryPoint() {
        return atmosphereEntryPoint;
    }
}
