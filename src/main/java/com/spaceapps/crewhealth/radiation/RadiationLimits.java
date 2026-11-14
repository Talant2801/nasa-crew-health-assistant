package com.spaceapps.crewhealth.radiation;

/** Exposure limits from NASA-STD-3001 Volume 1. */
public final class RadiationLimits {
    public static final double CAREER_LIMIT_MSV = 600;
    public static final String CAREER_LIMIT_SOURCE = "NASA-STD-3001 Vol 1, [V1 4030] career effective dose limit";
    public static final double SPE_LIMIT_MSV = 250;
    public static final String SPE_LIMIT_SOURCE = "NASA-STD-3001 Vol 1, [V1 4031] short-term (SPE) limit";
    public static final String MODEL = "Simplified model: mission days × published dose rate";

    private RadiationLimits() {
    }
}
