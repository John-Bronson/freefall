package com.johnbronson.freefall;

public class Constants
{
    public static final int MILS_PER_CIRCLE = 6400;
    public static final float MILS_TO_RADIANS = (2f * (float)Math.PI) / 6400f;
    public static final float MILS_TO_DEGREES = 360f / 6400f;  // ≈ 0.05625
    public static final float RADIANS_TO_MILS = 6400f / (2f * (float)Math.PI);
    public static final float DEGREES_TO_MILS = 6400f / 360f;  // ≈ 17.778
    public static final float RADIANS_TO_DEGREES = 360f / (2f * (float)Math.PI);

    public static final float GRAVITATIONAL_CONSTANT = 85000f;
    public static final float SOLID_BOOST_ACCELERATION = 15f;   // m/s² (was 150 px/s² before Box2D)
    public static final float MAIN_THRUST_ACCELERATION = 25f;

    public static final float MAX_LANDING_ANGLE = 0.2618f;  // 15 degrees in radians
}
