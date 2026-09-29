package com.johnbronson.freefall.world;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.World;
import com.johnbronson.freefall.physics.CollisionCategory;
import com.johnbronson.freefall.physics.FixtureRole;
import com.johnbronson.freefall.physics.InterpolatedBody;

/**
 * The player's lander.
 *
 * <p>The pilot never moves the ship directly. Input sets an <em>intent</em> (throttle,
 * turn, booster). Before each physics step the ship turns that intent into forces and
 * torques, and Box2D does the moving. After the step, {@link #afterStep()} decides
 * whether a touchdown was a landing or a crash.
 *
 * <p>Local frame: +Y is the nose, so thrust pushes along +Y. Sprites are drawn
 * upright, and future landing legs will hang off −Y.
 */
public final class Ship {

    public enum FlightState { FLYING, LANDED, CRASHED }

    // Hull. A circle is Box2D's cheapest and most stable shape.
    public static final float HULL_RADIUS = 0.6f;
    public static final float HULL_MASS = 1000f;
    private static final float HULL_FRICTION = 0.3f;
    /** 0 = stops dead on impact, 1 = bounces back at full speed. */
    private static final float HULL_RESTITUTION = 0.1f;

    // Engines. Thrust is a force in newtons, so a heavier ship accelerates less.
    // 25 kN on 1000 kg is 25 m/s², about 1.4× gravity at landing-zone height.
    public static final float MAIN_ENGINE_THRUST = 25_000f;
    /** Seconds of full-throttle burn. */
    public static final float MAIN_FUEL_CAPACITY = 500f;
    public static final float BOOSTER_THRUST = 15_000f;
    public static final float BOOSTER_BURN_TIME = 2f;
    public static final int BOOSTER_CHARGES = 1;

    // Attitude control ("RCS thrusters").
    private static final float MAX_TURN_RATE = MathUtils.PI2;
    /** Reaches full turn rate in a quarter second. */
    private static final float MAX_TURN_ACCELERATION = 4f * MathUtils.PI2;

    // Landing rules.
    public static final float MAX_LANDING_SPEED = 10f;
    public static final float MAX_LANDING_TILT = 15f * MathUtils.degRad;

    private static final Vector2 NOSE_DIRECTION = new Vector2(0f, 1f);

    private final Body body;
    private final InterpolatedBody hull;

    // Pilot intent, set once per frame by input and used by every step in that frame.
    private float throttle;
    private float turnInput;
    private boolean boosterRequested;

    private float mainFuel = MAIN_FUEL_CAPACITY;
    private float mainEngineOutput;
    private int boosterCharges = BOOSTER_CHARGES;
    private float boosterTimeLeft;

    private FlightState state = FlightState.FLYING;
    private String crashReason = "";

    // Written by ShipContactListener during world.step(). Read in afterStep().
    private int groundContacts;
    private Planet touchdownPlanet;
    private float touchdownSpeed;

    private final Vector2 force = new Vector2();
    private final Vector2 localUp = new Vector2();

    /**
     * @param angle radians; 0 points the nose along world +Y. See {@link #uprightAngle}.
     */
    public Ship(World world, Vector2 position, float angle) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.set(position);
        bodyDef.angle = angle;
        // No `bullet` flag here. Box2D already sweeps dynamic bodies against static
        // ones between steps, so the ship can't tunnel through terrain. `bullet` only
        // adds that sweep against other *dynamic* bodies, such as future debris.

        body = world.createBody(bodyDef);
        body.setUserData(this);

        CircleShape shape = new CircleShape();
        shape.setRadius(HULL_RADIUS);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        // Box2D derives mass from density × area. Choose the density that gives the
        // mass we want.
        fixtureDef.density = HULL_MASS / (MathUtils.PI * HULL_RADIUS * HULL_RADIUS);
        fixtureDef.friction = HULL_FRICTION;
        fixtureDef.restitution = HULL_RESTITUTION;
        fixtureDef.filter.categoryBits = CollisionCategory.SHIP;
        fixtureDef.filter.maskBits = CollisionCategory.TERRAIN;

        body.createFixture(fixtureDef).setUserData(FixtureRole.SHIP_HULL);
        shape.dispose();

