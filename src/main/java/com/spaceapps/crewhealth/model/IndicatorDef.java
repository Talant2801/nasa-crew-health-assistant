package com.spaceapps.crewhealth.model;

/**
 * Indicator definition loaded from data/indicators.json.
 * badDirection: "UP" or "DOWN". limitDirection: "ABOVE" or "BELOW". absoluteLimit may be null.
 */
public record IndicatorDef(
        String code,
        String name,
        String unit,
        String bodySystem,
        double bandPercent,
        String badDirection,
        Double absoluteLimit,
        String limitDirection,
        String source,
        String actionCardId) {

    public boolean badIsUp() {
        return "UP".equalsIgnoreCase(badDirection);
    }

    public boolean limitIsAbove() {
        return "ABOVE".equalsIgnoreCase(limitDirection);
    }
}
