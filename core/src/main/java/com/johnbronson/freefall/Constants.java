package com.johnbronson.freefall;

public class Constants
{
    public static final int MILS_PER_CIRCLE = 6400;
    public static final float MILS_TO_RADIANS = (2f * (float)Math.PI) / 6400f;
    public static final float MILS_TO_DEGREES = 360f / 6400f;  // ≈ 0.05625
    public static final float RADIANS_TO_MILS = 6400f / (2f * (float)Math.PI);
    public static final float DEGREES_TO_MILS = 6400f / 360f;  // ≈ 17.778
}
