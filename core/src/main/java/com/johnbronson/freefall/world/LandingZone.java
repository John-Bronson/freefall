package com.johnbronson.freefall.world;

import com.badlogic.gdx.math.MathUtils;

/**
 * A flat stretch of terrain, given as an arc of the planet's surface. Angles are in
 * radians, counter-clockwise from the planet's +X axis, with
 * {@code 0 <= startAngle < endAngle <= 2π}.
 */
public record LandingZone(float startAngle, float endAngle) {

    public LandingZone {
        if (startAngle < 0f || endAngle > MathUtils.PI2 || startAngle >= endAngle) {
            throw new IllegalArgumentException("Landing zone must satisfy 0 <= start < end <= 2π");
        }
    }

    public static LandingZone ofDegrees(float startDegrees, float endDegrees) {
        return new LandingZone(startDegrees * MathUtils.degRad, endDegrees * MathUtils.degRad);
    }
}