        hull = new InterpolatedBody(body);
    }

    /** Body angle that points the nose straight away from a planet, at the given angle around it. */
    public static float uprightAngle(float angleAroundPlanet) {
        return angleAroundPlanet - MathUtils.HALF_PI;
    }

    // ---- Pilot intent -------------------------------------------------------

    /** 0 = engine off, 1 = full thrust. */
    public void setThrottle(float throttle) {
        this.throttle = MathUtils.clamp(throttle, 0f, 1f);
    }

    /** −1 = full clockwise, +1 = full counter-clockwise, 0 = hold attitude. */
    public void setTurnInput(float turnInput) {
        this.turnInput = MathUtils.clamp(turnInput, -1f, 1f);
    }

    /** Lights the solid booster on the next step. Once lit, it burns until empty. */
    public void fireBooster() {
        boosterRequested = true;
    }

    // ---- Physics step -------------------------------------------------------

    /** Turns pilot intent into forces. Call before every world step. */
    public void beforeStep(float dt) {
        hull.savePreviousTransform();

        if (state == FlightState.CRASHED) {
            mainEngineOutput = 0f;
            return;
        }
        applyMainEngine(dt);
        applyBooster(dt);
        applyAttitudeControl(dt);
    }

    private void applyMainEngine(float dt) {
        mainEngineOutput = mainFuel > 0f ? throttle : 0f;
        if (mainEngineOutput == 0f) return;

        mainFuel = Math.max(0f, mainFuel - mainEngineOutput * dt);
        applyThrust(MAIN_ENGINE_THRUST * mainEngineOutput);
    }

    private void applyBooster(float dt) {
        if (boosterRequested && boosterCharges > 0 && boosterTimeLeft <= 0f) {
            boosterCharges--;
            boosterTimeLeft = BOOSTER_BURN_TIME;
        }
        boosterRequested = false;

        if (boosterTimeLeft <= 0f) return;
        boosterTimeLeft -= dt;
        applyThrust(BOOSTER_THRUST);
    }

    private void applyThrust(float newtons) {
        // getWorldVector rotates a local direction by the body's current angle.
        force.set(body.getWorldVector(NOSE_DIRECTION)).scl(newtons);
        // wake = true: thrusting must wake a ship that fell asleep on a landing zone.
        body.applyForceToCenter(force, true);
    }

    /**
     * Arcade rotation done with real physics. Each step, apply the torque that brings
     * the spin rate toward the pilot's requested rate (zero when there's no input, so
     * the ship holds its attitude). The torque is capped so the change isn't instant.
     *
     * <p>The alternative, {@code setTransform}, teleports the angle. It skips
     * collision response and would tear jointed parts such as landing legs away from
     * the hull.
     */
    private void applyAttitudeControl(float dt) {
        float targetRate = turnInput * MAX_TURN_RATE;
        float neededAcceleration = (targetRate - body.getAngularVelocity()) / dt;
        float angularAcceleration = MathUtils.clamp(neededAcceleration, -MAX_TURN_ACCELERATION, MAX_TURN_ACCELERATION);

        // Torque = I × α, the rotational twin of F = m × a.
        // Only wake the ship when the pilot is steering. Holding attitude on a
        // sleeping ship would keep it awake forever.
        body.applyTorque(body.getInertia() * angularAcceleration, turnInput != 0f);
    }

    /** Game rules. Call after every world step, when contacts for the step are known. */
    public void afterStep() {
        Planet planet = touchdownPlanet;
        float speed = touchdownSpeed;
        touchdownPlanet = null;
        touchdownSpeed = 0f;

        if (state == FlightState.CRASHED) return;

        if (planet != null) {
            judgeTouchdown(planet, speed);
        } else if (state == FlightState.LANDED && groundContacts == 0) {
            state = FlightState.FLYING;
        }
    }

    private void judgeTouchdown(Planet planet, float impactSpeed) {
        float tilt = tiltRelativeTo(planet);
        if (impactSpeed > MAX_LANDING_SPEED) {
            crash(String.format("Hit the ground at %.1f m/s (max %.0f)", impactSpeed, MAX_LANDING_SPEED));
        } else if (tilt > MAX_LANDING_TILT) {
            crash(String.format("Touched down tilted %.0f° (max %.0f°)",
                tilt * MathUtils.radDeg, MAX_LANDING_TILT * MathUtils.radDeg));
        } else {
            state = FlightState.LANDED;
        }
    }

    /** Angle between the nose and "straight up" (away from the planet's center), 0..π. */
    private float tiltRelativeTo(Planet planet) {
        localUp.set(body.getWorldCenter()).sub(planet.getCenter()).nor();
        Vector2 nose = body.getWorldVector(NOSE_DIRECTION);
        return Math.abs((float) Math.atan2(localUp.crs(nose), localUp.dot(nose)));
    }

    private void crash(String reason) {
        state = FlightState.CRASHED;
        crashReason = reason;
        boosterTimeLeft = 0f;
    }

    // ---- Contact reports (called by ShipContactListener inside world.step) --

    /**
     * Record only; don't decide yet. The world is locked mid-step, and several
     * contacts can begin in one step. The hardest hit wins.
     */
    void onGroundContactBegin(Planet planet, float impactSpeed) {
        groundContacts++;
        if (touchdownPlanet == null || impactSpeed > touchdownSpeed) {
            touchdownPlanet = planet;
            touchdownSpeed = impactSpeed;
        }
    }

    void onGroundContactEnd() {
        groundContacts--;
    }

    // ---- Lifecycle and read-only state for HUD and rendering ----------------

    /** Removes the ship's body from the world. Never call this during world.step(). */
    public void removeFrom(World world) {
        world.destroyBody(body);
    }

    public InterpolatedBody getHull() {
        return hull;
    }

    public Body getBody() {
        return body;
    }

    public FlightState getState() {
        return state;
    }

    public String getCrashReason() {
        return crashReason;
    }

    public float getMainFuel() {
        return mainFuel;
    }

    /** Throttle actually applied last step: 0 when out of fuel or crashed. */
    public float getMainEngineOutput() {
        return mainEngineOutput;
    }

    public int getBoosterCharges() {
        return boosterCharges;
    }

    public boolean isBoosterBurning() {
        return boosterTimeLeft > 0f;
    }
}
